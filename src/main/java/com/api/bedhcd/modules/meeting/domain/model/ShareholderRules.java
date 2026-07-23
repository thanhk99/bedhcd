package com.api.bedhcd.modules.meeting.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareholderRules {

    private AccountManagement accountManagement;
    private MeetingModule meeting;
    private VotingAndElection votingAndElection;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountManagement {
        @Builder.Default
        private boolean allowView = false;
        @Builder.Default
        private boolean allowEdit = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeetingModule {
        @Builder.Default
        private boolean allowView = false;
        @Builder.Default
        private boolean allowAction = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VotingAndElection {
        @Builder.Default
        private boolean allowView = false;
        @Builder.Default
        private boolean allowVote = false;
    }

    public static ShareholderRules createEmpty() {
        return ShareholderRules.builder()
                .accountManagement(new AccountManagement())
                .meeting(new MeetingModule())
                .votingAndElection(new VotingAndElection())
                .build();
    }
}
