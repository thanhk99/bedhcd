package com.api.bedhcd.modules.shareholder.api.v1.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateShareholderRequest {
    
    @NotBlank(message = "Meeting ID không được để trống")
    private String meetingId;

    @NotBlank(message = "CCCD không được để trống")
    @Pattern(regexp = "^\\d{9}|\\d{12}$", message = "CCCD/CMND phải là 9 hoặc 12 số")
    private String cccd;

    @NotBlank(message = "Họ và tên không được để trống")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "Họ và tên chỉ được chứa chữ cái và khoảng trắng")
    private String fullName;
    
    @NotBlank(message = "Email không được để trống")
    @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "Email phải đúng định dạng")
    private String email;
    
    @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại phải bao gồm đúng 10 chữ số")
    private String phoneNumber;
    
    @Pattern(regexp = "^[\\p{L}0-9\\s,./-]*$", message = "Địa chỉ không được chứa ký tự đặc biệt hoặc icon")
    private String address;
    
    private Long sharesOwned;
}
