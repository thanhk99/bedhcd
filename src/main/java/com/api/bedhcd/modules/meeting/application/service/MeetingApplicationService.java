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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import com.api.bedhcd.modules.meeting.api.v1.dto.MeetingWebSocketResponse;

@Service
@RequiredArgsConstructor
public class MeetingApplicationService {

    // Cho phép chữ cái (Unicode), chữ số, khoảng trắng, dấu câu thông thường
    private static final Pattern VALID_MEETING_TEXT = Pattern.compile("^[\\p{L}\\p{N}\\s,.:;\\-/()'\"!?&]+$");

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

    // Sử dụng ObjectMapper với JavaTimeModule để hỗ trợ serialize LocalDateTime
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

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
                List<com.api.bedhcd.modules.voting.application.port.VoteResult> voteResults = votingPort.getVotesByTarget(r.resolutionId());
                
                // Tạo danh sách các option từ resolution (có thể ít hơn 3)
                List<MeetingRealtimeResponse.VoteOptionResult> optionResults = new ArrayList<>();
                long totalWeight = 0;
                long totalVoters = 0;
                
                // Lấy tất cả các option từ resolution
                List<com.api.bedhcd.modules.resolution.application.port.ResolutionPort.OptionSummary> resolutionOptions = r.options();
                
                // Duyệt đúng theo optionId thật từ DB thay vì hardcode
                for (com.api.bedhcd.modules.resolution.application.port.ResolutionPort.OptionSummary opt : r.options()) {
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
                List<com.api.bedhcd.modules.voting.application.port.VoteResult> voteResults = votingPort.getVotesByTarget(e.electionId());
                
                // Tạo danh sách các candidate từ election (có thể ít hơn 3)
                List<MeetingRealtimeResponse.VoteOptionResult> candidateResults = new ArrayList<>();
                long totalWeight = 0;
                long totalVoters = 0;
                
                // Lấy tất cả các candidate từ election
                List<com.api.bedhcd.modules.election.application.port.ElectionPort.CandidateSummary> electionCandidates = e.candidates();
                
                // Duyệt đúng theo candidateId thật từ DB thay vì hardcode
                for (com.api.bedhcd.modules.election.application.port.ElectionPort.CandidateSummary candidate : e.candidates()) {
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
    public MeetingResponse createMeeting(Meeting meeting) {
        // Validate các trường bắt buộc
        validateMeetingFields(meeting.getTitle(), meeting.getDescription(), meeting.getLocation(),
                meeting.getStartTime(), meeting.getEndTime());

        if (meeting.getMeetingCode() == null || meeting.getMeetingCode().trim().isEmpty()) {
            meeting.setMeetingCode(UuidFactory.generate().substring(0, 8).toUpperCase());
        }

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

        Meeting saved = meetingRepository.save(meeting);
        return meetingMapper.toResponse(saved, config);
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
        if (!java.util.Objects.equals(meeting.getTitle(), updateInfo.getTitle()))
            changes.put("title", updateInfo.getTitle());
        if (!java.util.Objects.equals(meeting.getDescription(), updateInfo.getDescription()))
            changes.put("description", updateInfo.getDescription());
        if (!java.util.Objects.equals(meeting.getStartTime(), updateInfo.getStartTime()))
            changes.put("startTime", updateInfo.getStartTime());
        if (!java.util.Objects.equals(meeting.getEndTime(), updateInfo.getEndTime()))
            changes.put("endTime", updateInfo.getEndTime());
        if (!java.util.Objects.equals(meeting.getLocation(), updateInfo.getLocation()))
            changes.put("location", updateInfo.getLocation());
        if (!java.util.Objects.equals(meeting.getConfigId(), updateInfo.getConfigId()))
            changes.put("configId", updateInfo.getConfigId());
        if (!java.util.Objects.equals(meeting.getStatus(), updateInfo.getStatus()))
            changes.put("status", updateInfo.getStatus());

        if (changes.isEmpty()) {
            throw MeetingException.invalidState("Không có thay đổi nào để tạo yêu cầu.");
        }

        String payloadJson = serializeToJson(changes);
        String adminId = adminContextService.getCurrentAdminId();
        MeetingEditRequest request = MeetingEditRequest.createUpdateRequest(id, adminId, payloadJson);
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
        meetingRepository.findById(id)
                .orElseThrow(() -> MeetingException.notFound(id));

        // SUPERADMIN cũng phải tạo yêu cầu phê duyệt như ADMIN thông thường
        if (editRequestRepository.existsPendingForMeeting(id)) {
            throw MeetingException.pendingRequestAlreadyExists(id);
        }

        java.util.Map<String, Object> changes = new java.util.HashMap<>();
        changes.put("status", status);

        String payloadJson = serializeToJson(changes);
        String adminId = adminContextService.getCurrentAdminId();
        MeetingEditRequest request = MeetingEditRequest.createUpdateRequest(id, adminId, payloadJson);
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

        String adminId = adminContextService.getCurrentAdminId();
        MeetingEditRequest request = MeetingEditRequest.createDeleteRequest(id, adminId);
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
            case "UPDATE" -> {
                Meeting meeting = meetingRepository.findById(request.getMeetingId())
                        .orElseThrow(() -> MeetingException.notFound(request.getMeetingId()));

                com.fasterxml.jackson.databind.JsonNode root = parseJsonNode(request.getPayload());
                if (root.has("title"))
                    meeting.setTitle(root.get("title").isNull() ? null : root.get("title").asText());
                if (root.has("description"))
                    meeting.setDescription(root.get("description").isNull() ? null : root.get("description").asText());
                if (root.has("location"))
                    meeting.setLocation(root.get("location").isNull() ? null : root.get("location").asText());
                if (root.has("configId"))
                    meeting.setConfigId(root.get("configId").isNull() ? null : root.get("configId").asText());
                if (root.has("status")) {
                    String newStatus = root.get("status").isNull() ? null : root.get("status").asText();
                    meeting.setStatus(newStatus);
                    handleMeetingStatusChange(meeting.getId(), newStatus);
                }

                if (root.has("startTime")) {
                    meeting.setStartTime(root.get("startTime").isNull() ? null
                            : objectMapper.convertValue(root.get("startTime"), java.time.LocalDateTime.class));
                }
                if (root.has("endTime")) {
                    meeting.setEndTime(root.get("endTime").isNull() ? null
                            : objectMapper.convertValue(root.get("endTime"), java.time.LocalDateTime.class));
                }

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

    private String serializeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw MeetingException.invalidState("Không thể serialize payload: " + e.getMessage());
        }
    }

    private com.fasterxml.jackson.databind.JsonNode parseJsonNode(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw MeetingException.invalidState("Không thể parse payload: " + e.getMessage());
        }
    }

    private <T> T deserializeFromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw MeetingException.invalidState("Không thể deserialize payload: " + e.getMessage());
        }
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
                identityPort.updateShareholderStatusBatch(userIds, com.api.bedhcd.shared.domain.enums.ShareholderStatus.EXPIRED);
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
            
            MeetingWebSocketResponse.ResolutionResult webSocketResult = MeetingWebSocketResponse.ResolutionResult.builder()
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
