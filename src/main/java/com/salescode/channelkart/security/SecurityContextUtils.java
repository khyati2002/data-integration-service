package com.salescode.channelkart.security;


import com.salescode.channelkart.logging.LoggerContext;
import com.salescode.channelkart.models.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@Component
public class SecurityContextUtils implements EnvironmentAware {

    private static final Logger log = LoggerFactory.getLogger(SecurityContextUtils.class);
    private static Environment environment;
    private static final ThreadLocal<UserContext> userContextHolder = new ThreadLocal<>();

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
    

    public static <R> R switchWithUser(UserContext userContext, Function<R> function) {
        Optional<UserContext> currentContext = getUserContext();
        Optional<UserContext> loggerContext = LoggerContext.getCurrentContext();
        try {
            userContextHolder.set(userContext);
            LoggerContext.setUserContext(userContext);
            return function.invoke();
        } finally {
            if (currentContext.isPresent()) {
                UserContext context = currentContext.get();
                userContextHolder.set(context);
            } else {
                userContextHolder.remove();
            }
            if (loggerContext.isPresent()) {
                LoggerContext.setUserContext(loggerContext.get());
            } else {
                LoggerContext.clear();
            }
        }
    }

    public static void switchWithUser(UserContext userContext, Runnable runnable) {
        switchWithUser(userContext, () -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public void setEnvironment(Environment environment) {
        setEnv(environment);
    }

    public static String getCurrentRequestId() {
        Optional<String> streamId = getUserContext().map(UserContext::getStreamId);
        if (streamId.isPresent()) {
            return streamId.get();
        }
        Optional<HttpServletRequest> optionalHttpServletRequest = getRequestSafely();
        if (optionalHttpServletRequest.isPresent()) {
            HttpServletRequest httpServletRequest = optionalHttpServletRequest.get();
            Object attribute = httpServletRequest.getAttribute("streamId");
            if (attribute != null) {
                return attribute.toString();
            }
        }
        return null;
    }

    private static Optional<HttpServletRequest> getRequestSafely() {
        try {
            return Optional.of(((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getRequest());
        } catch (Exception e) {
            log.debug("Could not find request", e);
        }
        return Optional.empty();
    }

    public static String getTimeZone() {
        return "Asia/Kolkata";
    }

    public static Optional<UserContext> getUserContext() {
        return Optional.ofNullable(userContextHolder.get());
    }
}
