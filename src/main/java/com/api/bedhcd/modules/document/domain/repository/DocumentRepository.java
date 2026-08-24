package com.api.bedhcd.modules.document.domain.repository;
import com.api.bedhcd.modules.document.domain.model.Document;
import java.util.List;
import java.util.Optional;
public interface DocumentRepository {
    Document save(Document document);
    Optional<Document> findById(String id);
    List<Document> findByMeetingId(String meetingId);
    void deleteById(String id);
    void deleteByMeetingId(String meetingId);
}
