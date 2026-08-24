package com.api.bedhcd.modules.document.api.v1.dto;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DocumentRequest {
    private String title;
    private String fileUrl;
    private String fileName;
    private Integer displayOrder;
}
