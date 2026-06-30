package com.api.bedhcd.modules.election.infrastructure.persistence;

import com.api.bedhcd.modules.election.domain.model.Election;
import com.api.bedhcd.modules.election.domain.repository.ElectionRepository;
import com.api.bedhcd.modules.voting.domain.model.VotingOption;
import com.api.bedhcd.modules.voting.infrastructure.persistence.VotingOptionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ElectionRepositoryImpl implements ElectionRepository {

    private final ElectionJpaRepository jpaRepository;

    @Override
    public Optional<Election> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Election> findByMeetingId(String meetingId) {
        return jpaRepository.findByMeetingId(meetingId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Election save(Election domain) {
        return toDomain(jpaRepository.save(toEntity(domain)));
    }

    private Election toDomain(ElectionEntity entity) {
        return Election.builder()
                .id(entity.getId())
                .meetingId(entity.getMeetingId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .electionType(entity.getElectionType())
                .numSeats(entity.getNumSeats())
                .displayOrder(entity.getDisplayOrder())
                .candidates(entity.getCandidates() != null
                        ? entity.getCandidates().stream()
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

    private ElectionEntity toEntity(Election domain) {
        return ElectionEntity.builder()
                .id(domain.getId())
                .meetingId(domain.getMeetingId())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .electionType(domain.getElectionType())
                .numSeats(domain.getNumSeats())
                .displayOrder(domain.getDisplayOrder())
                .candidates(domain.getCandidates() != null
                        ? domain.getCandidates().stream()
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
