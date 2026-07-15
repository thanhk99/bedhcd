package com.api.bedhcd.modules.notification.infrastructure.persistence.entity;

import com.api.bedhcd.modules.notification.domain.model.EmailJobStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailJobEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String toEmail;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailJobStatus status;

    private int retryCount;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}
