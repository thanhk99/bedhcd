package com.api.bedhcd.modules.election.application.service;

import com.api.bedhcd.modules.election.api.v1.dto.CandidateRequest;
import com.api.bedhcd.modules.election.api.v1.dto.ElectionRequest;
import com.api.bedhcd.modules.election.api.v1.dto.ElectionResponse;
import com.api.bedhcd.modules.election.api.v1.dto.ElectionResultResponse;
import com.api.bedhcd.modules.election.domain.exception.ElectionException;
import com.api.bedhcd.modules.election.domain.model.Election;
import com.api.bedhcd.modules.election.domain.repository.ElectionRepository;
import com.api.bedhcd.modules.identity.application.port.IdentityPort;
import com.api.bedhcd.modules.participant.application.port.ParticipantPort;
import com.api.bedhcd.modules.meeting.application.port.MeetingPort;
import com.api.bedhcd.modules.election.api.v1.dto.ElectionVoteRequest;
import com.api.bedhcd.modules.voting.application.port.OptionVote;
import com.api.bedhcd.modules.voting.application.port.VoteResult;
import com.api.bedhcd.modules.voting.application.port.VotingPort;
import com.api.bedhcd.modules.election.domain.model.Candidate;
import com.api.bedhcd.modules.election.infrastructure.persistence.CandidateJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import com.api.bedhcd.shared.domain.UuidFactory;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ElectionApplicationService {

        private final ElectionRepository electionRepository;
        private final VotingPort votingPort;
        private final IdentityPort identityPort;
        private final ParticipantPort participantPort;
        private final MeetingPort meetingPort;
        private final CandidateJpaRepository candidateJpaRepository;

        @Transactional(readOnly = true)
        public List<ElectionResponse> getByMeeting(String meetingId) {
                // Nếu người dùng là cổ đông, kiểm tra quyền xem
                String currentUserId = identityPort.getCurrentUserId();
                if (currentUserId != null && !identityPort.hasRole(com.api.bedhcd.shared.domain.enums.Role.ADMIN)) {
                        if (!meetingPort.canViewResolutionOrElection(meetingId)) {
                                throw ElectionException.invalidState(
                                                "Cấu hình cuộc họp hiện tại không cho phép cổ đông xem nội dung bầu cử.");
                        }
                }
                return electionRepository.findByMeetingId(meetingId).stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public ElectionResponse createElection(String meetingId, ElectionRequest request) {
                if (!meetingPort.canAddResolutionOrElection(meetingId)) {
                        throw ElectionException.invalidState(
                                        "Trạng thái cuộc họp hiện tại không cho phép thêm phiên bầu cử mới.");
                }

                boolean typeExists = electionRepository.findByMeetingId(meetingId).stream()
                                .anyMatch(e -> e.getElectionType() == request.getType());

                if (typeExists) {
                        throw ElectionException.electionAlreadyExists(meetingId, request.getType().name());
                }

                Election election = Election.builder()
                                .id(UuidFactory.generate())
                                .meetingId(meetingId)
                                .title(request.getTitle())
                                .description(request.getDescription())
                                .electionType(request.getType())
                                .displayOrder(request.getDisplayOrder())
                                .createdAt(LocalDateTime.now())
                                .build();
                return toResponse(electionRepository.save(election));
        }

        @Transactional
        public ElectionResponse updateElection(String electionId, ElectionRequest request) {
                Election election = electionRepository.findById(electionId)
                                .orElseThrow(() -> ElectionException.electionNotFound(electionId));

                if (!meetingPort.canEditResolutionOrElection(election.getMeetingId())) {
                        throw ElectionException.invalidState(
                                        "Trạng thái cuộc họp hiện tại không cho phép chỉnh sửa phiên bầu cử.");
                }

                election.setTitle(request.getTitle());
                election.setDescription(request.getDescription());
                election.setElectionType(request.getType());
                election.setDisplayOrder(request.getDisplayOrder());
                return toResponse(electionRepository.save(election));
        }

        @Transactional
        public ElectionResponse addCandidate(String electionId, CandidateRequest request) {
                Election election = electionRepository.findById(electionId)
                                .orElseThrow(() -> ElectionException.electionNotFound(electionId));

                if (!meetingPort.canEditResolutionOrElection(election.getMeetingId())) {
                        throw ElectionException
                                        .invalidState("Trạng thái cuộc họp hiện tại không cho phép thêm ứng cử viên.");
                }

                int nextOrder = request.getDisplayOrder() != null
                                ? request.getDisplayOrder()
                                : (election.getCandidates() != null ? election.getCandidates().size() + 1 : 1);

                Candidate candidate = Candidate.builder()
                                .id(UuidFactory.generate())
                                .name(request.getFullName())
                                .description(request.getDescription())
                                .bio(request.getDescription())
                                .displayOrder(nextOrder)
                                .build();

                election.getCandidates().add(candidate);
                return toResponse(electionRepository.save(election));
        }

        @Transactional
        public ElectionResponse updateCandidate(String electionId, String candidateId, CandidateRequest request) {
                Election election = electionRepository.findById(electionId)
                                .orElseThrow(() -> ElectionException.electionNotFound(electionId));

                if (!meetingPort.canEditResolutionOrElection(election.getMeetingId())) {
                        throw ElectionException.invalidState(
                                        "Trạng thái cuộc họp hiện tại không cho phép chỉnh sửa ứng cử viên.");
                }

                Candidate candidate = election.getCandidates().stream()
                                .filter(c -> c.getId().equals(candidateId))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("Candidate not found: " + candidateId));

                if (request.getFullName() != null) {
                        candidate.setName(request.getFullName());
                }
                if (request.getDescription() != null) {
                        candidate.setDescription(request.getDescription());
                        candidate.setBio(request.getDescription());
                }
                if (request.getDisplayOrder() != null) {
                        candidate.setDisplayOrder(request.getDisplayOrder());
                }

                return toResponse(electionRepository.save(election));
        }

        @Transactional
        public void submitVote(String electionId, ElectionVoteRequest request) {
                String userId = identityPort.getCurrentUserId();
                if (userId == null)
                        throw ElectionException.unauthorized();

                Election election = electionRepository.findById(electionId)
                                .orElseThrow(() -> ElectionException.electionNotFound(electionId));

                if (!meetingPort.canVote(election.getMeetingId())) {
                        throw ElectionException
                                        .invalidState("Cấu hình cuộc họp hiện tại không cho phép cổ đông bỏ phiếu.");
                }

                if (!participantPort.isPrinted(election.getMeetingId(), userId)) {
                        throw ElectionException.invalidState("Cổ đông chưa in phiếu tham dự, không thể bỏ phiếu bầu.");
                }

                long basePower = participantPort.getVotingPower(election.getMeetingId(), userId);
                long totalPower = election.calculateVotingPower(basePower);

                if (request.getOptionVotes() == null || request.getOptionVotes().isEmpty()) {
                        throw ElectionException.invalidVoteDistribution("Phải chọn ít nhất một ứng viên");
                }

                List<OptionVote> optionVotes = request.getOptionVotes().stream()
                                .filter(opt -> election.getCandidates().stream()
                                                .anyMatch(c -> c.getId().equals(opt.getCandidateId())))
                                .map(opt -> OptionVote.builder()
                                                .optionId(opt.getCandidateId())
                                                .weight(opt.getVoteWeight())
                                                .build())
                                .collect(Collectors.toList());

                long distributedPower = optionVotes.stream().mapToLong(OptionVote::getWeight).sum();
                if (distributedPower > totalPower) {
                        throw ElectionException.invalidVoteDistribution("Tổng số quyền biểu quyết phân bổ ("
                                        + distributedPower + ") vượt quá số quyền biểu quyết hợp lệ (" + totalPower
                                        + ")");
                }

                votingPort.submitVotes(electionId, userId, optionVotes);
        }

        @Transactional(readOnly = true)
        public ElectionResultResponse getResults(String electionId) {
                Election election = electionRepository.findById(electionId)
                                .orElseThrow(() -> ElectionException.electionNotFound(electionId));

                List<VoteResult> portResults = votingPort.getVotesByTarget(electionId);

                List<ElectionResultResponse.CandidateResult> results;
                if (election.getCandidates() != null) {
                        results = election.getCandidates().stream()
                                        .map(cand -> {
                                                VoteResult pr = portResults.stream()
                                                                .filter(r -> r.getOptionId().equals(cand.getId()))
                                                                .findFirst().orElse(null);
                                                long weight = pr != null ? pr.getTotalWeight() : 0;
                                                long count = pr != null ? pr.getVoteCount() : 0;
                                                return ElectionResultResponse.CandidateResult.builder()
                                                                .candidateId(cand.getId())
                                                                .candidateName(cand.getName())
                                                                .voteCount(count)
                                                                .totalWeight(weight)
                                                                .build();
                                        }).collect(Collectors.toList());
                } else {
                        results = Collections.emptyList();
                }

                return ElectionResultResponse.builder()
                                .electionId(electionId)
                                .title(election.getTitle())
                                .results(results)
                                .totalWeight(results.stream()
                                                .mapToLong(ElectionResultResponse.CandidateResult::getTotalWeight)
                                                .sum())
                                .totalVoters(votingPort.countVotersByTarget(electionId))
                                .build();
        }

        @Transactional
        public void deleteElection(String meetingId, String electionId) {
                Election election = electionRepository.findById(electionId)
                                .orElseThrow(() -> ElectionException.electionNotFound(electionId));
                if (!election.getMeetingId().equals(meetingId)) {
                        throw ElectionException.electionNotFound(electionId);
                }

                if (!meetingPort.canEditResolutionOrElection(meetingId)) {
                        throw ElectionException
                                        .invalidState("Trạng thái cuộc họp hiện tại không cho phép xóa phiên bầu cử.");
                }

                election.validateCanBeDeleted(votingPort.countVotersByTarget(electionId));

                electionRepository.delete(election);
        }

        @Transactional
        public void deleteCandidate(String electionId, String candidateId) {
                Election election = electionRepository.findById(electionId)
                                .orElseThrow(() -> ElectionException.electionNotFound(electionId));

                if (!meetingPort.canEditResolutionOrElection(election.getMeetingId())) {
                        throw ElectionException.invalidState(
                                        "Trạng thái cuộc họp hiện tại không cho phép xóa ứng viên.");
                }

                // Kiểm tra logic business rule trong domain model
                election.validateCandidateCanBeDeleted(candidateId);

                // Xóa candidate khỏi database
                candidateJpaRepository.deleteById(candidateId);

                // Loại bỏ candidate khỏi danh sách (nếu vẫn còn cần)
                if (election.getCandidates() != null) {
                        election.getCandidates().removeIf(c -> c.getId().equals(candidateId));
                }

                electionRepository.save(election);
        }

        private ElectionResponse toResponse(Election domain) {
                return ElectionResponse.builder()
                                .id(domain.getId())
                                .meetingId(domain.getMeetingId())
                                .title(domain.getTitle())
                                .description(domain.getDescription())
                                .type(domain.getElectionType())
                                .displayOrder(domain.getDisplayOrder())
                                .candidates(domain.getCandidates() == null ? Collections.emptyList()
                                                : domain.getCandidates().stream()
                                                                .map(c -> ElectionResponse.CandidateResponse.builder()
                                                                                .id(c.getId())
                                                                                .fullName(c.getName())
                                                                                .description(c.getBio())
                                                                                .build())
                                                                .collect(Collectors.toList()))
                                .build();
        }
}
