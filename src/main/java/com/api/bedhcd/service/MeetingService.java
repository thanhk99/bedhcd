package com.api.bedhcd.service;

import com.api.bedhcd.dto.request.MeetingRequest;
import com.api.bedhcd.dto.response.MeetingResponse;
import com.api.bedhcd.entity.Meeting;
import com.api.bedhcd.entity.User;
import com.api.bedhcd.entity.enums.MeetingStatus;
import com.api.bedhcd.exception.ResourceNotFoundException;
import com.api.bedhcd.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.api.bedhcd.util.RandomUtil;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;

    @Transactional
    public MeetingResponse createMeeting(MeetingRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Meeting meeting = Meeting.builder()
                .id(RandomUtil.generate6DigitId(meetingRepository::existsById))
                .title(request.getTitle())
                .description(request.getDescription())
                .meetingDate(request.getMeetingDate())
                .location(request.getLocation())
                .status(request.getStatus() != null ? request.getStatus() : MeetingStatus.SCHEDULED)
                .createdBy(currentUser)
                .build();

        meeting = meetingRepository.save(meeting);
        return mapToResponse(meeting);
    }

    @Transactional
    public MeetingResponse updateMeeting(Long id, MeetingRequest request) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        meeting.setTitle(request.getTitle());
        meeting.setDescription(request.getDescription());
        meeting.setMeetingDate(request.getMeetingDate());
        meeting.setLocation(request.getLocation());
        if (request.getStatus() != null) {
            meeting.setStatus(request.getStatus());
        }

        meeting = meetingRepository.save(meeting);
        return mapToResponse(meeting);
    }

    public MeetingResponse getMeetingById(Long id) {
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
    public void updateMeetingStatus(Long id, MeetingStatus status) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        meeting.setStatus(status);
        meetingRepository.save(meeting);
    }

    @Transactional
    public void deleteMeeting(Long id) {
        if (!meetingRepository.existsById(id)) {
            throw new ResourceNotFoundException("Meeting not found");
        }
        meetingRepository.deleteById(id);
    }

    private MeetingResponse mapToResponse(Meeting meeting) {
        return MeetingResponse.builder()
                .id(meeting.getId())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .meetingDate(meeting.getMeetingDate())
                .location(meeting.getLocation())
                .status(meeting.getStatus())
                .createdAt(meeting.getCreatedAt())
                .updatedAt(meeting.getUpdatedAt())
                .build();
    }
}
