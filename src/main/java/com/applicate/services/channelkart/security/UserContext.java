package com.applicate.services.channelkart.security;

import com.applicate.services.channelkart.utils.StringUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class UserContext {

	private String userName;
	private String lob;
	private String timeZone;
	private boolean isDebug;
	private String streamId;
	private String dbPrivilege;

	public UserContext(String userName, String lob, String timeZone) {
		this.userName = (userName != null) ? userName : SecurityContextUtils.getPrincipal();
		this.lob = lob;
		this.timeZone = timeZone != null ? timeZone : SecurityContextUtils.getTimeZone();
		this.isDebug = SecurityContextUtils.isDebugEnabled();
		this.streamId = SecurityContextUtils.getCurrentRequestId();
	}

	public UserContext(String userName, String lob, String timeZone, String streamId) {
		this.userName = (userName != null) ? userName : SecurityContextUtils.getPrincipal();
		this.lob = lob;
		this.timeZone = timeZone != null ? timeZone : SecurityContextUtils.getTimeZone();
		this.isDebug = SecurityContextUtils.isDebugEnabled();
		this.streamId = (streamId != null) ? streamId : SecurityContextUtils.getCurrentRequestId();
	}

	public UserContext(String userName, String lob) {
		this(userName, lob, null);
	}

	public static UserContext fromLob(String lob) {
		return from(SecurityContextUtils.getPrincipal(), lob);
	}

	public static UserContext loginId(String userName) {
		return from(userName, SecurityContextUtils.getLob());
	}

	public static UserContext from(String userName, String lob) {
		return new UserContext(userName, lob, SecurityContextUtils.getTimeZone(), SecurityContextUtils.getCurrentRequestId());
	}

	public static UserContext fromPrivilege(String userName, String lob, String privilege) {
		UserContext context = new UserContext(userName, lob);
		context.setDbPrivilege(privilege);
		return context;
	}

	public String getUserName() { return userName; }

	public String getStreamId() { return streamId; }

	public void setStreamId(String streamId) { this.streamId = streamId; }

	public String getLob() { return lob; }

	public UserContext setLob(String lob) {
		this.lob = lob;
		return this;
	}

	public boolean isDebug() { return isDebug; }

	public UserContext setDebug(boolean debug) {
		this.isDebug = debug;
		return this;
	}

	public String getTimeZone() { return timeZone; }

	public void setTimeZone(String timeZone) { this.timeZone = timeZone; }

	public void setDbPrivilege(String dbPrivilege) { this.dbPrivilege = dbPrivilege; }

	public String getDbPrivilege() { return dbPrivilege; }

	@Override
	public String toString() {
		String joinedContext = getAsMap()
				.entrySet()
				.stream()
				.filter(entry -> StringUtils.isNotEmpty(entry.getValue()))
				.map(entry -> entry.getKey() + ":" + entry.getValue())
				.collect(Collectors.joining(","));
		return "[" + joinedContext + "]";
	}

	private Map<String, String> getAsMap() {
		LinkedHashMap<String, String> context = new LinkedHashMap<>();
		context.putIfAbsent("lob", lob);
		context.putIfAbsent("userName", userName);
		context.putIfAbsent("reqId", streamId);
		return context;
	}
}
