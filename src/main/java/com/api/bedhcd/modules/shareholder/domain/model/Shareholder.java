package com.api.bedhcd.modules.shareholder.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shareholder {
    private String id;
    private String username;
    private String phoneNumber;
    private String investorCode;
    private String cccd;
    private String fullName;
    private String email;
    private String address;
    private String password;
    private Long sharesOwned;
    private boolean enabled;
    private boolean splitAccount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    /**
     * Factory method tạo Shareholder mới
     */
    public static Shareholder createShareholder(String id, String cccd, String fullName, Long shares) {
        return Shareholder.builder()
                .id(id)
                .cccd(cccd)
                .fullName(fullName)
                .sharesOwned(shares != null ? shares : 0L)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
