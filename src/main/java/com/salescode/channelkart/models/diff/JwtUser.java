package com.salescode.channelkart.models.diff;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class JwtUser implements UserDetails {

    private static final long serialVersionUID = 7776594848935133740L;

    private final String id;
    private final String username;
    private String outletCode;
    private final String password;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;
    private final Date lastPasswordResetDate;
    private String lob;
    private final String hierarchy;
    private String timeZone;
    private boolean admin;
    private boolean verified;
    private boolean anonymous;
    private String tokenGroup;
    private String dialCode;
    private String deviceId;
    private String mobile;
    private String resource;
    private String requestType;

    private String secureToken;

    public boolean isAdmin() {
        return admin;
    }

    private String shortToken;

    @SuppressWarnings("java:S107")
    public JwtUser(
            String id,
            String username,
            String email,
            String password, Collection<? extends GrantedAuthority> authorities,
            boolean enabled,
            Date lastPasswordResetDate,
            String lob,
            String hierarchy,
            boolean admin,
            boolean verified,
            boolean anonymous,
            String tokenGroup,
            String dialCode,
            String mobile,
            String resource,
            String requestType,
            String secureToken
    ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.enabled = enabled;
        this.lastPasswordResetDate = lastPasswordResetDate;
        this.lob=lob;
        this.hierarchy = hierarchy;
        this.admin=admin;
        this.verified=verified;
        this.anonymous = anonymous;
        this.tokenGroup = tokenGroup;
        this.dialCode = dialCode;
        this.mobile = mobile;
        this.resource = resource;
        this.requestType = requestType;
        this.secureToken = secureToken;
    }


    @JsonIgnore
    public String getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return isEnabled();
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @JsonIgnore
    public Date getLastPasswordResetDate() {
        return lastPasswordResetDate;
    }
}
