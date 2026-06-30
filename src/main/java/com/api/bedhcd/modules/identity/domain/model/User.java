package com.api.bedhcd.modules.identity.domain.model;

import com.api.bedhcd.modules.identity.domain.exception.IdentityException;
import com.api.bedhcd.shared.domain.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
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
    private Set<Role> roles;
    private boolean enabled;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Kiểm tra xem User có quyền SUPER_ADMIN không
     */
    public boolean isSuperAdmin() {
        return roles != null && roles.contains(Role.SUPER_ADMIN);
    }

    /**
     * Kiểm tra xem User có quyền Admin (ADMIN hoặc SUPER_ADMIN) không
     */
    public boolean isAdmin() {
        return roles != null && (roles.contains(Role.ADMIN) || roles.contains(Role.SUPER_ADMIN));
    }

    /**
     * Nghiệp vụ Domain: Cấp một Role cho User này.
     * Chỉ SUPER_ADMIN mới được phép cấp các quyền quản trị (ADMIN / SUPER_ADMIN).
     *
     * @param newRole       Role muốn cấp
     * @param assignerRoles Tập quyền của người đang thực hiện thao tác
     */
    public void assignRole(Role newRole, Set<Role> assignerRoles) {
        boolean assignerIsSuperAdmin = assignerRoles != null && assignerRoles.contains(Role.SUPER_ADMIN);

        if ((newRole == Role.ADMIN || newRole == Role.SUPER_ADMIN) && !assignerIsSuperAdmin) {
            throw IdentityException.accessDenied(
                "Chỉ SUPER_ADMIN mới có quyền cấp tài khoản ADMIN hoặc SUPER_ADMIN."
            );
        }

        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        this.roles.add(newRole);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Nghiệp vụ Domain: Thu hồi một Role khỏi User.
     * Chỉ SUPER_ADMIN mới được phép thu hồi quyền ADMIN / SUPER_ADMIN.
     *
     * @param roleToRevoke  Role muốn thu hồi
     * @param assignerRoles Tập quyền của người đang thực hiện thao tác
     */
    public void revokeRole(Role roleToRevoke, Set<Role> assignerRoles) {
        boolean assignerIsSuperAdmin = assignerRoles != null && assignerRoles.contains(Role.SUPER_ADMIN);

        if ((roleToRevoke == Role.ADMIN || roleToRevoke == Role.SUPER_ADMIN) && !assignerIsSuperAdmin) {
            throw IdentityException.accessDenied(
                "Chỉ SUPER_ADMIN mới có quyền thu hồi quyền ADMIN hoặc SUPER_ADMIN."
            );
        }

        if (this.roles != null) {
            this.roles.remove(roleToRevoke);
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Factory method tạo User mới (mặc định là Shareholder)
     */
    public static User createShareholder(String cccd, String fullName, Long shares) {
        return User.builder()
                .cccd(cccd)
                .fullName(fullName)
                .sharesOwned(shares)
                .roles(Set.of(Role.SHAREHOLDER))
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
