//package com.salescode.channelkart.models.diff;
//
//import com.salescode.channelkart.services.SpringContext;
//import com.salescode.channelkart.security.SecurityContextUtils;
//import com.salescode.jooq.generated.tables.CkUser;
//
//import java.util.LinkedHashMap;
//import java.util.Map;
//import java.util.stream.Collectors;
//
///**
// * @author : Jinu
// * Date    : 7/21/2020
// **/
//public class UserContext {
//
//    private String userName;
//
//    private String lob;
//
//    private String timeZone;
//
//    private JwtUser jwtUser;
//
//    private boolean isDebug = false;
//
//    private String streamId;
//
//
//    private String dbPrivilege;
//
//
//    public UserContext(String userName, String lob, String timeZone) {
//        this.userName = userName;
//        if (this.userName == null) {
//            this.userName = SecurityContextUtils.getPrincipal();
//        }
//        this.lob = lob;
//        this.timeZone = timeZone;
//        this.isDebug = SecurityContextUtils.isDebugEnabled();
//        this.streamId = SecurityContextUtils.getCurrentRequestId();
//    }
//
//    public UserContext(String userName, String lob, String timeZone, String streamId) {
//        this.userName = userName;
//        if (this.userName == null) {
//            this.userName = SecurityContextUtils.getPrincipal();
//        }
//        this.lob = lob;
//        this.timeZone = timeZone;
//        this.isDebug = SecurityContextUtils.isDebugEnabled();
//        this.streamId = streamId;
//        this.streamId = SecurityContextUtils.getCurrentRequestId();
//    }
//
//    public UserContext(String userName, String lob) {
//        this(userName, lob, null);
//    }
//
//    public static UserContext fromLob(String lob) {
//        return from(SecurityContextUtils.getPrincipal(), lob);
//    }
//
//    public static UserContext loginId(String userName) {
//        return from(userName, SecurityContextUtils.getLob());
//    }
//
//    public static UserContext from(String userName, String lob) {
//        return new UserContext(userName, lob, SecurityContextUtils.getTimeZone(), SecurityContextUtils.getCurrentRequestId());
//    }
//
//    public static UserContext fromPrivilege(String userName, String lob, String privilege) {
//        UserContext userContext = new UserContext(userName, lob);
//        userContext.setDbPrivilege(privilege);
//        return userContext;
//    }
//
//    public static UserContext fromCurrentContext() {
//        String lob = SecurityContextUtils.getLob();
//        String userName = SecurityContextUtils.getPrincipal();
//        return from(userName, lob);
//    }
//
//    private Map<String, String> getAsMap() {
//        LinkedHashMap<String, String> context = new LinkedHashMap<>();
//        context.putIfAbsent("lob", lob);
//        context.putIfAbsent("userName", userName);
//        context.putIfAbsent("reqId", streamId);
//        return context;
//    }
//
//    public JwtUser getJwtUser() {
//        if (this.jwtUser == null && StringUtils.isNotEmpty(this.userName)) {
//            CkUser user = SpringContext.getBean(UserService.class).findByLoginId(this.getUserName());
//            this.jwtUser = JwtUserFactory.create(user);
//        }
//        return this.jwtUser;
//    }
//
//    @Override
//    public String toString() {
//        String joinedContext = getAsMap()
//                .entrySet()
//                .stream()
//                .filter(entry -> StringUtils.isNotEmpty(entry.getValue()))
//                .map(entry -> entry.getKey() + ":" + entry.getValue())
//                .collect(Collectors.joining(","));
//        return "[" + joinedContext + "]";
//    }
//
//    public boolean isDebug() {
//        return isDebug;
//    }
//
//    public UserContext setDebug(boolean debug) {
//        isDebug = debug;
//        return this;
//    }
//
//    public String getTimeZone() {
//        return timeZone;
//    }
//
//    public void setTimeZone(String timeZone) {
//        this.timeZone = timeZone;
//    }
//
//    public void setDbPrivilege(String dbPrivilege) {
//        this.dbPrivilege = dbPrivilege;
//    }
//
//    public String getDbPrivilege() {
//        return dbPrivilege;
//    }
//}
//
