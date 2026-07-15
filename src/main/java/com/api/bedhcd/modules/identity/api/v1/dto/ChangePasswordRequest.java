package com.api.bedhcd.modules.identity.api.v1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank(message = "Mật khẩu cũ không được để trống")
    private String oldPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[\\W_]).{7,}$", message = "Mật khẩu phải có chữ hoa, thường, ký tự đặc biệt và dài hơn 6 ký tự")
    private String newPassword;
}
