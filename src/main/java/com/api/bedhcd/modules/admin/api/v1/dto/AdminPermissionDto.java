package com.api.bedhcd.modules.admin.api.v1.dto;

import com.api.bedhcd.modules.admin.domain.model.ActionCode;
import com.api.bedhcd.modules.admin.domain.model.ResourceCode;
import lombok.Data;

import java.util.Set;

@Data
public class AdminPermissionDto {
    private ResourceCode resource;
    private Set<ActionCode> actions;
}
