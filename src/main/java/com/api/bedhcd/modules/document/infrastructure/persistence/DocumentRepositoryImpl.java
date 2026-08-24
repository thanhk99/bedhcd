package com.api.bedhcd.modules.document.infrastructure.persistence;
import com.api.bedhcd.modules.document.domain.model.Document;
import com.api.bedhcd.modules.document.domain.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DocumentRepositoryImpl implements DocumentRepository {
    private final DocumentJpaRepository jpaRepository;

    private Document toDomain(DocumentEntity entity) {
        return Document.builder()
            .id(entity.getId())
            .meetingId(entity.getMeetingId())
            .title(entity.getTitle())
            .fileUrl(entity.getFileUrl())
            .fileName(entity.getFileName())
            .displayOrder(entity.getDisplayOrder())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    private DocumentEntity toEntity(Document domain) {
        return DocumentEntity.builder()
            .id(domain.getId())
            .meetingId(domain.getMeetingId())
            .title(domain.getTitle())
            .fileUrl(domain.getFileUrl())
            .fileName(domain.getFileName())
            .displayOrder(domain.getDisplayOrder())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .build();
    }

    @Override
    public Document save(Document document) {
        DocumentEntity entity = jpaRepository.save(toEntity(document));
        return toDomain(entity);
    }

    @Override
    public Optional<Document> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Document> findByMeetingId(String meetingId) {
        return jpaRepository.findByMeetingIdOrderByDisplayOrderAsc(meetingId)
            .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void deleteByMeetingId(String meetingId) {
        jpaRepository.deleteByMeetingId(meetingId);
    }
}
