package com.api.bedhcd.modules.admin.api.v1.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoleGroupResponse {
    private String id;
    private String name;
    private String description;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AdminPermissionDto> permissions;
}
