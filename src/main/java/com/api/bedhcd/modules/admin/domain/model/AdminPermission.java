package com.api.bedhcd.modules.admin.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPermission {
    private ResourceCode resource;
    private Set<ActionCode> actions;
}
