package com.api.bedhcd.modules.voting.infrastructure.persistence;

import com.api.bedhcd.modules.voting.domain.model.Vote;
import com.api.bedhcd.modules.voting.domain.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class VoteRepositoryImpl implements VoteRepository {

    private final VoteJpaRepository jpaRepository;

    @Override
    public List<Vote> findByResolutionAndUser(String resolutionId, String userId) {
        return jpaRepository.findByResolutionIdAndUserId(resolutionId, userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Vote save(Vote domain) {
        return toDomain(jpaRepository.save(toEntity(domain)));
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

@Override
    public List<Vote> findByResolution(String resolutionId) {
        return jpaRepository.findByResolutionId(resolutionId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Vote> findByElection(String electionId) {
        return jpaRepository.findByElectionId(electionId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Vote> findByUser(String userId) {
        return jpaRepository.findByUserIdOrderByVotedAtDesc(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public long countByMeetingId(String meetingId) {
        return jpaRepository.countByMeetingId(meetingId);
    }

    @Override
    public void deleteByMeetingIdAndUserIdForElection(String meetingId, String userId) {
        jpaRepository.deleteByMeetingIdAndUserIdForElection(meetingId, userId);
    }

    @Override
    public void deleteByMeetingIdAndUserIdForResolution(String meetingId, String userId) {
        jpaRepository.deleteByMeetingIdAndUserIdForResolution(meetingId, userId);
    }

    private Vote toDomain(VoteEntity entity) {
        return Vote.builder()
                .id(entity.getId())
                .resolutionId(entity.getResolutionId())
                .electionId(entity.getElectionId())
                .userId(entity.getUserId())
                .votingOptionId(entity.getVotingOptionId())
                .voteWeight(entity.getVoteWeight())
                .isProxyVote(entity.isProxyVote())
                .proxyFromUserId(entity.getProxyFromUserId())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .votedAt(entity.getVotedAt())
                .build();
    }

    private VoteEntity toEntity(Vote domain) {
        return VoteEntity.builder()
                .id(domain.getId())
                .resolutionId(domain.getResolutionId())
                .electionId(domain.getElectionId())
                .userId(domain.getUserId())
                .votingOptionId(domain.getVotingOptionId())
                .voteWeight(domain.getVoteWeight())
                .isProxyVote(domain.isProxyVote())
                .proxyFromUserId(domain.getProxyFromUserId())
                .ipAddress(domain.getIpAddress())
                .userAgent(domain.getUserAgent())
                .votedAt(domain.getVotedAt())
                .build();
    }
}
