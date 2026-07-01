package com.api.bedhcd.modules.resolution.application.mapper;

import com.api.bedhcd.modules.resolution.domain.model.Resolution;
import com.api.bedhcd.modules.resolution.domain.model.VotingOption;
import com.api.bedhcd.modules.voting.api.v1.dto.ResolutionResponse;
import com.api.bedhcd.modules.voting.api.v1.dto.VotingOptionResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

@Component
public class ResolutionMapper {

    public ResolutionResponse toResponse(Resolution domain) {
        if (domain == null) return null;
        return ResolutionResponse.builder()
                .id(domain.getId())
                .meetingId(domain.getMeetingId())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .displayOrder(domain.getDisplayOrder())
                .options(domain.getOptions() == null ? Collections.emptyList() :
                        domain.getOptions().stream()
                                .map(opt -> toOptionResponse(opt, domain.getId()))
                                .collect(Collectors.toList()))
                .build();
    }

    public VotingOptionResponse toOptionResponse(VotingOption domain, String resolutionId) {
        if (domain == null) return null;
        return VotingOptionResponse.builder()
                .id(domain.getId())
                .resolutionId(resolutionId)
                .label(domain.getName())
                .description(domain.getDescription())
                .type(domain.getType())
                .build();
    }
}
