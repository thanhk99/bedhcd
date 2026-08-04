package com.api.bedhcd.modules.admin.domain.exception;

import com.api.bedhcd.shared.exception.BaseDomainException;
import org.springframework.http.HttpStatus;

public class AdminException extends BaseDomainException {

    protected AdminException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static AdminException roleGroupNotFound(String id) {
        return new AdminException(
                HttpStatus.NOT_FOUND,
                "ADMIN_ROLE_GROUP_NOT_FOUND",
                "Role Group not found: " + id
        );
    }

    public static AdminException roleGroupNameExists(String name) {
        return new AdminException(
                HttpStatus.CONFLICT,
                "ADMIN_ROLE_GROUP_NAME_EXISTS",
                "Role Group name already exists: " + name
        );
    }

    public static AdminException roleGroupAssignedToAdmins() {
        return new AdminException(
                HttpStatus.CONFLICT,
                "ADMIN_ROLE_GROUP_ASSIGNED",
                "Cannot delete Role Group because it is assigned to one or more Admins"
        );
    }

    public static AdminException adminNotFound(String adminId) {
        return new AdminException(
                HttpStatus.NOT_FOUND,
                "ADMIN_NOT_FOUND",
                "Admin not found: " + adminId
        );
    }

    public static AdminException cannotDeactivateSuperAdmin() {
        return new AdminException(
                HttpStatus.FORBIDDEN,
                "ADMIN_CANNOT_DEACTIVATE_SUPER_ADMIN",
                "Không thể vô hiệu hoá tài khoản Super Admin"
        );
    }

    public static AdminException onlySuperAdminCanDeactivate() {
        return new AdminException(
                HttpStatus.FORBIDDEN,
                "ADMIN_ONLY_SUPER_ADMIN_CAN_DEACTIVATE",
                "Chỉ Super Admin mới có quyền vô hiệu hoá tài khoản Admin"
        );
    }

    public static AdminException cannotDeleteSuperAdmin() {
        return new AdminException(
                HttpStatus.FORBIDDEN,
                "ADMIN_CANNOT_DELETE_SUPER_ADMIN",
                "Không thể xoá tài khoản Super Admin"
        );
    }

    public static AdminException onlySuperAdminCanDelete() {
        return new AdminException(
                HttpStatus.FORBIDDEN,
                "ADMIN_ONLY_SUPER_ADMIN_CAN_DELETE",
                "Chỉ Super Admin mới có quyền xoá tài khoản Admin"
        );
    }

    public static AdminException incorrectOldPassword() {
        return new AdminException(
                HttpStatus.BAD_REQUEST,
                "ADMIN_INCORRECT_OLD_PASSWORD",
                "Mật khẩu cũ không chính xác"
        );
    }

    public static AdminException newPasswordSameAsOld() {
        return new AdminException(
                HttpStatus.BAD_REQUEST,
                "ADMIN_NEW_PASSWORD_SAME_AS_OLD",
                "Mật khẩu mới không được trùng với mật khẩu cũ"
        );
    }

    public static AdminException weakPassword() {
        return new AdminException(
                HttpStatus.BAD_REQUEST,
                "ADMIN_WEAK_PASSWORD",
                "Mật khẩu yếu. Mật khẩu phải chứa ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số"
        );
    }
}
