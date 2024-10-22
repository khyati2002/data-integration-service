package com.salescode.channelkart.security;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author : Jinu
 * Date    : 7/21/2020
 **/
@Getter
@Setter
public class UserContext {

    private String userName;
    private String lob;
    private String timeZone;
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

    public UserContext setUserName(String userName) {
        this.userName = userName;
        return this;
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
        String joinedContext = getAsMap().entrySet().stream().filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty()).map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.joining(","));
        return "[" + joinedContext + "]";
    }

}
