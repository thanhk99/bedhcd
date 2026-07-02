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
import com.api.bedhcd.modules.participant.application.port.ParticipantPort;
import com.api.bedhcd.modules.resolution.application.port.ResolutionPort;
import com.api.bedhcd.modules.voting.application.port.VotingPort;
import com.api.bedhcd.shared.domain.enums.MeetingStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import com.api.bedhcd.shared.domain.UuidFactory;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingApplicationService {

    private final MeetingRepository meetingRepository;
    private final MeetingConfigRepository configRepository;
    private final MeetingEditRequestRepository editRequestRepository;
    private final MeetingMapper meetingMapper;
    private final MeetingEditRequestMapper editRequestMapper;
    private final MeetingPort meetingPort;
    private final ParticipantPort participantPort;
    private final ResolutionPort resolutionPort;
    private final VotingPort votingPort;
    private final AdminContextService adminContextService;

    // Sử dụng ObjectMapper với JavaTimeModule để hỗ trợ serialize LocalDateTime
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // ─── Queries ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MeetingResponse> getAll() {
        List<Meeting> meetings = meetingRepository.findAll();
        Map<String, MeetingConfig> configCache = loadConfigs(meetings);
        return meetings.stream()
                .map(m -> meetingMapper.toResponse(m, configCache.get(m.getConfigId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MeetingResponse getById(String id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> MeetingException.notFound(id));
        MeetingConfig config = loadConfig(meeting.getConfigId());
        return meetingMapper.toResponse(meeting, config);
    }

    @Transactional(readOnly = true)
    public MeetingResponse getOngoingMeeting() {
        return meetingRepository.findOngoing()
                .map(m -> {
                    MeetingConfig config = loadConfig(m.getConfigId());
                    return meetingMapper.toResponse(m, config);
                })
                .orElse(null);
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

        long totalResolutions = resolutionPort.countResolutionsByMeetingId(id);
        long totalVotes = votingPort.countVotesByMeetingId(id);

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
                .voting(MeetingRealtimeResponse.VotingStats.builder()
                        .totalResolutions(totalResolutions)
                        .totalVotes(totalVotes)
                        .build())
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

    @Transactional
    public MeetingResponse createMeeting(Meeting meeting) {
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

        if (meeting.getStatus() == null || meeting.getStatus().isEmpty()) {
            meeting.setStatus(MeetingStatus.SCHEDULED);
        }

        Meeting saved = meetingRepository.save(meeting);
        return meetingMapper.toResponse(saved, config);
    }

    /**
     * Chỉnh sửa cuộc họp.
     * - SUPERADMIN: Cập nhật trực tiếp, không cần approval.
     * - ADMIN thường: Tạo MeetingEditRequest (PENDING), cần approval.
     *
     * @return MeetingEditRequestResponse khi cần approval, MeetingResponse khi
     *         SUPERADMIN cập nhật trực tiếp
     *         (client phân biệt qua field requiresApproval)
     */
    @Transactional
    public Object updateMeeting(String id, Meeting updateInfo) {
        // Xác minh cuộc họp tồn tại
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> MeetingException.notFound(id));

        if (adminContextService.isCurrentAdminSuperAdmin()) {
            // SUPERADMIN: cập nhật trực tiếp
            meeting.setTitle(updateInfo.getTitle());
            meeting.setDescription(updateInfo.getDescription());
            meeting.setStartTime(updateInfo.getStartTime());
            meeting.setEndTime(updateInfo.getEndTime());
            meeting.setLocation(updateInfo.getLocation());
            meeting.setConfigId(updateInfo.getConfigId());
            meeting.setStatus(updateInfo.getStatus());

            Meeting saved = meetingRepository.save(meeting);
            MeetingConfig config = loadConfig(saved.getConfigId());
            return meetingMapper.toResponse(saved, config);
        }

        // ADMIN thường: tạo pending request
        if (editRequestRepository.existsPendingForMeeting(id)) {
            throw MeetingException.pendingRequestAlreadyExists(id);
        }

        String payloadJson = serializeToJson(updateInfo);
        String adminId = adminContextService.getCurrentAdminId();
        MeetingEditRequest request = MeetingEditRequest.createUpdateRequest(id, adminId, payloadJson);
        MeetingEditRequest saved = editRequestRepository.save(request);
        return editRequestMapper.toResponse(saved);
    }

    /**
     * Đổi trạng thái cuộc họp.
     * - SUPERADMIN: Đổi trực tiếp.
     * - ADMIN thường: Tạo MeetingEditRequest (PENDING) chờ duyệt.
     */
    @Transactional
    public Object updateStatus(String id, String status) {
        // Xác minh cuộc họp tồn tại
        meetingRepository.findById(id)
                .orElseThrow(() -> MeetingException.notFound(id));

        if (adminContextService.isCurrentAdminSuperAdmin()) {
            // SUPERADMIN: cập nhật trực tiếp
            Meeting meeting = meetingRepository.findById(id)
                    .orElseThrow(() -> MeetingException.notFound(id));
            meeting.setStatus(status);
            meeting.setUpdatedAt(java.time.LocalDateTime.now());

            Meeting saved = meetingRepository.save(meeting);
            MeetingConfig config = loadConfig(saved.getConfigId());
            return meetingMapper.toResponse(saved, config);
        }

        // ADMIN thường: tạo pending request
        if (editRequestRepository.existsPendingForMeeting(id)) {
            throw MeetingException.pendingRequestAlreadyExists(id);
        }

        String adminId = adminContextService.getCurrentAdminId();
        MeetingEditRequest request = MeetingEditRequest.createUpdateStatusRequest(id, adminId, status);
        MeetingEditRequest saved = editRequestRepository.save(request);
        return editRequestMapper.toResponse(saved);
    }

    /**
     * Xóa cuộc họp.
     * - SUPERADMIN: Xóa trực tiếp.
     * - ADMIN thường: Tạo MeetingEditRequest DELETE (PENDING) chờ duyệt.
     */
    @Transactional
    public Object deleteMeeting(String id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> MeetingException.notFound(id));

        if (adminContextService.isCurrentAdminSuperAdmin()) {
            // SUPERADMIN: xóa trực tiếp
            if (!meetingPort.canEditMeeting(id)) {
                throw MeetingException.invalidState("Trạng thái hiện tại không cho phép xóa cuộc họp.");
            }
            if (!meeting.canDelete()) {
                throw MeetingException.invalidState("Chỉ có thể xóa cuộc họp ở trạng thái Sắp diễn ra.");
            }
            meetingRepository.deleteById(id);
            return null;
        }

        // ADMIN thường: tạo pending request
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
                Meeting updateInfo = deserializeFromJson(request.getPayload(), Meeting.class);
                meeting.setTitle(updateInfo.getTitle());
                meeting.setDescription(updateInfo.getDescription());
                meeting.setStartTime(updateInfo.getStartTime());
                meeting.setEndTime(updateInfo.getEndTime());
                meeting.setLocation(updateInfo.getLocation());
                meeting.setConfigId(updateInfo.getConfigId());
                meeting.setStatus(updateInfo.getStatus());
                meetingRepository.save(meeting);
            }
            case "UPDATE_STATUS" -> {
                Meeting meeting = meetingRepository.findById(request.getMeetingId())
                        .orElseThrow(() -> MeetingException.notFound(request.getMeetingId()));
                meeting.setStatus(request.getPayload());
                meeting.setUpdatedAt(java.time.LocalDateTime.now());
                meetingRepository.save(meeting);
            }
            case "DELETE" -> {
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
}
