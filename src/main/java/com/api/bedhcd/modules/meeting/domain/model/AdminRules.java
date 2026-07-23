package com.api.bedhcd.modules.meeting.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminRules {
    
    private RoleGroup roleGroup;
    private MeetingModule meeting;
    private ShareholderModule shareholder;
    private EligibilityCheck eligibilityCheck;
    private VotingModule voting;
    private ReportModule report;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleGroup {
        @Builder.Default
        private boolean allowEdit = false;
        @Builder.Default
        private boolean allowView = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeetingModule {
        @Builder.Default
        private boolean allowAdd = false;
        @Builder.Default
        private boolean allowEdit = false;
        @Builder.Default
        private boolean allowDelete = false;
        @Builder.Default
        private boolean allowView = false;
        @Builder.Default
        private boolean allowApprove = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShareholderModule {
        @Builder.Default
        private boolean allowAdd = false;
        @Builder.Default
        private boolean allowEdit = false;
        @Builder.Default
        private boolean allowDelete = false;
        @Builder.Default
        private boolean allowView = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EligibilityCheck {
        @Builder.Default
        private boolean allowAction = false;
        @Builder.Default
        private boolean allowView = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VotingModule {
        @Builder.Default
        private boolean allowView = false;
        @Builder.Default
        private boolean allowEditVoteResult = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportModule {
        @Builder.Default
        private boolean allowViewStats = false;
        @Builder.Default
        private boolean allowExportReport = false;
    }

    public static AdminRules createEmpty() {
        return AdminRules.builder()
                .roleGroup(new RoleGroup())
                .meeting(new MeetingModule())
                .shareholder(new ShareholderModule())
                .eligibilityCheck(new EligibilityCheck())
                .voting(new VotingModule())
                .report(new ReportModule())
                .build();
    }
}

