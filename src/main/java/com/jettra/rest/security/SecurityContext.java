package com.jettra.rest.security;

import java.security.Principal;
import java.util.Set;

public class SecurityContext {

    private final Principal userPrincipal;
    private final Set<String> roles;
    private final boolean secure;
    private final String authScheme;

    public SecurityContext(Principal userPrincipal, Set<String> roles, boolean secure, String authScheme) {
        this.userPrincipal = userPrincipal;
        this.roles = roles != null ? roles : Set.of();
        this.secure = secure;
        this.authScheme = authScheme;
    }

    public Principal getUserPrincipal() {
        return userPrincipal;
    }

    public boolean isUserInRole(String role) {
        return roles.contains(role);
    }

    public boolean isSecure() {
        return secure;
    }

    public String getAuthenticationScheme() {
        return authScheme;
    }
}
