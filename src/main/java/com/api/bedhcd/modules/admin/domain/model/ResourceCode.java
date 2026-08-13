package com.api.bedhcd.modules.admin.domain.model;

import java.util.List;

public enum ResourceCode {
    DASHBOARD(List.of(ActionCode.VIEW)),
    MANAGE_SHAREHOLDER(List.of(ActionCode.VIEW, ActionCode.CREATE, ActionCode.UPDATE, ActionCode.DELETE)),
    MANAGE_ADMIN(List.of(ActionCode.VIEW, ActionCode.CREATE, ActionCode.UPDATE)),
    CONFIG_STATUS(List.of(ActionCode.VIEW, ActionCode.CREATE, ActionCode.UPDATE, ActionCode.DELETE)),
    MANAGE_MEETING(List.of(ActionCode.VIEW, ActionCode.CREATE, ActionCode.UPDATE, ActionCode.DELETE, ActionCode.APPROVE)),
    CHECK_ELIGIBILITY(List.of(ActionCode.VIEW, ActionCode.CHECK)),
    ATTENDANCE_LIST(List.of(ActionCode.VIEW, ActionCode.APPROVE)),
    MANAGE_PROXY(List.of(ActionCode.VIEW, ActionCode.CREATE, ActionCode.UPDATE, ActionCode.DELETE, ActionCode.APPROVE)),
    REPORT(List.of(ActionCode.VIEW, ActionCode.EXPORT)),
    MANAGE_ROLE_GROUP(List.of(ActionCode.VIEW, ActionCode.CREATE, ActionCode.UPDATE, ActionCode.DELETE)),
    RECONCILE(List.of(ActionCode.VIEW, ActionCode.EXPORT));

    private final List<ActionCode> allowedActions;

    ResourceCode(List<ActionCode> allowedActions) {
        this.allowedActions = allowedActions;
    }

    public List<ActionCode> getAllowedActions() {
        return allowedActions;
    }
}
