package com.api.bedhcd.modules.shareholder.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShareholderJpaRepository extends JpaRepository<ShareholderEntity, String> {
       Optional<ShareholderEntity> findById(String id);
       Optional<ShareholderEntity> findByCccd(String cccd);
       Optional<ShareholderEntity> findByUsername(String username);

       @Query("SELECT s FROM ShareholderEntity s WHERE s.splitAccount = false AND (" +
                     "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(s.cccd) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(s.investorCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
       List<ShareholderEntity> searchByKeyword(@Param("keyword") String keyword);

       @Query("SELECT s FROM ShareholderEntity s WHERE s.splitAccount = false AND (" +
                     "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(s.cccd) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(s.investorCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
       Page<ShareholderEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

       @Query("SELECT s FROM ShareholderEntity s WHERE s.splitAccount = false AND " +
                     "LOWER(s.cccd) LIKE LOWER(CONCAT(:keyword, '%'))")
       List<ShareholderEntity> searchTop10ByKeyword(@Param("keyword") String keyword, Pageable pageable);

       // Tìm kiếm và lọc theo meetingId (cả active và không active)
       @Query("SELECT s FROM ShareholderEntity s WHERE s.splitAccount = false AND " +
              "(:meetingId IS NULL OR s.id IN (SELECT p.userId FROM ParticipantEntity p WHERE p.meetingId = :meetingId AND p.splitTicket = false)) AND " +
              "(:keyword IS NULL OR :keyword = '' OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
              "LOWER(s.cccd) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
              "LOWER(s.investorCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
       Page<ShareholderEntity> searchByKeywordAndMeetingId(@Param("keyword") String keyword, @Param("meetingId") String meetingId, Pageable pageable);

       @Query("SELECT COUNT(s) FROM ShareholderEntity s WHERE s.splitAccount = false AND " +
              "(:meetingId IS NULL OR s.id IN (SELECT p.userId FROM ParticipantEntity p WHERE p.meetingId = :meetingId AND p.splitTicket = false)) AND " +
              "(:keyword IS NULL OR :keyword = '' OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
              "LOWER(s.cccd) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
              "LOWER(s.investorCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
       long countByKeywordAndMeetingId(@Param("keyword") String keyword, @Param("meetingId") String meetingId);

       Page<ShareholderEntity> findAllBySplitAccountFalse(Pageable pageable);

       long countBySplitAccountFalse();
}
