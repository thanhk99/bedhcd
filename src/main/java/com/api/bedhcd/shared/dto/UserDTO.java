package com.api.bedhcd.shared.dto;

import com.api.bedhcd.shared.domain.enums.Role;
import com.api.bedhcd.shared.domain.enums.ShareholderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String id;
    private String username;
    private String fullName;
    private String email;
    private String cccd;
    private String investorCode;
    private String phoneNumber;
    private Long sharesOwned;
    private Set<Role> roles;
    private boolean enabled;
    private boolean splitAccount;
    private ShareholderStatus shareholderStatus;
}
