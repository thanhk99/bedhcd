package com.api.bedhcd.modules.election.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CandidateJpaRepository extends JpaRepository<CandidateEntity, String> {

    // Lấy tất cả candidates theo electionId (FK plain String)
    List<CandidateEntity> findByElectionIdOrderByDisplayOrderAsc(String electionId);

    // Xóa toàn bộ candidates khi xóa election
    @Modifying
    @Query("DELETE FROM CandidateEntity c WHERE c.electionId = :electionId")
    void deleteByElectionId(@Param("electionId") String electionId);

    // Xóa candidate theo candidateId
    @Modifying
    @Query("DELETE FROM CandidateEntity c WHERE c.id = :candidateId")
    void deleteById(@Param("candidateId") String candidateId);
}
