package com.applicate.services.channelkart.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collection;
import java.util.Date;

public class JwtUser {

	private static final long serialVersionUID = 7776594848935133740L;

	private final String id;
	private final String username;
	private String outletCode;
	private final String password;
	private final String email;
	private final Collection<String> authorities;
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

	public void setAdmin(boolean admin) {
		this.admin = admin;
	}

	public String getShortToken() {
		return shortToken;
	}

	public void setShortToken(String shortToken) {
		this.shortToken = shortToken;
	}

	private String shortToken;

	@SuppressWarnings("java:S107")
	public JwtUser(
			String id,
			String username,
			String email,
			String password, Collection<String> authorities,
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

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getLob() {
		return lob;
	}

	@JsonIgnore
	public String getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	@JsonIgnore
	public boolean isAccountNonExpired() {
		return true;
	}

	@JsonIgnore
	public boolean isAccountNonLocked() {
		return isEnabled();
	}

	@JsonIgnore
	public boolean isCredentialsNonExpired() {
		return true;
	}

	public String getEmail() {
		return email;
	}

	@JsonIgnore
	public String getPassword() {
		return password;
	}

	public Collection<String> getAuthorities() {
		return authorities;
	}


	public boolean isEnabled() {
		return enabled;
	}

	@JsonIgnore
	public Date getLastPasswordResetDate() {
		return lastPasswordResetDate;
	}

	public String getOutletCode() {
		return outletCode;
	}

	public String setOutletCode(String outletCode) {
		this.outletCode = outletCode;
		return this.outletCode;
	}

	public void setLob(String lob){
		this.lob = lob;
	}

	/**
	 * @return the hierarchy
	 */
	public String getHierarchy() {
		return hierarchy;
	}

	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	public boolean isVerified() {
		return verified;
	}

	public void setVerified(boolean verified) {
		this.verified = verified;
	}

	public boolean isAnonymous() {
		return anonymous;
	}

	public void setAnonymous(boolean anonymous) {
		this.anonymous = anonymous;
	}

	public String getTokenGroup() {
		return tokenGroup;
	}

	public void setTokenGroup(String tokenGroup) {
		this.tokenGroup = tokenGroup;
	}

	public String getDialCode() {
		return dialCode;
	}

	public void setDialCode(String dialCode) {
		this.dialCode = dialCode;
	}

	public String getSecureToken() {
		return secureToken;
	}

	public JwtUser setSecureToken(String secureToken) {
		this.secureToken = secureToken;
		return this;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getRequestType() {
		return requestType;
	}

	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}
}
