package com.api.bedhcd.shared.dto.importing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProxyImportRecord {
    private String delegatorCccd;
    private String proxyCccd;
    private Long sharesDelegated;
    private String authorizationDocument;
    private LocalDate authorizationDate;
    private String description;
    
    // Thông tin người được ủy quyền (nếu chưa có trong hệ thống)
    private String fullName;
    private String email;
    private String dateOfIssue;
    private String address;
    private String placeOfIssue;
}
