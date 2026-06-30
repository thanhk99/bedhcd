package com.api.bedhcd.modules.voting.infrastructure.persistence;

import com.api.bedhcd.modules.voting.domain.model.Resolution;
import com.api.bedhcd.modules.voting.domain.model.VotingOption;
import com.api.bedhcd.modules.voting.domain.repository.ResolutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ResolutionRepositoryImpl implements ResolutionRepository {

    private final ResolutionJpaRepository jpaRepository;

    @Override
    public Optional<Resolution> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Resolution> findByMeetingId(String meetingId) {
        return jpaRepository.findByMeetingId(meetingId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Resolution save(Resolution domain) {
        ResolutionEntity entity = toEntity(domain);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public long countByMeetingId(String meetingId) {
        return jpaRepository.countByMeetingId(meetingId);
    }

    private Resolution toDomain(ResolutionEntity entity) {
        return Resolution.builder()
                .id(entity.getId())
                .meetingId(entity.getMeetingId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .displayOrder(entity.getDisplayOrder())
                .options(entity.getOptions() != null
                        ? entity.getOptions().stream()
                                .map(opt -> VotingOption.builder()
                                        .id(opt.getId())
                                        .name(opt.getName())
                                        .type(opt.getType())
                                        .position(opt.getPosition())
                                        .bio(opt.getBio())
                                        .photoUrl(opt.getPhotoUrl())
                                        .displayOrder(opt.getDisplayOrder())
                                        .build())
                                .collect(Collectors.toList())
                        : Collections.emptyList())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ResolutionEntity toEntity(Resolution domain) {
        return ResolutionEntity.builder()
                .id(domain.getId())
                .meetingId(domain.getMeetingId())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .displayOrder(domain.getDisplayOrder())
                .options(domain.getOptions() != null
                        ? domain.getOptions().stream()
                                .map(opt -> VotingOptionEntity.builder()
                                        .id(opt.getId())
                                        .name(opt.getName())
                                        .type(opt.getType())
                                        .position(opt.getPosition())
                                        .bio(opt.getBio())
                                        .photoUrl(opt.getPhotoUrl())
                                        .displayOrder(opt.getDisplayOrder())
                                        .build())
                                .collect(Collectors.toList())
                        : Collections.emptyList())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
