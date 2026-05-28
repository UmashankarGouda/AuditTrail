package com.audittrail.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class SecurityContext {

    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
            org.springframework.security.core.userdetails.User user = 
                (org.springframework.security.core.userdetails.User) auth.getPrincipal();
            // Try to extract from auth details if available
            return null;
        }
        return null;
    }

    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getName();
        }
        return null;
    }

    public Collection<String> getCurrentRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
        }
        return java.util.Collections.emptyList();
    }

    public boolean hasRole(String role) {
        return getCurrentRoles().contains("ROLE_" + role);
    }
}
