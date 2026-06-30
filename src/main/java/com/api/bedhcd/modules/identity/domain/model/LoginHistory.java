package com.api.bedhcd.modules.identity.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistory {
    private Long id;
    private String userId;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private String ipAddress;
    private String userAgent;
    private String location;
    private LoginStatus status;
    private String failureReason;
    private String sessionToken;
    private LoginMethod loginMethod;
}
