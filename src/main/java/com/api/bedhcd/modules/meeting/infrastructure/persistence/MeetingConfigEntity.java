package com.api.bedhcd.modules.meeting.infrastructure.persistence;

import com.api.bedhcd.shared.domain.enums.MeetingStatus;
import com.api.bedhcd.modules.meeting.domain.model.MeetingRules;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "meeting_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingConfigEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    private String description;

    /**
     * Lưu trữ toàn bộ cấu hình quy tắc dưới dạng JSONB trong Postgres
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, MeetingRules> stateConfigs;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
