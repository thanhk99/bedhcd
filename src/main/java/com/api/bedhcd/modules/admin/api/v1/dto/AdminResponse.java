package com.api.bedhcd.modules.admin.api.v1.dto;

import com.api.bedhcd.shared.domain.enums.Role;
import lombok.Data;

import java.util.List;

@Data
public class AdminResponse {
    private String id;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Role role;
    private boolean isActive;

    private List<String> roleGroupIds;
    private List<String> roleGroupNames;
    private String department;
    private String jobTitle;
}
