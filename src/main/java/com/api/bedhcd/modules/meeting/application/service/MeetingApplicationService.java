package com.api.bedhcd.modules.meeting.application.service;

import com.api.bedhcd.modules.meeting.api.v1.dto.MeetingEditRequestResponse;
import com.api.bedhcd.modules.meeting.api.v1.dto.MeetingResponse;
import com.api.bedhcd.modules.meeting.api.v1.dto.MeetingRealtimeResponse;
import com.api.bedhcd.modules.meeting.application.mapper.MeetingEditRequestMapper;
import com.api.bedhcd.modules.meeting.application.mapper.MeetingMapper;
import com.api.bedhcd.modules.meeting.application.port.MeetingPort;
import com.api.bedhcd.modules.meeting.domain.exception.MeetingException;
import com.api.bedhcd.modules.meeting.domain.model.Meeting;
import com.api.bedhcd.modules.meeting.domain.model.MeetingConfig;
import com.api.bedhcd.modules.meeting.domain.model.MeetingEditRequest;
import com.api.bedhcd.modules.meeting.domain.repository.MeetingConfigRepository;
import com.api.bedhcd.modules.meeting.domain.repository.MeetingEditRequestRepository;
import com.api.bedhcd.modules.meeting.domain.repository.MeetingRepository;
import com.api.bedhcd.modules.election.application.port.ElectionPort;
import com.api.bedhcd.modules.participant.application.port.ParticipantPort;
import com.api.bedhcd.modules.resolution.application.port.ResolutionPort;
import com.api.bedhcd.modules.voting.application.port.VotingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import com.api.bedhcd.shared.domain.UuidFactory;
import java.util.stream.Collectors;

import com.api.bedhcd.modules.identity.application.port.IdentityPort;
import com.api.bedhcd.shared.port.KafkaPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import com.api.bedhcd.modules.meeting.api.v1.dto.MeetingWebSocketResponse;

@Service
@RequiredArgsConstructor
public class MeetingApplicationService {

    // Cho phép chữ cái (Unicode), chữ số, khoảng trắng, dấu câu thông thường
    private static final Pattern VALID_MEETING_TEXT = Pattern.compile("^[\\p{L}\\p{N}\\s,.:;\\-/()'\"!?&]+$");

    // Nhãn hiển thị cho từng field trong bảng meeting_edit_requests (cột changes)
    private static final Map<String, String> FIELD_LABELS = Map.of(
            "title", "Tên cuộc họp",
            "description", "Mô tả",
            "location", "Địa điểm",
            "startTime", "Giờ bắt đầu",
            "endTime", "Giờ kết thúc",
            "configId", "Cấu hình",
            "status", "Trạng thái");

    // Format thời gian hiển thị trong mô tả tự nhiên
    private static final java.time.format.DateTimeFormatter DATE_TIME_FORMATTER = java.time.format.DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm");

    // Ký hiệu "không có giá trị" trong cột changes
    private static final String EMPTY_MARK = "∅";

    private final MeetingRepository meetingRepository;
    private final MeetingConfigRepository configRepository;
    private final MeetingEditRequestRepository editRequestRepository;
    private final MeetingMapper meetingMapper;
    private final MeetingEditRequestMapper editRequestMapper;
    private final MeetingPort meetingPort;
    private final ParticipantPort participantPort;
    private final ResolutionPort resolutionPort;
    private final VotingPort votingPort;
    private final ElectionPort electionPort;
    private final IdentityPort identityPort;
    private final AdminContextService adminContextService;
    private final KafkaPort kafkaPort;

    // ─── Queries ────────────────────────────────────────────────────────────────

    // Không cache getAll() - List<MeetingResponse> phức tạp, dễ lỗi serialization
    @Transactional(readOnly = true)
    public List<MeetingResponse> getAll() {
        List<Meeting> meetings = meetingRepository.findAll();
        Map<String, MeetingConfig> configCache = loadConfigs(meetings);
        return meetings.stream()
                .map(m -> meetingMapper.toResponse(m, configCache.get(m.getConfigId())))
                .collect(Collectors.toList());
    }

    @Cacheable(value = "meetings", key = "#id")
    @Transactional(readOnly = true)
    public MeetingResponse getById(String id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> MeetingException.notFound(id));

        // Nếu người dùng hiện tại là cổ đông, kiểm tra cấu hình xem cuộc họp
        String currentUserId = identityPort.getCurrentUserId();
        if (currentUserId != null && !identityPort.hasRole(com.api.bedhcd.shared.domain.enums.Role.ADMIN)) {
            if (!meetingPort.shareholderCanViewMeeting(id)) {
                throw MeetingException.invalidState("Cấu hình cuộc họp hiện tại không cho phép cổ đông xem thông tin.");
            }
        }

