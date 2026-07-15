package com.api.bedhcd.modules.participant.domain.model;

import com.api.bedhcd.shared.domain.enums.DelegationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProxyDelegation {
    private Long id;
    private String meetingId;
    private String delegatorId;
    private String proxyId;
    private Long sharesDelegated;
    private DelegationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime revokedAt;

    public boolean isRevocable() {
        return this.status == DelegationStatus.ACTIVE;
    }

    public void validateEditable() {
        if (this.status != DelegationStatus.ACTIVE) {
            throw new RuntimeException("Chỉ có thể cập nhật uỷ quyền đang hoạt động");
        }
    }
}
