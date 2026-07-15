package com.api.bedhcd.modules.shareholder.api.v1.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateShareholderRequest {
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "Họ và tên chỉ được chứa chữ cái và khoảng trắng (không có số và ký tự đặc biệt)")
    private String fullName;
    
    @NotBlank(message = "Email không được để trống")
    @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "Email phải đúng định dạng, bao gồm @ và dấu .")
    private String email;
    
    @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại phải bao gồm đúng 10 chữ số")
    private String phoneNumber;
    @Pattern(regexp = "^[\\p{L}0-9\\s,./-]*$", message = "Địa chỉ không được chứa ký tự đặc biệt hoặc icon")
    private String address;
    private Boolean enabled;
    private Long sharesOwned;
}
