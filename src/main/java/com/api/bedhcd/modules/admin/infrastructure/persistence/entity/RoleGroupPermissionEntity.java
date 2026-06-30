package com.api.bedhcd.modules.admin.infrastructure.persistence.entity;

import com.api.bedhcd.modules.admin.domain.model.ActionCode;
import com.api.bedhcd.modules.admin.domain.model.ResourceCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Table(name = "role_group_permissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleGroupPermissionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String roleGroupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceCode resource;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_group_permission_actions", joinColumns = @JoinColumn(name = "permission_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "action")
    private Set<ActionCode> actions;
}
