package com.api.bedhcd.modules.audit.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    private String id;
    private String actorId;
    private String actorName;
    private String action;
    private String resource;
    private String targetId;
    private String payload;
    private String ipAddress;
    private LocalDateTime createdAt;
}
