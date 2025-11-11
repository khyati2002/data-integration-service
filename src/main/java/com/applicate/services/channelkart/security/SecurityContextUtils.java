package com.applicate.services.channelkart.security;

import com.applicate.services.channelkart.utils.StringUtils;
import java.util.Optional;
import java.util.UUID;

/**
 * Lightweight replacement for Spring's SecurityContextHolder.
 * Works in standalone Java/jOOQ projects.
 */
public final class SecurityContextUtils {

	private static final ThreadLocal<JwtUser> jwtUserHolder = new ThreadLocal<>();
	private static final ThreadLocal<UserContext> userContextHolder = new ThreadLocal<>();
	private static final ThreadLocal<String> requestIdHolder = new ThreadLocal<>();
	private static final ThreadLocal<Boolean> debugFlagHolder = new ThreadLocal<>();
	private static final ThreadLocal<String> timeZoneHolder = new ThreadLocal<>();

	private SecurityContextUtils() {
		throw new IllegalStateException("Utility class");
	}

	// ===== Basic context management =====
	public static void setJwtUser(JwtUser user) { jwtUserHolder.set(user); }

	public static JwtUser getJWTUser() { return jwtUserHolder.get(); }

	public static void setUserContext(UserContext ctx) { userContextHolder.set(ctx); }

	public static Optional<UserContext> getUserContext() {
		return Optional.ofNullable(userContextHolder.get());
	}

	public static void clear() {
		jwtUserHolder.remove();
		userContextHolder.remove();
		requestIdHolder.remove();
		debugFlagHolder.remove();
		timeZoneHolder.remove();
	}

	// ===== Principal utilities =====
	public static String getPrincipal() {
		JwtUser user = getJWTUser();
		return user != null ? user.getUsername() : getTempUserName();
	}

	private static String getTempUserName() {
		return getUserContext().map(UserContext::getUserName).orElse("anonymous");
	}

	// ===== LOB handling =====
	public static String getLob() {
		String tempLOB = getTempLOB();
		if (StringUtils.isNotEmpty(tempLOB)) {
			return tempLOB;
		}

		JwtUser user = getJWTUser();
		String lob = (user == null) ? tempLOB : user.getLob();

		if (lob == null) {
			lob = getLobFromRequest();
		}

		return lob;
	}

	private static String getLobFromRequest() {
		// Implement request-based LOB extraction if applicable
		return null;
	}

	private static String getTempLOB() {
		return getUserContext().map(UserContext::getLob).orElse(null);
	}

	// ===== Extra context features (debug, requestId, timeZone) =====
	public static boolean isDebugEnabled() {
		return Optional.ofNullable(debugFlagHolder.get()).orElse(false);
	}

	public static void setDebugEnabled(boolean debug) {
		debugFlagHolder.set(debug);
	}

	public static String getCurrentRequestId() {
		return Optional.ofNullable(requestIdHolder.get())
				.orElseGet(() -> {
					String id = UUID.randomUUID().toString();
					requestIdHolder.set(id);
					return id;
				});
	}

	public static void setCurrentRequestId(String id) {
		requestIdHolder.set(id);
	}

	public static String getTimeZone() {
		return Optional.ofNullable(timeZoneHolder.get()).orElse("UTC");
	}

	public static void setTimeZone(String tz) {
		timeZoneHolder.set(tz);
	}
}
