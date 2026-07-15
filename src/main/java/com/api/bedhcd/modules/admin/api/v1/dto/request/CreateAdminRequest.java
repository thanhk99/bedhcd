package com.api.bedhcd.modules.admin.api.v1.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.util.Set;

@Data
public class CreateAdminRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Pattern(regexp = "^[\\p{L}]+$", message = "Tên đăng nhập chỉ được chứa chữ cái (không có số và ký tự đặc biệt)")
    private String username;

    @Pattern(regexp = "^$|^(?=.*[a-z])(?=.*[A-Z])(?=.*[\\W_]).{7,}$", message = "Mật khẩu phải có chữ hoa, thường, ký tự đặc biệt và dài hơn 6 ký tự")
    private String password;

    @NotBlank(message = "Họ và tên không được để trống")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "Họ và tên chỉ được chứa chữ cái và khoảng trắng (không có số và ký tự đặc biệt)")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "Email phải đúng định dạng, bao gồm @ và dấu .")
    private String email;

    @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại phải bao gồm đúng 10 chữ số")
    private String phoneNumber;

    private java.util.List<String> roleGroupIds;

    @Pattern(regexp = "^[\\p{L}\\s]*$", message = "Phòng ban chỉ được chứa chữ cái và khoảng trắng")
    private String department;

    @Pattern(regexp = "^[\\p{L}\\s]*$", message = "Chức vụ chỉ được chứa chữ cái và khoảng trắng")
    private String jobTitle;
}
