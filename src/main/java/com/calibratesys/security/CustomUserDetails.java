package com.calibratesys.security;

import com.calibratesys.entity.Staff;
import com.calibratesys.entity.enums.StaffStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security UserDetails adapter wrapping the Staff entity.
 */
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String staffId;
    private final String email;
    private final String password;
    private final String fullName;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Staff staff) {
        this.id = staff.getId();
        this.staffId = staff.getStaffId();
        this.email = staff.getEmail();
        this.password = staff.getPasswordHash();
        this.fullName = staff.getFullName();
        this.active = staff.getStatus() == StaffStatus.ACTIVE;
        this.authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + staff.getSystemRole().name())
        );
    }

    public Long getId() {
        return id;
    }

    public String getStaffId() {
        return staffId;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
