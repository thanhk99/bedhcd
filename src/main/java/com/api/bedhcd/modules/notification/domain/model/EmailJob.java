package com.api.bedhcd.modules.notification.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailJob {
    private String id;
    private String toEmail;
    private String subject;
    private String body;
    private EmailJobStatus status;
    private int retryCount;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;

    public static EmailJob createNew(String toEmail, String subject, String body) {
        return EmailJob.builder()
                .id(UUID.randomUUID().toString())
                .toEmail(toEmail)
                .subject(subject)
                .body(body)
                .status(EmailJobStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void markSent() {
        this.status = EmailJobStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = EmailJobStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}
