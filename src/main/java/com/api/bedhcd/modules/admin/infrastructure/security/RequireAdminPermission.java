package com.api.bedhcd.modules.admin.infrastructure.security;

import com.api.bedhcd.modules.admin.domain.model.ActionCode;
import com.api.bedhcd.modules.admin.domain.model.ResourceCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAdminPermission {
    ResourceCode resource();
    ActionCode action();
}