        MeetingConfig config = loadConfig(meeting.getConfigId());
        return meetingMapper.toResponse(meeting, config);
    }

    /**
     * Dành cho Cổ đông gọi qua API.
     * Gọi cached method rồi áp dụng kiểm tra phân quyền ở ngoài cache.
     */
    @Transactional(readOnly = true)
    public MeetingResponse getOngoingMeetingForShareholder() {
        // Lấy danh sách cuộc họp từ mới nhất
        java.util.List<Meeting> allMeetings = meetingRepository.findAllOrderByCreatedAtDesc();
        if (allMeetings == null || allMeetings.isEmpty())
            return null;

        // Duyệt danh sách, tìm cuộc họp đầu tiên mà cổ đông được phép xem
        String currentUserId = identityPort.getCurrentUserId();
        boolean isShareholder = currentUserId != null
                && !identityPort.hasRole(com.api.bedhcd.shared.domain.enums.Role.ADMIN);

        Meeting ongoingMeeting = null;
        for (Meeting m : allMeetings) {
            if (isShareholder) {
                if (meetingPort.shareholderCanViewMeeting(m.getId())) {
                    ongoingMeeting = m;
                    break;
                }
            } else {
                // Nếu là Admin, lấy cuộc họp mới nhất
                ongoingMeeting = m;
                break;
            }
        }

        if (ongoingMeeting == null)
            return null;

        MeetingConfig config = loadConfig(ongoingMeeting.getConfigId());
        return meetingMapper.toResponse(ongoingMeeting, config);
    }

    @Transactional(readOnly = true)
    public MeetingRealtimeResponse getRealtimeStats(String id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> MeetingException.notFound(id));

        long totalParticipants = participantPort.countByMeetingId(id);
        long checkedInCount = participantPort.countCheckedInByMeetingId(id);
        long totalShares = participantPort.sumTotalSharesByMeetingId(id);
        long checkedInShares = participantPort.sumCheckedInSharesByMeetingId(id);

        double participationRate = totalShares > 0
                ? (double) checkedInShares * 100 / totalShares
                : 0;

        // Lấy danh sách resolutions kèm options qua ResolutionPort
        List<MeetingRealtimeResponse.ResolutionResult> resolutionResults = resolutionPort.getResolutionsByMeetingId(id)
                .stream()
                .map(r -> {
                    // Lấy chi tiết vote cho từng lựa chọn trong resolution
                    List<com.api.bedhcd.modules.voting.application.port.VoteResult> voteResults = votingPort
                            .getVotesByTarget(r.resolutionId());

                    // Tạo danh sách các option từ resolution (có thể ít hơn 3)
                    List<MeetingRealtimeResponse.VoteOptionResult> optionResults = new ArrayList<>();
                    long totalWeight = 0;
                    long totalVoters = 0;

                    // Lấy tất cả các option từ resolution
                    List<com.api.bedhcd.modules.resolution.application.port.ResolutionPort.OptionSummary> resolutionOptions = r
                            .options();

                    // Duyệt đúng theo optionId thật từ DB thay vì hardcode
                    for (com.api.bedhcd.modules.resolution.application.port.ResolutionPort.OptionSummary opt : r
                            .options()) {
                        // Tìm vote result tương ứng nếu tồn tại
                        com.api.bedhcd.modules.voting.application.port.VoteResult voteResult = voteResults.stream()
                                .filter(v -> v.getOptionId().equals(opt.optionId())) // dùng UUID thật từ DB
                                .findFirst()
                                .orElse(null);

                        if (voteResult != null) {
                            optionResults.add(MeetingRealtimeResponse.VoteOptionResult.builder()
                                    .votingOptionId(voteResult.getOptionId())
                                    .votingOptionName(opt.name()) // Sử dụng tên thật từ resolution
                                    .voteCount(voteResult.getVoteCount())
                                    .totalWeight(voteResult.getTotalWeight())
                                    .percentage(0.0) // Phần trăm sẽ tính sau
                                    .build());
                            totalWeight += voteResult.getTotalWeight();
                            totalVoters += voteResult.getVoteCount();
                        } else {
                            optionResults.add(MeetingRealtimeResponse.VoteOptionResult.builder()
                                    .votingOptionId(opt.optionId())
                                    .votingOptionName(opt.name())
                                    .voteCount(0)
                                    .totalWeight(0)
                                    .percentage(0.0)
                                    .build());
                        }
                    }

                    // Tính phần trăm cho từng option
                    for (MeetingRealtimeResponse.VoteOptionResult option : optionResults) {
                        if (totalWeight > 0) {
                            option.setPercentage((double) option.getTotalWeight() * 100 / totalWeight);
                        } else {
                            option.setPercentage(0.0); // Đảm bảo phần trăm là 0 nếu tổng weight = 0
                        }
                    }

                    return MeetingRealtimeResponse.ResolutionResult.builder()
                            .resolutionId(r.resolutionId())
                            .title(r.title())
                            .description(r.description())
                            .displayOrder(r.displayOrder())
                            .options(optionResults)
                            .totalVoters(totalVoters)
                            .totalWeight(totalWeight)
                            .build();
                })
                .collect(Collectors.toList());

        // Lấy danh sách elections kèm candidates qua ElectionPort
        List<MeetingRealtimeResponse.ElectionResult> electionResults = electionPort.getElectionsByMeetingId(id)
                .stream()
                .map(e -> {
                    // Lấy chi tiết vote cho từng ứng cử viên trong election
                    List<com.api.bedhcd.modules.voting.application.port.VoteResult> voteResults = votingPort
                            .getVotesByTarget(e.electionId());

                    // Tạo danh sách các candidate từ election (có thể ít hơn 3)
                    List<MeetingRealtimeResponse.VoteOptionResult> candidateResults = new ArrayList<>();
                    long totalWeight = 0;
                    long totalVoters = 0;

                    // Lấy tất cả các candidate từ election
                    List<com.api.bedhcd.modules.election.application.port.ElectionPort.CandidateSummary> electionCandidates = e
                            .candidates();

                    // Duyệt đúng theo candidateId thật từ DB thay vì hardcode
                    for (com.api.bedhcd.modules.election.application.port.ElectionPort.CandidateSummary candidate : e
                            .candidates()) {
                        // Tìm vote result tương ứng nếu tồn tại
                        com.api.bedhcd.modules.voting.application.port.VoteResult voteResult = voteResults.stream()
                                .filter(v -> v.getOptionId().equals(candidate.candidateId())) // dùng UUID thật từ DB
                                .findFirst()
                                .orElse(null);

                        if (voteResult != null) {
                            candidateResults.add(MeetingRealtimeResponse.VoteOptionResult.builder()
                                    .votingOptionId(voteResult.getOptionId())
                                    .votingOptionName(candidate.name()) // Sử dụng tên thật từ election
                                    .voteCount(voteResult.getVoteCount())
                                    .totalWeight(voteResult.getTotalWeight())
                                    .percentage(0.0) // Phần trăm sẽ tính sau
                                    .build());
                            totalWeight += voteResult.getTotalWeight();
                            totalVoters += voteResult.getVoteCount();
                        } else {
                            candidateResults.add(MeetingRealtimeResponse.VoteOptionResult.builder()
                                    .votingOptionId(candidate.candidateId())
                                    .votingOptionName(candidate.name())
                                    .voteCount(0)
                                    .totalWeight(0)
                                    .percentage(0.0)
                                    .build());
                        }
                    }

                    // Tính phần trăm cho từng candidate
                    for (MeetingRealtimeResponse.VoteOptionResult candidate : candidateResults) {
                        if (totalWeight > 0) {
                            candidate.setPercentage((double) candidate.getTotalWeight() * 100 / totalWeight);
                        }
                    }

                    return MeetingRealtimeResponse.ElectionResult.builder()
                            .electionId(e.electionId())
                            .title(e.title())
                            .electionType(e.electionType())
                            .candidates(candidateResults)
                            .totalVoters(totalVoters)
                            .totalWeight(totalWeight)
                            .build();
                })
                .collect(Collectors.toList());

        return MeetingRealtimeResponse.builder()
                .meetingId(meeting.getId())
                .title(meeting.getTitle())
                .status(meeting.getStatus())
                .attendance(MeetingRealtimeResponse.AttendanceStats.builder()
                        .totalParticipants(totalParticipants)
                        .checkedInCount(checkedInCount)
                        .totalShares(totalShares)
                        .checkedInShares(checkedInShares)
                        .participationRate(participationRate)
                        .build())
                .resolutions(resolutionResults)
                .elections(electionResults)
                .build();
    }

    // ─── Lấy danh sách Edit Requests ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MeetingEditRequestResponse> getPendingRequests() {
        return editRequestRepository.findAllPending().stream()
                .map(editRequestMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MeetingEditRequestResponse getEditRequestById(String requestId) {
        MeetingEditRequest request = editRequestRepository.findById(requestId)
                .orElseThrow(() -> MeetingException.editRequestNotFound(requestId));
        return editRequestMapper.toResponse(request);
    }

    // ─── Commands ────────────────────────────────────────────────────────────────

    @Caching(evict = {
            @CacheEvict(value = "meetings:all", allEntries = true),
            @CacheEvict(value = "meetings:ongoing", allEntries = true)
    })
    @Transactional
    public Object createMeeting(Meeting meeting) {
        // Validate các trường bắt buộc
        validateMeetingFields(meeting.getTitle(), meeting.getDescription(), meeting.getLocation(),
                meeting.getStartTime(), meeting.getEndTime());

        if (meeting.getMeetingCode() == null || meeting.getMeetingCode().trim().isEmpty()) {
            meeting.setMeetingCode(UuidFactory.generate().substring(0, 8).toUpperCase());
        }

        MeetingConfig config = loadConfig(meeting.getConfigId());

        // Logic kiểm tra nếu có yêu cầu đang chờ xử lý
        if (editRequestRepository.existsPendingForMeeting("NEW")) {
            throw MeetingException.pendingRequestAlreadyExists("NEW");
        }

        // Tạo mô tả rõ ràng + danh sách thay đổi (text) từ thông tin cuộc họp mới
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("title", meeting.getTitle());
        newValues.put("description", meeting.getDescription());
        newValues.put("location", meeting.getLocation());
        newValues.put("startTime", meeting.getStartTime());
        newValues.put("endTime", meeting.getEndTime());
        newValues.put("configId", meeting.getConfigId());

        String changesText = buildChangesText(newValues, null);
        String description = buildCreateDescription(meeting);
        String adminId = adminContextService.getCurrentAdminId();

        // Tạo yêu cầu phê duyệt CREATE thay vì trực tiếp lưu
        MeetingEditRequest request = MeetingEditRequest.createCreateRequest("NEW", adminId, description, changesText);
        MeetingEditRequest saved = editRequestRepository.save(request);
        return editRequestMapper.toResponse(saved);
    }

    /**
     * Chỉnh sửa cuộc họp.
     * - SUPERADMIN: Tạo MeetingEditRequest (PENDING), cần approval.
     * - ADMIN thường: Tạo MeetingEditRequest (PENDING), cần approval.
     *
     * @return MeetingEditRequestResponse khi cần approval
     */
    @Caching(evict = {
            @CacheEvict(value = "meetings:all", allEntries = true),
            @CacheEvict(value = "meetings", key = "#id"),
            @CacheEvict(value = "meetings:ongoing", allEntries = true),
            @CacheEvict(value = "meetings:realtime", key = "#id")
    })
    @Transactional
    public Object updateMeeting(String id, Meeting updateInfo) {
        // Validate các trường dữ liệu đầu vào
        validateMeetingFields(updateInfo.getTitle(), updateInfo.getDescription(), updateInfo.getLocation(),
                updateInfo.getStartTime(), updateInfo.getEndTime());

        // Xác minh cuộc họp tồn tại
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> MeetingException.notFound(id));

        // SUPERADMIN cũng phải tạo yêu cầu phê duyệt như ADMIN thông thường
        if (editRequestRepository.existsPendingForMeeting(id)) {
            throw MeetingException.pendingRequestAlreadyExists(id);
        }

        java.util.Map<String, Object> changes = new java.util.HashMap<>();
        java.util.Map<String, Object> oldValues = new java.util.HashMap<>();
        if (!java.util.Objects.equals(meeting.getTitle(), updateInfo.getTitle())) {
            changes.put("title", updateInfo.getTitle());
            oldValues.put("title", meeting.getTitle());
        }
        if (!java.util.Objects.equals(meeting.getDescription(), updateInfo.getDescription())) {
            changes.put("description", updateInfo.getDescription());
            oldValues.put("description", meeting.getDescription());
        }
        if (!java.util.Objects.equals(meeting.getStartTime(), updateInfo.getStartTime())) {
            changes.put("startTime", updateInfo.getStartTime());
            oldValues.put("startTime", meeting.getStartTime());
        }
        if (!java.util.Objects.equals(meeting.getEndTime(), updateInfo.getEndTime())) {
            changes.put("endTime", updateInfo.getEndTime());
            oldValues.put("endTime", meeting.getEndTime());
        }
        if (!java.util.Objects.equals(meeting.getLocation(), updateInfo.getLocation())) {
            changes.put("location", updateInfo.getLocation());
            oldValues.put("location", meeting.getLocation());
        }
        if (!java.util.Objects.equals(meeting.getConfigId(), updateInfo.getConfigId())) {
            changes.put("configId", updateInfo.getConfigId());
            oldValues.put("configId", meeting.getConfigId());
        }
        if (!java.util.Objects.equals(meeting.getStatus(), updateInfo.getStatus())) {
            changes.put("status", updateInfo.getStatus());
            oldValues.put("status", meeting.getStatus());
        }

        if (changes.isEmpty()) {
            throw MeetingException.invalidState("Không có thay đổi nào để tạo yêu cầu.");
        }

        String changesText = buildChangesText(changes, oldValues);
        String description = buildUpdateDescription(meeting.getTitle(), changes, oldValues);
        String adminId = adminContextService.getCurrentAdminId();
        MeetingEditRequest request = MeetingEditRequest.createUpdateRequest(id, adminId, description, changesText);
        MeetingEditRequest saved = editRequestRepository.save(request);
        return editRequestMapper.toResponse(saved);
    }

    /**
     * Đổi trạng thái cuộc họp.
     * - SUPERADMIN: Tạo MeetingEditRequest (PENDING) chờ duyệt.
     * - ADMIN thường: Tạo MeetingEditRequest (PENDING) chờ duyệt.
     */
    @Caching(evict = {
            @CacheEvict(value = "meetings:all", allEntries = true),
            @CacheEvict(value = "meetings", key = "#id"),
            @CacheEvict(value = "meetings:ongoing", allEntries = true),
            @CacheEvict(value = "meetings:realtime", key = "#id")
    })
    @Transactional
    public Object updateStatus(String id, String status) {
        // Xác minh cuộc họp tồn tại
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> MeetingException.notFound(id));

        // SUPERADMIN cũng phải tạo yêu cầu phê duyệt như ADMIN thông thường
        if (editRequestRepository.existsPendingForMeeting(id)) {
            throw MeetingException.pendingRequestAlreadyExists(id);
        }

        java.util.Map<String, Object> changes = new java.util.HashMap<>();
        changes.put("status", status);
        java.util.Map<String, Object> oldValues = new java.util.HashMap<>();
        oldValues.put("status", meeting.getStatus());

        String changesText = buildChangesText(changes, oldValues);
        String description = buildUpdateDescription(meeting.getTitle(), changes, oldValues);
        String adminId = adminContextService.getCurrentAdminId();
        MeetingEditRequest request = MeetingEditRequest.createUpdateRequest(id, adminId, description, changesText);
        MeetingEditRequest saved = editRequestRepository.save(request);
        return editRequestMapper.toResponse(saved);
    }

    /**
     * Xóa cuộc họp.
     * - SUPERADMIN: Tạo MeetingEditRequest DELETE (PENDING) chờ duyệt.
     * - ADMIN thường: Tạo MeetingEditRequest DELETE (PENDING) chờ duyệt.
     */
    @Caching(evict = {
            @CacheEvict(value = "meetings:all", allEntries = true),
            @CacheEvict(value = "meetings", key = "#id"),
            @CacheEvict(value = "meetings:ongoing", allEntries = true),
            @CacheEvict(value = "meetings:realtime", key = "#id")
    })
    @Transactional
    public Object deleteMeeting(String id) {

        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> MeetingException.notFound(id));

        if (!meetingPort.canDeleteMeeting(id)) {
            throw MeetingException.invalidState("Trạng thái cuộc họp hiện tại không cho phép xóa cuộc họp.");
        }

        // Kiểm tra xem có cổ đông nào trong bảng tham dự của cuộc họp này không
        if (participantPort.countByMeetingId(id) > 0) {
            throw MeetingException.invalidState("Không thể xóa vì cuộc họp đã tồn tại danh sách cổ đông tham dự.");
        }

        // SUPERADMIN cũng phải tạo yêu cầu phê duyệt như ADMIN thông thường
        if (editRequestRepository.existsPendingForMeeting(id)) {
            throw MeetingException.pendingRequestAlreadyExists(id);
        }

        String meetingName = meeting.getTitle() != null && !meeting.getTitle().isBlank()
                ? meeting.getTitle()
                : id;
        String description = "Xoá cuộc họp '" + meetingName + "'";

        String adminId = adminContextService.getCurrentAdminId();
        MeetingEditRequest request = MeetingEditRequest.createDeleteRequest(id, adminId, description);
        MeetingEditRequest saved = editRequestRepository.save(request);
        return editRequestMapper.toResponse(saved);
    }

    // ─── Approve / Reject ────────────────────────────────────────────────────────

    /**
     * Phê duyệt một yêu cầu chỉnh sửa cuộc họp.
     * Áp dụng thay đổi thực tế vào cuộc họp và đánh dấu request là APPROVED.
     */
    @Caching(evict = {
            @CacheEvict(value = "meetings:all", allEntries = true),
            @CacheEvict(value = "meetings", allEntries = true),
            @CacheEvict(value = "meetings:ongoing", allEntries = true),
            @CacheEvict(value = "meetings:realtime", allEntries = true)
    })
    @Transactional
    public MeetingEditRequestResponse approveEditRequest(String requestId) {
        MeetingEditRequest request = editRequestRepository.findById(requestId)
                .orElseThrow(() -> MeetingException.editRequestNotFound(requestId));

        String reviewerId = adminContextService.getCurrentAdminId();

        // Business logic: approve() sẽ throw MeetingException nếu không phải PENDING
        request.approve(reviewerId);

        // Áp dụng thay đổi vào Meeting
        applyRequestToMeeting(request);

        // Lưu request đã được duyệt
        MeetingEditRequest saved = editRequestRepository.save(request);
        return editRequestMapper.toResponse(saved);
    }

    /**
     * Từ chối một yêu cầu chỉnh sửa cuộc họp.
     */
    @Transactional
    public MeetingEditRequestResponse rejectEditRequest(String requestId, String note) {
        MeetingEditRequest request = editRequestRepository.findById(requestId)
                .orElseThrow(() -> MeetingException.editRequestNotFound(requestId));

        String reviewerId = adminContextService.getCurrentAdminId();

        // Business logic: reject() sẽ throw MeetingException nếu không phải PENDING
        request.reject(reviewerId, note);

        MeetingEditRequest saved = editRequestRepository.save(request);
        return editRequestMapper.toResponse(saved);
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────────

    private void applyRequestToMeeting(MeetingEditRequest request) {
        switch (request.getActionType()) {
            case "CREATE" -> {
                // Xử lý yêu cầu tạo mới cuộc họp
                Meeting meeting = new Meeting();
                applyChangesToMeeting(meeting, request.getChanges());

                // Set mã cuộc họp
                meeting.setMeetingCode(UuidFactory.generate().substring(0, 8).toUpperCase());

                // Set trạng thái ban đầu nếu có trong config
                MeetingConfig config = loadConfig(meeting.getConfigId());
                if (config != null && config.getStateConfigs() != null && !config.getStateConfigs().isEmpty()) {
                    Set<String> nextStates = config.getStateConfigs().values().stream()
                            .map(rule -> rule.getNextState())
                            .filter(state -> state != null)
                            .collect(Collectors.toSet());

                    String firstStatus = config.getStateConfigs().keySet().stream()
                            .filter(k -> !nextStates.contains(k))
                            .findFirst()
                            .orElse(null);

                    if (firstStatus != null) {
                        meeting.setStatus(firstStatus);
                    }
                }

                // Lưu cuộc họp mới
                Meeting savedMeeting = meetingRepository.save(meeting);

                // Gửi thông báo WebSocket nếu cần
                sendMeetingUpdateToWebSocket(savedMeeting.getId());
            }
            case "UPDATE" -> {
                // Xử lý chỉnh sửa cuộc họp bình thường
                Meeting meeting = meetingRepository.findById(request.getMeetingId())
                        .orElseThrow(() -> MeetingException.notFound(request.getMeetingId()));

                applyChangesToMeeting(meeting, request.getChanges());

                meetingRepository.save(meeting);
            }
            case "DELETE" -> {
                if (!meetingPort.canDeleteMeeting(request.getMeetingId())) {
                    throw MeetingException
                            .invalidState("Trạng thái cuộc họp tại thời điểm duyệt không cho phép thực thi xóa.");
                }
                if (participantPort.countByMeetingId(request.getMeetingId()) > 0) {
                    throw MeetingException
                            .invalidState("Không thể thực thi xóa vì cuộc họp đã tồn tại danh sách cổ đông tham dự.");
                }
                meetingRepository.deleteById(request.getMeetingId());
            }
            default -> throw MeetingException.invalidState(
                    "Loại thao tác không hợp lệ: " + request.getActionType());
        }
    }

    /**
     * Tạo nội dung cột changes (text quy ước, mỗi dòng: fieldCode|fieldLabel|old|new).
     * oldValues == null nghĩa là tạo mới (old = ∅).
     */
    private String buildChangesText(Map<String, Object> newValues, Map<String, Object> oldValues) {
        List<String> lines = new ArrayList<>();
        if (newValues == null) {
            return "";
        }
        for (Map.Entry<String, Object> entry : newValues.entrySet()) {
            String field = entry.getKey();
            String label = FIELD_LABELS.getOrDefault(field, field);
            String oldValue = oldValues == null ? EMPTY_MARK : formatStorageValue(oldValues.get(field));
            String newValue = formatStorageValue(entry.getValue());
            lines.add(field + "|" + label + "|" + oldValue + "|" + newValue);
        }
        return String.join("\n", lines);
    }

    /**
     * Mô tả tự nhiên cho yêu cầu tạo mới cuộc họp.
     */
    private String buildCreateDescription(Meeting meeting) {
        List<String> parts = new ArrayList<>();
        if (meeting.getLocation() != null && !meeting.getLocation().isBlank()) {
            parts.add("Địa điểm: '" + meeting.getLocation() + "'");
        }
        if (meeting.getStartTime() != null) {
            parts.add("Từ " + meeting.getStartTime().format(DATE_TIME_FORMATTER));
        }
        if (meeting.getEndTime() != null) {
            parts.add("đến " + meeting.getEndTime().format(DATE_TIME_FORMATTER));
        }
        String title = meeting.getTitle() != null && !meeting.getTitle().isBlank()
                ? "'" + meeting.getTitle() + "'"
                : "";
        String info = parts.isEmpty() ? "" : " (" + String.join(", ", parts) + ")";
        return "Tạo cuộc họp mới " + title + info;
    }

    /**
     * Mô tả tự nhiên cho yêu cầu cập nhật cuộc họp: "Đổi X từ 'A' thành 'B'".
     */
    private String buildUpdateDescription(String meetingTitle, Map<String, Object> changes,
            Map<String, Object> oldValues) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Object> entry : changes.entrySet()) {
            String field = entry.getKey();
            String label = FIELD_LABELS.getOrDefault(field, field);
            String oldValue = oldValues == null ? EMPTY_MARK : formatDisplayValue(oldValues.get(field));
            String newValue = formatDisplayValue(entry.getValue());
            parts.add("Đổi " + label + " từ '" + oldValue + "' thành '" + newValue + "'");
        }
        String title = meetingTitle != null && !meetingTitle.isBlank() ? "'" + meetingTitle + "'" : "";
        return "Cập nhật cuộc họp " + title + ": " + String.join(", ", parts);
    }

    /**
     * Giá trị lưu trong cột changes (LocalDateTime để dạng ISO để parse lại).
     */
    private String formatStorageValue(Object value) {
        if (value == null) {
            return EMPTY_MARK;
        }
        if (value instanceof java.time.LocalDateTime) {
            return ((java.time.LocalDateTime) value).toString();
        }
        return value.toString();
    }

    /**
     * Giá trị hiển thị trong mô tả tự nhiên (ngày giờ dạng dd/MM/yyyy HH:mm).
     */
    private String formatDisplayValue(Object value) {
        if (value == null) {
            return EMPTY_MARK;
        }
        if (value instanceof java.time.LocalDateTime) {
            return ((java.time.LocalDateTime) value).format(DATE_TIME_FORMATTER);
        }
        String text = value.toString();
        return text.isEmpty() ? "(trống)" : text;
    }

    /**
     * Áp dụng danh sách thay đổi (cột changes) lên cuộc họp khi APPROVE.
     */
    private void applyChangesToMeeting(Meeting meeting, String changes) {
        if (changes == null || changes.isBlank()) {
            return;
        }
        for (String line : changes.split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\|");
            if (parts.length < 4) {
                continue;
            }
            String field = parts[0];
            String newValue = EMPTY_MARK.equals(parts[3]) ? null : parts[3];
            switch (field) {
                case "title" -> meeting.setTitle(newValue);
                case "description" -> meeting.setDescription(newValue);
                case "location" -> meeting.setLocation(newValue);
                case "configId" -> meeting.setConfigId(newValue);
                case "startTime" -> meeting.setStartTime(parseDateTime(newValue));
                case "endTime" -> meeting.setEndTime(parseDateTime(newValue));
                case "status" -> {
                    meeting.setStatus(newValue);
                    if (meeting.getId() != null) {
                        handleMeetingStatusChange(meeting.getId(), newValue);
                    }
                }
                default -> {
                }
            }
        }
    }

    private java.time.LocalDateTime parseDateTime(String value) {
        if (value == null) {
            return null;
        }
        return java.time.LocalDateTime.parse(value);
    }

    private MeetingConfig loadConfig(String configId) {
        if (configId == null || configId.isEmpty())
            return null;
        return configRepository.findById(configId).orElse(null);
    }

    private Map<String, MeetingConfig> loadConfigs(List<Meeting> meetings) {
        Set<String> configIds = meetings.stream()
                .map(Meeting::getConfigId)
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toSet());
        if (configIds.isEmpty())
            return Map.of();
        return configRepository.findAll().stream()
                .filter(c -> configIds.contains(c.getId()))
                .collect(Collectors.toMap(MeetingConfig::getId, c -> c));
    }

    /**
     * Kiểm tra tính hợp lệ của thông tin cuộc họp.
     * - Tên, mô tả, địa điểm không được chứa ký tự đặc biệt hoặc icon/emoji.
     * - Thời gian bắt đầu phải trước thời gian kết thúc.
     */
    private void validateMeetingFields(String title, String description, String location,
            java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        if (title != null && !title.isBlank() && !VALID_MEETING_TEXT.matcher(title).matches()) {
            throw MeetingException.invalidState("Tên cuộc họp không được chứa ký tự đặc biệt hoặc icon/emoji.");
        }
        if (description != null && !description.isBlank() && !VALID_MEETING_TEXT.matcher(description).matches()) {
            throw MeetingException.invalidState("Mô tả không được chứa ký tự đặc biệt hoặc icon/emoji.");
        }
        if (location != null && !location.isBlank() && !VALID_MEETING_TEXT.matcher(location).matches()) {
            throw MeetingException.invalidState("Địa điểm không được chứa ký tự đặc biệt hoặc icon/emoji.");
        }
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            throw MeetingException.invalidState("Thời gian bắt đầu phải trước thời gian kết thúc.");
        }
    }

    private void handleMeetingStatusChange(String meetingId, String newStatus) {
        if ("COMPLETED".equals(newStatus)) {
            java.util.List<String> userIds = participantPort.getParticipantUserIds(meetingId);
            if (userIds != null && !userIds.isEmpty()) {
                identityPort.updateShareholderStatusBatch(userIds,
                        com.api.bedhcd.shared.domain.enums.ShareholderStatus.EXPIRED);
            }
        }
    }

    /**
     * Tạo và gửi dữ liệu WebSocket khi có cập nhật
     */
    public void sendMeetingUpdateToWebSocket(String meetingId) {
        try {
            MeetingRealtimeResponse realtimeStats = getRealtimeStats(meetingId);

            // Tạo dữ liệu theo định dạng WebSocket - sử dụng kết quả đã tính
            MeetingWebSocketResponse webSocketResponse = MeetingWebSocketResponse.builder()
                    .type("FULL")
                    .data(MeetingWebSocketResponse.Payload.builder()
                            .meetingId(meetingId)
                            .resolutionResults(convertToResolutionResults(realtimeStats.getResolutions()))
                            .electionResults(convertToElectionResults(realtimeStats.getElections()))
                            .build())
                    .timestamp(Instant.now().toEpochMilli())
                    .build();

            // Gửi qua Kafka (topic vote_events)
            kafkaPort.send("vote_events", meetingId, webSocketResponse);
        } catch (Exception e) {
            // Log lỗi nhưng không throw để không ảnh hưởng đến hệ thống chính
            System.err.println("Error sending websocket update: " + e.getMessage());
        }
    }

    private List<MeetingWebSocketResponse.ResolutionResult> convertToResolutionResults(
            List<MeetingRealtimeResponse.ResolutionResult> resolutionResults) {
        List<MeetingWebSocketResponse.ResolutionResult> results = new ArrayList<>();

        for (MeetingRealtimeResponse.ResolutionResult res : resolutionResults) {
            // Tạo một bản sao để đảm bảo kiểu dữ liệu phù hợp
            List<MeetingWebSocketResponse.VoteOptionResult> webSocketOptions = new ArrayList<>();
            for (MeetingRealtimeResponse.VoteOptionResult option : res.getOptions()) {
                webSocketOptions.add(MeetingWebSocketResponse.VoteOptionResult.builder()
                        .votingOptionId(option.getVotingOptionId())
                        .votingOptionName(option.getVotingOptionName())
                        .voteCount(option.getVoteCount())
                        .totalWeight(option.getTotalWeight())
                        .percentage(option.getPercentage())
                        .build());
            }

            MeetingWebSocketResponse.ResolutionResult webSocketResult = MeetingWebSocketResponse.ResolutionResult
                    .builder()
                    .resolutionId(res.getResolutionId())
                    .resolutionTitle(res.getTitle())
                    .results(webSocketOptions)
                    .totalVoters(res.getTotalVoters())
                    .totalWeight(res.getTotalWeight())
                    .build();

            results.add(webSocketResult);
        }

        return results;
    }

    private List<MeetingWebSocketResponse.ElectionResult> convertToElectionResults(
            List<MeetingRealtimeResponse.ElectionResult> electionResults) {
        List<MeetingWebSocketResponse.ElectionResult> results = new ArrayList<>();

        for (MeetingRealtimeResponse.ElectionResult election : electionResults) {
            // Tạo một bản sao để đảm bảo kiểu dữ liệu phù hợp
            List<MeetingWebSocketResponse.VoteOptionResult> webSocketCandidates = new ArrayList<>();
            for (MeetingRealtimeResponse.VoteOptionResult candidate : election.getCandidates()) {
                webSocketCandidates.add(MeetingWebSocketResponse.VoteOptionResult.builder()
                        .votingOptionId(candidate.getVotingOptionId())
                        .votingOptionName(candidate.getVotingOptionName())
                        .voteCount(candidate.getVoteCount())
                        .totalWeight(candidate.getTotalWeight())
                        .percentage(candidate.getPercentage())
                        .build());
            }

            MeetingWebSocketResponse.ElectionResult webSocketResult = MeetingWebSocketResponse.ElectionResult.builder()
                    .electionId(election.getElectionId())
                    .electionTitle(election.getTitle())
                    .results(webSocketCandidates)
                    .totalVoters(election.getTotalVoters())
                    .totalWeight(election.getTotalWeight())
                    .build();

            results.add(webSocketResult);
        }

        return results;
    }
}
