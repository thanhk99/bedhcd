package com.api.bedhcd.modules.election.infrastructure.port;

import com.api.bedhcd.modules.election.application.port.ElectionPort;
import com.api.bedhcd.modules.election.domain.repository.ElectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ElectionPortImpl implements ElectionPort {

    private final ElectionRepository electionRepository;

    @Override
    public List<ElectionSummary> getElectionsByMeetingId(String meetingId) {
        return electionRepository.findByMeetingId(meetingId).stream()
                .map(election -> new ElectionSummary(
                        election.getId(),
                        election.getTitle(),
                        election.getElectionType() != null ? election.getElectionType().name() : null,
                        election.getCandidates() == null ? List.of()
                                : election.getCandidates().stream()
                                        .map(c -> new CandidateSummary(
                                                c.getId(),
                                                c.getName(),
                                                c.getDescription(),
                                                c.getDisplayOrder()))
                                        .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }
}
