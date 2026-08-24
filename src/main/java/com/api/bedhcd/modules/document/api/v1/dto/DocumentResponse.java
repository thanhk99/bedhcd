package com.api.bedhcd.modules.document.api.v1.dto;
import lombok.*;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DocumentResponse {
    private String id;
    private String meetingId;
    private String title;
    private String fileUrl;
    private String fileName;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
