package com.api.bedhcd.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminQrGenerateRequest {
    private String userId;
    private java.time.LocalDateTime expiresAt;
}
