package com.api.bedhcd.modules.shareholder.domain.model;

import com.api.bedhcd.modules.shareholder.domain.exception.ShareholderException;
import com.api.bedhcd.shared.domain.enums.ShareholderStatus;
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
    private ShareholderStatus status;

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
                .status(ShareholderStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Nghiệp vụ Domain: Kiểm tra xem thông tin cổ đông có thể chỉnh sửa không.
     * Chỉ cho phép chỉnh sửa khi đang ở trạng thái ACTIVE.
     * Ném ShareholderException nếu bị khoá hoặc hết hạn.
     */
    public void validateEditable() {
        if (this.status == ShareholderStatus.LOCKED) {
            throw ShareholderException.locked("Thông tin cổ đông đã bị chốt (đã gửi email). Không thể chỉnh sửa.");
        }
        if (this.status == ShareholderStatus.EXPIRED) {
            throw ShareholderException.locked("Thông tin cổ đông đã hết hạn. Không thể chỉnh sửa.");
        }
    }

    /**
     * Nghiệp vụ Domain: Chốt thông tin cổ đông (sau khi đã gửi email).
     */
    public void lockInfo() {
        this.status = ShareholderStatus.LOCKED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Nghiệp vụ Domain: Đánh dấu cổ đông hết hạn (khi cuộc họp hoàn tất).
     */
    public void expire() {
        this.status = ShareholderStatus.EXPIRED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Nghiệp vụ Domain: Kiểm tra xem cổ đông có thể bị xóa không.
     * Chỉ không cho phép xóa khi đang ở trạng thái LOCKED (đã gửi email).
     */
    public void validateDeletable() {
        if (this.status == ShareholderStatus.LOCKED) {
            throw ShareholderException.locked("Không thể xóa cổ đông đã được chốt (đã gửi email). Vui lòng liên hệ quản trị viên.");
        }
    }
}
