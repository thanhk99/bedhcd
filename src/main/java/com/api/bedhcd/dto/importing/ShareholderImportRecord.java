package com.api.bedhcd.dto.importing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareholderImportRecord {
    private String cccd;
    private String fullName;
    private String investorCode;
    private Long shares;
    private String email;
    private String phoneNumber;
    private String address;
    private String dateOfIssue;
    private String placeOfIssue;
    private String nation;
    private String password;
}
