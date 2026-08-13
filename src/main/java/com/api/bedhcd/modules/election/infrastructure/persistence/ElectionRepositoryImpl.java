package com.api.bedhcd.modules.election.infrastructure.persistence;

import com.api.bedhcd.modules.election.domain.model.Candidate;
import com.api.bedhcd.modules.election.domain.model.Election;
import com.api.bedhcd.modules.election.domain.repository.ElectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ElectionRepositoryImpl implements ElectionRepository {

        private final ElectionJpaRepository jpaRepository;
        private final CandidateJpaRepository candidateJpaRepository;

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
        @Transactional
        public Election save(Election domain) {
                // Bước 1: Lưu ElectionEntity
                ElectionEntity entity = toEntity(domain);
                ElectionEntity saved = jpaRepository.save(entity);

                // Bước 2: Xử lý candidates - cần kiểm tra các candidate bị loại bỏ và xóa khỏi DB nếu có
                if (domain.getCandidates() != null && !domain.getCandidates().isEmpty()) {
                        List<CandidateEntity> candidateEntities = domain.getCandidates().stream()
                                        .map(c -> toCandidateEntity(c, saved.getId()))
                                        .collect(Collectors.toList());

                        // Lưu các candidate mới/update
                        candidateJpaRepository.saveAll(candidateEntities);
                } else {
                        // Nếu không còn candidates nào, xóa toàn bộ candidate cũ
                        candidateJpaRepository.deleteByElectionId(saved.getId());
                }

                return toDomain(saved);
        }

        @Override
        public void delete(Election domain) {
                candidateJpaRepository.deleteByElectionId(domain.getId());
                jpaRepository.deleteById(domain.getId());
        }

        // Chuyển Entity -> Domain Model
        private Election toDomain(ElectionEntity entity) {
                List<CandidateEntity> candidates = candidateJpaRepository
                                .findByElectionIdOrderByDisplayOrderAsc(entity.getId());

                return Election.builder()
                                .id(entity.getId())
                                .meetingId(entity.getMeetingId())
                                .title(entity.getTitle())
                                .description(entity.getDescription())
                                .electionType(entity.getElectionType())
                                .displayOrder(entity.getDisplayOrder())
                                .candidates(candidates.isEmpty() ? new ArrayList<>()
                                                : candidates.stream()
                                                                .map(c -> Candidate.builder()
                                                                                .id(c.getId())
                                                                                .name(c.getName())
                                                                                .bio(c.getBio())
                                                                                .description(c.getDescription())
                                                                                .displayOrder(c.getDisplayOrder())
                                                                                .build())
                                                                .collect(Collectors.toList()))
                                .createdAt(entity.getCreatedAt())
                                .updatedAt(entity.getUpdatedAt())
                                .build();
        }

        // Chuyển Domain Model -> Entity (đúng rule: đơn giản, không có logic phức tạp)
        private ElectionEntity toEntity(Election domain) {
                return ElectionEntity.builder()
                                .id(domain.getId())
                                .meetingId(domain.getMeetingId())
                                .title(domain.getTitle())
                                .description(domain.getDescription())
                                .electionType(domain.getElectionType())
                                .displayOrder(domain.getDisplayOrder())
                                .createdAt(domain.getCreatedAt())
                                .updatedAt(domain.getUpdatedAt())
                                .build();
        }

        // Chuyển Candidate domain -> CandidateEntity với electionId
        private CandidateEntity toCandidateEntity(Candidate candidate, String electionId) {
                return CandidateEntity.builder()
                                .id(candidate.getId())
                                .electionId(electionId)
                                .name(candidate.getName())
                                .bio(candidate.getBio())
                                .description(candidate.getDescription())
                                .displayOrder(candidate.getDisplayOrder())
                                .build();
        }
}
