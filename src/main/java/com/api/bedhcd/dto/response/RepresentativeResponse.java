package com.api.bedhcd.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepresentativeResponse {
    private String id;
    private String fullName;
    private String cccd;
    private String generatedPassword;
    private String meetingId;
    private Long sharesDelegated;
}
