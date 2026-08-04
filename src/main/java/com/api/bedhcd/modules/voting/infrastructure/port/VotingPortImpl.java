package com.api.bedhcd.modules.voting.infrastructure.port;

import com.api.bedhcd.modules.voting.application.port.OptionVote;
import com.api.bedhcd.modules.voting.application.port.VoteResult;
import com.api.bedhcd.modules.voting.application.port.VotingPort;
import com.api.bedhcd.modules.voting.domain.model.Vote;
import com.api.bedhcd.modules.voting.domain.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VotingPortImpl implements VotingPort {

    private final VoteRepository voteRepository;

    @Override
    public long countVotes() {
        return voteRepository.count();
    }

    @Override
    public long countVotesByMeetingId(String meetingId) {
        return voteRepository.countByMeetingId(meetingId);
    }

    @Override
    @Transactional
    public void submitVotes(String targetId, String userId, List<OptionVote> votes) {
        // Find existing votes
        List<Vote> oldVotes = voteRepository.findByResolutionAndUser(targetId, userId);
        oldVotes.forEach(v -> voteRepository.delete(v.getId()));

        for (OptionVote optVote : votes) {
            if (optVote.getWeight() == 0) continue;
            
            Vote vote = Vote.builder()
                    .resolutionId(targetId) // Using resolutionId as the generic targetId for now
                    .votingOptionId(optVote.getOptionId())
                    .userId(userId)
                    .voteWeight(optVote.getWeight())
                    .votedAt(LocalDateTime.now())
                    .build();
            voteRepository.save(vote);
        }
    }

    @Override
    @Transactional
    public void deleteVotesByTarget(String targetId, String userId) {
        List<Vote> oldVotes = voteRepository.findByResolutionAndUser(targetId, userId);
        oldVotes.forEach(v -> voteRepository.delete(v.getId()));
    }

    @Override
    public List<VoteResult> getVotesByTarget(String targetId) {
        // Check if this is a resolution or election by looking at the vote structure
        // For now, we'll try to find votes by resolution first, then by election
        List<Vote> votes = voteRepository.findByResolution(targetId);
        
        // If no votes found by resolution, try finding by election (for backward compatibility)
        if (votes.isEmpty()) {
            votes = voteRepository.findByElection(targetId);
        }
        
        // Group by optionId and sum weights/counts
        return votes.stream()
            .collect(Collectors.groupingBy(Vote::getVotingOptionId))
            .entrySet().stream()
            .map(e -> VoteResult.builder()
                .optionId(e.getKey())
                .totalWeight(e.getValue().stream().mapToLong(Vote::getVoteWeight).sum())
                .voteCount(e.getValue().size())
                .build())
            .collect(Collectors.toList());
    }

    @Override
    public List<Vote> getVotesByUser(String userId) {
        return voteRepository.findByUser(userId);
    }

    @Override
    public long countVotersByTarget(String targetId) {
        return voteRepository.findByResolution(targetId).stream()
                .map(Vote::getUserId)
                .distinct()
                .count();
    }
}
