package com.api.bedhcd.modules.document.application.service;
import com.api.bedhcd.modules.document.api.v1.dto.DocumentRequest;
import com.api.bedhcd.modules.document.api.v1.dto.DocumentResponse;
import com.api.bedhcd.modules.document.application.mapper.DocumentMapper;
import com.api.bedhcd.modules.document.domain.exception.DocumentException;
import com.api.bedhcd.modules.document.domain.model.Document;
import com.api.bedhcd.modules.document.domain.repository.DocumentRepository;
import com.api.bedhcd.shared.domain.UuidFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentApplicationService {
    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;

    public List<DocumentResponse> getDocumentsByMeetingId(String meetingId) {
        return documentRepository.findByMeetingId(meetingId)
            .stream().map(documentMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public DocumentResponse createDocument(String meetingId, DocumentRequest request) {
        Document document = Document.builder()
            .id(UuidFactory.generate())
            .meetingId(meetingId)
            .title(request.getTitle())
            .fileUrl(request.getFileUrl())
            .fileName(request.getFileName())
            .displayOrder(request.getDisplayOrder())
            .build();
        Document saved = documentRepository.save(document);
        return documentMapper.toResponse(saved);
    }

    @Transactional
    public DocumentResponse updateDocument(String meetingId, String documentId, DocumentRequest request) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> DocumentException.notFound(documentId));
        document.update(request.getTitle(), request.getFileUrl(), request.getFileName(), request.getDisplayOrder());
        Document saved = documentRepository.save(document);
        return documentMapper.toResponse(saved);
    }

    @Transactional
    public void deleteDocument(String meetingId, String documentId) {
        documentRepository.deleteById(documentId);
    }
}
