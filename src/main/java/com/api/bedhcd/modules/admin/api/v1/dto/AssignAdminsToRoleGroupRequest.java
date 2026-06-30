package com.api.bedhcd.modules.admin.api.v1.dto;

import lombok.Data;

import java.util.List;

@Data
public class AssignAdminsToRoleGroupRequest {
    private List<String> adminIds;
}
