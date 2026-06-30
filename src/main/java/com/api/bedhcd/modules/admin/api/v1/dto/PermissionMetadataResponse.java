package com.api.bedhcd.modules.admin.api.v1.dto;

import com.api.bedhcd.modules.admin.domain.model.ActionCode;
import com.api.bedhcd.modules.admin.domain.model.ResourceCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionMetadataResponse {
    private ResourceCode resource;
    private List<ActionCode> allowedActions;
}
