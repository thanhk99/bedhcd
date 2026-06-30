package com.api.bedhcd.modules.admin.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "admin_role_groups")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(AdminRoleGroupId.class)
public class AdminRoleGroupEntity {

    @Id
    @Column(nullable = false)
    private String adminId;

    @Id
    @Column(nullable = false)
    private String roleGroupId;
}
