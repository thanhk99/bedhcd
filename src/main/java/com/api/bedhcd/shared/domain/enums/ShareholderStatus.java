package com.api.bedhcd.shared.domain.enums;

public enum ShareholderStatus {
    /**
     * Hoạt động: Trạng thái mặc định sau khi import. Cho phép chỉnh sửa thông tin.
     */
    ACTIVE,

    /**
     * Đã chốt: Sau khi đã gửi email cho cổ đông. Không cho phép chỉnh sửa thông tin,
     * chỉ cho phép đổi mật khẩu.
     */
    LOCKED,

    /**
     * Hết hạn: Khi cuộc họp hoàn tất. Có thể được kích hoạt lại (về ACTIVE)
     * khi import trùng thông tin vào cuộc họp mới.
     */
    EXPIRED
}
