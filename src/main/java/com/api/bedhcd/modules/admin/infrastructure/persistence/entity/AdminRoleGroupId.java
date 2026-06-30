package com.api.bedhcd.modules.admin.infrastructure.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRoleGroupId implements Serializable {
    private String adminId;
    private String roleGroupId;
}
