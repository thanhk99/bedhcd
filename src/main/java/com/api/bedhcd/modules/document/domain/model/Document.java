package com.api.bedhcd.modules.document.domain.model;
import lombok.*;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Document {
    private String id;
    private String meetingId;
    private String title;
    private String fileUrl;
    private String fileName;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public void update(String title, String fileUrl, String fileName, Integer displayOrder) {
        this.title = title;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.displayOrder = displayOrder;
    }
}
