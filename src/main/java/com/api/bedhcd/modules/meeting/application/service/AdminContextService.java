package com.api.bedhcd.modules.meeting.application.service;

import com.api.bedhcd.modules.admin.infrastructure.persistence.entity.AdminEntity;
import com.api.bedhcd.modules.admin.infrastructure.persistence.repository.AdminJpaRepository;
import com.api.bedhcd.modules.meeting.domain.exception.MeetingException;
import com.api.bedhcd.shared.domain.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service helper để lấy thông tin admin đang đăng nhập từ SecurityContext.
 * Được dùng trong Application Service để phân quyền thực hiện hành động.
 */
@Service
@RequiredArgsConstructor
public class AdminContextService {

    private final AdminJpaRepository adminJpaRepository;

    /**
     * Lấy ID của admin đang đăng nhập hiện tại.
     */
    public String getCurrentAdminId() {
        AdminEntity admin = getCurrentAdminEntity();
        return admin.getId();
    }

    /**
     * Kiểm tra admin đang đăng nhập có phải SUPER_ADMIN không.
     */
    public boolean isCurrentAdminSuperAdmin() {
        AdminEntity admin = getCurrentAdminEntity();
        return Role.SUPER_ADMIN.equals(admin.getRole());
    }

    /**
     * Lấy username của admin đang đăng nhập.
     */
    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw MeetingException.accessDenied("Chưa xác thực");
        }
        return auth.getName();
    }

    private AdminEntity getCurrentAdminEntity() {
        String username = getCurrentUsername();
        return adminJpaRepository.findByUsername(username)
                .orElseThrow(() -> MeetingException.accessDenied("Không tìm thấy thông tin admin: " + username));
    }
}
