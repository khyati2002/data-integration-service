package com.salescode.channelkart.security;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextUtils implements EnvironmentAware {

    private static final Logger log = LoggerFactory.getLogger(SecurityContextUtils.class);
    private static Environment environment;

    private SecurityContextUtils() {
//        throw new IllegalStateException("Utility Class");
    }

    private static void setEnv(Environment env) {
        environment = env;
    }

    public static String getLob() {
//        String tempLOB = getTempLOB();
//        if (StringUtils.isNotEmpty(tempLOB)) {
//            return tempLOB;
//        }
//        JwtUser user = getJWTUser();
//        String lob = (user == null) ? tempLOB : user.getLob();
//        if (lob == null) {
//            lob = getLobFromRequest();
//        }
//        if (lob == null) {
//            lob = AbstractDataSourceConstants.DEFAULT;
//        }
//        return lob;
        return environment.getProperty("channelkart.lobs");
    }

    public static String getPrincipal() {
        return "integration_user";
    }

    public static <T> T switchWithLOB(String lob, Function<T> function) {
        return null;
    }
    @Override
    public void setEnvironment(Environment environment) {
        setEnv(environment);
    }
}
