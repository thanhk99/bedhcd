package com.api.bedhcd.modules.document.infrastructure.persistence;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "documents")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DocumentEntity {
    @Id private String id;
    @Column(nullable = false) private String meetingId;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "TEXT") private String fileUrl;
    private String fileName;
    private Integer displayOrder;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
