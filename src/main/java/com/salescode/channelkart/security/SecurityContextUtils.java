package com.salescode.channelkart.security;


import com.salescode.channelkart.abstractdatasource.AbstractDataSourceConstants;
import com.salescode.channelkart.utils.StringUtils;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;


public class SecurityContextUtils implements EnvironmentAware {

    //private static final ThreadLocal<UserContext> userContextHolder = new ThreadLocal<>();

   public static Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        setEnv(environment);
    }

    private SecurityContextUtils() {
        throw new IllegalStateException("Utility Class");
    }

//    public static String getPrincipal() {
//        JwtUser user = getJWTUser();
//        return user != null ? user.getUsername() : getTempUserName();
//    }

    public static String getLob() {
        String tempLOB = getTempLOB();
        if (StringUtils.isNotEmpty(tempLOB)) {
            return tempLOB;
        }
//        JwtUser user = getJWTUser();
//        String lob = (user == null) ? tempLOB : user.getLob();
//        if (lob == null) {
//            lob = getLobFromRequest();
//        }
//        if (lob == null) {
//            lob = AbstractDataSourceConstants.DEFAULT;
//        }
     //   return lob;
        return AbstractDataSourceConstants.DEFAULT;
    }
    private static void setEnv(Environment env) {
        environment = env;
    }

    public static String getTempLOB() {
       return "cktestitcloyalty";
    }

    public static String getPrincipal() {
        return "integration_user";
    }

//    public static Optional<UserContext> getUserContext() {
//        return Optional.ofNullable(userContextHolder.get());
//    }

    public static boolean isDebugEnabled() {
        return false;
    }

    public static <T> T switchWithLOB(String lob, Function<T> function) {
        return function.invoke();
    }


}

