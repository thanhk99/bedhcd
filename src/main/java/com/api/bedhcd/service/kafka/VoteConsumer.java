package com.api.bedhcd.service.kafka;

import com.api.bedhcd.dto.event.VoteEvent;

import com.api.bedhcd.service.VotingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// @Service
@RequiredArgsConstructor
@Slf4j
public class VoteConsumer {

    private final VotingService votingService;

    /**
     * Lắng nghe các event bầu cử nhẹ (VOTE_CAST/VOTE_CHANGED).
     * Sẽ trigger việc tính toán lại từng phần và update cache.
     */
    // @KafkaListener(topics = "vote_events", groupId = "voting-group")
    public void consumeVoteEvent(VoteEvent event) {
        log.info("Received vote event: {} for item {}", event.getType(), event.getItemId());
        try {
            votingService.processVoteUpdate(event);
        } catch (Exception e) {
            log.error("Error processing vote event", e);
        }
    }

}
