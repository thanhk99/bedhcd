package com.api.bedhcd.modules.admin.api.v1.dto;

import lombok.Data;
import java.util.List;

@Data
public class RoleGroupRequest {
    private String name;
    private String description;
    private boolean isActive = true;
    private List<AdminPermissionDto> permissions;
}
