package com.api.bedhcd.modules.resolution.domain.model;

import com.api.bedhcd.shared.domain.enums.VotingOptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import com.api.bedhcd.shared.domain.UuidFactory;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resolution {
        private String id;
        private String meetingId;
        private String title;
        private String description;
        private Integer displayOrder;
        @Builder.Default
        private List<VotingOption> options = Collections.emptyList();

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Resolution createNew(String meetingId, String title, String description, Integer displayOrder) {
                Resolution resolution = Resolution.builder()
                                .id(UuidFactory.generate())
                                .meetingId(meetingId)
                                .title(title)
                                .description(description)
                                .displayOrder(displayOrder)
                                .createdAt(LocalDateTime.now())
                                .build();

                resolution.setOptions(List.of(
                                VotingOption.builder()
                                                .id(UuidFactory.generate())
                                                .name("Đồng ý")
                                                .type(VotingOptionType.AGREE)
                                                .displayOrder(1)
                                                .build(),
                                VotingOption.builder()
                                                .id(UuidFactory.generate())
                                                .name("Không đồng ý")
                                                .type(VotingOptionType.DISAGREE)
                                                .displayOrder(2)
                                                .build(),
                                VotingOption.builder()
                                                .id(UuidFactory.generate())
                                                .name("Ý kiến khác")
                                                .type(VotingOptionType.NO_IDEA)
                                                .displayOrder(3)
                                                .build()));

                return resolution;
        }

        public VotingOption findOption(String optionId) {
                if (options == null)
                        return null;
                return options.stream()
                                .filter(opt -> opt.getId().equals(optionId))
                                .findFirst()
                                .orElse(null);
        }
}
