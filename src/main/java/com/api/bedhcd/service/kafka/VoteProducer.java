package com.api.bedhcd.service.kafka;

import com.api.bedhcd.dto.response.MeetingRealtimeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendVoteUpdate(MeetingRealtimeStatus status) {
        try {
            log.info("Sending meeting update for meeting {}", status.getMeetingId());
            kafkaTemplate.send("vote_updates", status.getMeetingId(), status);
        } catch (Exception e) {
            log.error("Error sending vote update to Kafka", e);
        }
    }
}
