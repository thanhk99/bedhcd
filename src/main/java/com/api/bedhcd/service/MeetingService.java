package com.api.bedhcd.service;

import com.api.bedhcd.dto.request.MeetingRequest;
import com.api.bedhcd.dto.response.MeetingResponse;
import com.api.bedhcd.entity.Meeting;
import com.api.bedhcd.entity.enums.MeetingStatus;
import com.api.bedhcd.exception.ResourceNotFoundException;
import com.api.bedhcd.repository.MeetingRepository;
import com.api.bedhcd.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final VotingService votingService;
    private final ElectionService electionService;

    @Transactional
    public MeetingResponse createMeeting(MeetingRequest request) {
        if (request.getStatus() == MeetingStatus.ONGOING && meetingRepository.existsByStatus(MeetingStatus.ONGOING)) {
            throw new com.api.bedhcd.exception.BadRequestException(
                    "There is already an ongoing meeting. Only one meeting can be ongoing at a time.");
        }

        String id = RandomUtil.generate6DigitId(meetingRepository::existsById);

        Meeting meeting = Meeting.builder()
                .id(id)
                .meetingCode(id)
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .location(request.getLocation())
                .status(request.getStatus() != null ? request.getStatus() : MeetingStatus.SCHEDULED)
                .build();

        meeting = meetingRepository.save(meeting);
        return mapToResponse(meeting);
    }

    @Transactional
    public MeetingResponse updateMeeting(String id, MeetingRequest request) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (request.getTitle() != null)
            meeting.setTitle(request.getTitle());
        if (request.getDescription() != null)
            meeting.setDescription(request.getDescription());
        if (request.getStartTime() != null)
            meeting.setStartTime(request.getStartTime());
        if (request.getEndTime() != null)
            meeting.setEndTime(request.getEndTime());
        if (request.getLocation() != null)
            meeting.setLocation(request.getLocation());
        if (request.getStatus() != null) {
            if (request.getStatus() == MeetingStatus.ONGOING) {
                java.util.Optional<Meeting> ongoingMeeting = meetingRepository.findFirstByStatus(MeetingStatus.ONGOING);
                if (ongoingMeeting.isPresent() && !ongoingMeeting.get().getId().equals(meeting.getId())) {
                    throw new com.api.bedhcd.exception.BadRequestException(
                            "There is already another ongoing meeting. Finish it before starting a new one.");
                }
            }
            meeting.setStatus(request.getStatus());
        }

        meeting = meetingRepository.save(meeting);
        return mapToResponse(meeting);
    }

    public MeetingResponse getMeetingById(String id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        return mapToResponse(meeting);
    }

    public List<MeetingResponse> getAllMeetings() {
        return meetingRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateMeetingStatus(String id, MeetingStatus status) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (status == MeetingStatus.ONGOING) {
            java.util.Optional<Meeting> ongoingMeeting = meetingRepository.findFirstByStatus(MeetingStatus.ONGOING);
            if (ongoingMeeting.isPresent() && !ongoingMeeting.get().getId().equals(meeting.getId())) {
                throw new com.api.bedhcd.exception.BadRequestException(
                        "There is already another ongoing meeting. Finish it before starting a new one.");
            }
        }

        meeting.setStatus(status);
        meetingRepository.save(meeting);
    }

    public MeetingResponse getOngoingMeeting() {
        return meetingRepository.findFirstByStatus(MeetingStatus.ONGOING)
                .map(this::mapToResponse)
                .orElse(null);
    }

    @Transactional
    public void deleteMeeting(String id) {
        if (!meetingRepository.existsById(id)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        meetingRepository.deleteById(id);
    }

    private MeetingResponse mapToResponse(Meeting meeting) {
        return MeetingResponse.builder()
                .id(meeting.getId())
                .meetingCode(meeting.getMeetingCode())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .location(meeting.getLocation())
                .status(meeting.getStatus())
                .resolutions(meeting.getResolutions() != null ? meeting.getResolutions().stream()
                        .map(votingService::mapResolutionToResponse)
                        .collect(Collectors.toList()) : null)
                .elections(meeting.getElections() != null ? meeting.getElections().stream()
                        .map(electionService::mapElectionToResponse)
                        .collect(Collectors.toList()) : null)
                .createdAt(meeting.getCreatedAt())
                .updatedAt(meeting.getUpdatedAt())
                .build();
    }
}
