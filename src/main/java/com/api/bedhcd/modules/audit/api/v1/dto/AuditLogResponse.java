package com.api.bedhcd.modules.audit.api.v1.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogResponse {
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
