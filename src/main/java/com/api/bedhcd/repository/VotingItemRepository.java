package com.api.bedhcd.repository;

import com.api.bedhcd.entity.VotingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VotingItemRepository extends JpaRepository<VotingItem, Long> {
    List<VotingItem> findByMeeting_Id(Long meetingId);
}
