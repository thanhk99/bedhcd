package com.api.bedhcd.modules.identity.api.v1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Investor code is required")
    private String investorCode;

    @NotBlank(message = "CCCD is required")
    private String cccd;

    @NotBlank(message = "Date of issue is required")
    private String dateOfIssue;

    private String placeOfIssue;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Nation is required")
    private String nation;

    @NotNull(message = "Shares owned is required")
    private Long sharesOwned;

    @NotBlank(message = "Meeting ID is required")
    private String meetingId;
}
