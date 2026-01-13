package com.api.bedhcd.service.kafka;

import com.api.bedhcd.dto.response.VotingResultResponse;
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
    public void consumeVoteUpdate(VotingResultResponse votingResult) {
        log.info("Received vote update for resolution: {}", votingResult.getResolutionId());

        // Broadcast to WebSocket topic: /topic/voting-session/{resolutionId}
        String destination = "/topic/voting-session/" + votingResult.getResolutionId();
        messagingTemplate.convertAndSend(destination, votingResult);
        log.info("Broadcasted to WebSocket: {}", destination);
    }
}
