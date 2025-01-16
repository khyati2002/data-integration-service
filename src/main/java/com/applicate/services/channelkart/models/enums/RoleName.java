package com.applicate.services.channelkart.models.enums;


import com.applicate.services.channelkart.models.Role;

import java.util.List;

public enum RoleName {
    ROLE_USER,
    ROLE_ADMIN,
    ROLE_SUPER_ADMIN,
	ROLE_LOGIN_ACCESS,
    ROLE_BLOCK_LOGIN,
    PROJECT_ADMIN,
    SECURITY_ADMIN;

    public static boolean isAdmin(String name) {
        return ROLE_SUPER_ADMIN.name().equals(name) || ROLE_ADMIN.name().equals(name);
    }

    public static boolean isLoginBlocked(List<Role> roles){return roles.stream().anyMatch(role->role.getName().equals(ROLE_BLOCK_LOGIN.name()));}

    public static boolean isAdmin(List<String> names) {
        return names.stream().anyMatch(RoleName::isAdmin);
    }
}