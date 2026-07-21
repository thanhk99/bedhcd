package com.api.bedhcd.modules.election.infrastructure.persistence;

import com.api.bedhcd.shared.domain.enums.ElectionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "elections")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectionEntity {

    @Id
    private String id;

    @Column(name = "meeting_id", nullable = false)
    private String meetingId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private ElectionType electionType;

    private Integer displayOrder;

    // Đúng rule: KHÔNG dùng @OneToMany — candidates được quản lý riêng qua CandidateJpaRepository
    // Dùng @PrePersist/@PreUpdate để quản lý timestamp
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
