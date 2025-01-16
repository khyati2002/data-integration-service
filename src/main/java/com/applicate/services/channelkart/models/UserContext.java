package com.applicate.services.channelkart.models;

import com.applicate.services.channelkart.security.SecurityContextUtils;
import com.applicate.services.channelkart.utils.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class UserContext {

    private String userName;

    private String lob;

    private String timeZone;

    private boolean isDebug = false;

    private String streamId;


    private String dbPrivilege;

    public UserContext(String userName, String lob, String timeZone) {
        this.userName = userName;
        if (this.userName == null) {
            this.userName = SecurityContextUtils.getPrincipal();
        }
        this.lob = lob;
        this.timeZone = timeZone;
        this.streamId = SecurityContextUtils.getCurrentRequestId();
    }

    public UserContext(String userName, String lob, String timeZone, String streamId) {
        this.userName = userName;
        if (this.userName == null) {
            this.userName = SecurityContextUtils.getPrincipal();
        }
        this.lob = lob;
        this.timeZone = timeZone;
        this.streamId = streamId;
        this.streamId = SecurityContextUtils.getCurrentRequestId();
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
        UserContext userContext = new UserContext(userName, lob);
        userContext.setDbPrivilege(privilege);
        return userContext;
    }

    public static UserContext fromCurrentContext() {
        String lob = SecurityContextUtils.getLob();
        String userName = SecurityContextUtils.getPrincipal();
        return from(userName, lob);
    }

    public String getUserName() {
        return userName;
    }

    public UserContext setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public String getLob() {
        return lob;
    }

    public UserContext setLob(String lob) {
        this.lob = lob;
        return this;
    }

    private Map<String, String> getAsMap() {
        LinkedHashMap<String, String> context = new LinkedHashMap<>();
        context.putIfAbsent("lob", lob);
        context.putIfAbsent("userName", userName);
        context.putIfAbsent("reqId", streamId);
        return context;
    }

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

    public boolean isDebug() {
        return isDebug;
    }

    public UserContext setDebug(boolean debug) {
        isDebug = debug;
        return this;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public void setDbPrivilege(String dbPrivilege) {
        this.dbPrivilege = dbPrivilege;
    }

    public String getDbPrivilege() {
        return dbPrivilege;
    }
}
