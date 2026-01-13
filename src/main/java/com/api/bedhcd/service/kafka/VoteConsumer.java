package com.api.bedhcd.service.kafka;

import com.api.bedhcd.dto.response.MeetingRealtimeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "vote_updates", groupId = "voting-group")
    public void consumeVoteUpdate(MeetingRealtimeStatus meetingStatus) {
        log.info("Received vote update for meeting: {}", meetingStatus.getMeetingId());

        // Broadcast to WebSocket topic: /topic/meeting/{meetingId}
        String destination = "/topic/meeting/" + meetingStatus.getMeetingId();
        messagingTemplate.convertAndSend(destination, meetingStatus);
        log.info("Broadcasted to WebSocket: {}", destination);
    }
}
