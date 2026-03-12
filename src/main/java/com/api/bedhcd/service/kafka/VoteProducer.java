package com.api.bedhcd.service.kafka;

import com.api.bedhcd.dto.event.VoteEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendVoteEvent(VoteEvent event) {
        log.info("Kafka is temporarily disabled. Skipping vote event for meeting {} - Item: {}",
                event.getMeetingId(), event.getItemId());

        try {
            log.info("Sending vote event for meeting {} - Item: {}",
                    event.getMeetingId(), event.getItemId());
            kafkaTemplate.send("vote_events", event.getMeetingId(), event);
        } catch (Exception e) {
            log.error("Error sending vote event to Kafka", e);
        }

    }
}
