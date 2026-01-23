package com.api.bedhcd.dto.importing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxyImportRecord {
    private String delegatorCccd;
    private String proxyCccd;
    private Long sharesDelegated;
    private String authorizationDocument;
    private java.time.LocalDate authorizationDate;
    private String description;
    private String dateOfIssue;
    private String fullName;
    private String email;
}
