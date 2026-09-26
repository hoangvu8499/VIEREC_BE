package com.vierec.security;

import com.vierec.modules.role.entity.Role;
import com.vierec.modules.user.entity.User;
import com.vierec.modules.user.entity.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapts the {@link User} entity to Spring Security without exposing the entity itself
 * to the security layer.
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;
    private static final String ROLE_PREFIX = "ROLE_";

    private final Long id;
    private final String username;
    private final String email;
    private final String password;
    private final UserStatus status;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.status = user.getStatus();
        this.authorities = toAuthorities(user.getRoles());
    }

    /**
     * {@code ROLE_<role code>} for every role, plus the raw code of every permission granted to those roles
     * through {@code role_permissions}. Must run inside a transaction: {@code Role.permissions} is lazy.
     */
    private static List<GrantedAuthority> toAuthorities(Set<Role> roles) {
        Set<String> names = new LinkedHashSet<>();
        for (Role role : roles) {
            names.add(ROLE_PREFIX + role.getCode());
            role.getPermissions().forEach(permission -> names.add(permission.getCode()));
        }
        return names.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserStatus.LOCKED != status;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return UserStatus.ACTIVE == status;
    }
}
