package com.api.bedhcd.shared.infrastructure.monitor;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RequestLogEntry {
    private String id;
    private LocalDateTime timestamp;
    private String method;
    private String uri;
    private String username;
    private String ipAddress;
    private Integer statusCode;
    private Long durationMs;
    private String errorMessage;
    private String module; // tên module được gọi, ví dụ: admin, election,...
}
