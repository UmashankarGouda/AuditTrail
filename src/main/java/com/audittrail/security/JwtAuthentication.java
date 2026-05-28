package com.audittrail.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collection;
import java.util.List;

public class JwtAuthentication extends AbstractAuthenticationToken {
    private final Long userId;
    private final String username;
    private final String role;

    public JwtAuthentication(Long userId, String username, String role) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        this.userId = userId;
        this.username = username;
        this.role = role;
        setAuthenticated(true);
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    public String getRole() {
        return role;
    }
}
