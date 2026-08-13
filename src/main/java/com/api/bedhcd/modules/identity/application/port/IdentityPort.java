package com.api.bedhcd.modules.identity.application.port;

import com.api.bedhcd.shared.domain.enums.Role;
import com.api.bedhcd.shared.dto.UserDTO;

import java.util.Set;

public interface IdentityPort {
    /**
     * Lấy ID của người dùng hiện tại đang đăng nhập
     */
    String getCurrentUserId();

    /**
     * Lấy danh sách vai trò của một người dùng
     */
    Set<Role> getUserRoles(String userId);

    /**
     * Kiểm tra nhanh xem người dùng hiện tại có vai trò nhất định không
     */
    boolean hasRole(Role role);

    /**
     * Tìm userId theo CCCD
     */
    java.util.Optional<String> getUserIdByCccd(String cccd);

    /**
     * Lấy thông tin cơ bản của người dùng cho các module khác
     */
    UserDTO getUserInfo(String userId);
    
    /**
     * Tạo hoặc cập nhật người dùng (dùng trong quá trình import)
     */
    UserDTO createOrUpdateUser(UserDTO user);

    /**
     * Tạo hoặc cập nhật danh sách người dùng theo batch
     */
    java.util.List<UserDTO> createOrUpdateUserBatch(java.util.List<UserDTO> users);

    /**
     * Tìm các tài khoản phiếu con (tách phiếu) thuộc cùng CCCD gốc.
     * Phiếu con có cccd = baseCccd + 1 ký tự chữ cái (A..Z) và splitAccount = true.
     */
    java.util.List<UserDTO> findSplitAccountsByBaseCccd(String baseCccd);

    long countUsers();

    void updateShareholderStatus(String userId, com.api.bedhcd.shared.domain.enums.ShareholderStatus status);

    void updateShareholderStatusBatch(java.util.List<String> userIds, com.api.bedhcd.shared.domain.enums.ShareholderStatus status);
}

