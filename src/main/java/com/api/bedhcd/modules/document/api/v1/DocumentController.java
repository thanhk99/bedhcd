package com.api.bedhcd.modules.document.api.v1;
import com.api.bedhcd.modules.document.api.v1.dto.DocumentResponse;
import com.api.bedhcd.modules.document.application.service.DocumentApplicationService;
import com.api.bedhcd.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/meeting")
@Tag(name = "Document Management")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentApplicationService documentService;

    @Operation(summary = "Lấy danh sách tài liệu của cuộc họp")
    @GetMapping("/{id}/documents")
    public ApiResponse<List<DocumentResponse>> getMeetingDocuments(@PathVariable String id) {
        return ApiResponse.success(documentService.getDocumentsByMeetingId(id));
    }
}
