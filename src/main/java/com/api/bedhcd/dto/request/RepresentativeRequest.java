package com.api.bedhcd.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepresentativeRequest {
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    private String email;
    private String phoneNumber;

    @NotBlank(message = "CCCD không được để trống")
    private String cccd;

    private String dateOfIssue;
    private String address;

    @NotBlank(message = "ID cuộc họp không được để trống")
    private String meetingId;

    @NotBlank(message = "CCCD người uỷ quyền không được để trống")
    private String delegatorCccd;

    private Long sharesDelegated;
}
