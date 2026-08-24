package com.api.bedhcd.modules.document.application.mapper;
import com.api.bedhcd.modules.document.api.v1.dto.DocumentResponse;
import com.api.bedhcd.modules.document.domain.model.Document;
import org.springframework.stereotype.Component;
@Component
public class DocumentMapper {
    public DocumentResponse toResponse(Document domain) {
        return DocumentResponse.builder()
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
}
