package com.api.bedhcd.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteEvent {

    public enum Type {
        RESOLUTION,
        ELECTION
    }

    public enum Action {
        VOTE_CAST,
        VOTE_CHANGED
    }

    private String meetingId;
    private String itemId; // ResolutionId or ElectionId
    private Type type;
    private Action action;
}
