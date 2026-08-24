package com.api.bedhcd.modules.document.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface DocumentJpaRepository extends JpaRepository<DocumentEntity, String> {
    List<DocumentEntity> findByMeetingIdOrderByDisplayOrderAsc(String meetingId);
    void deleteByMeetingId(String meetingId);
}
