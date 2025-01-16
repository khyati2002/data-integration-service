package com.applicate.services.channelkart.logging;

import com.applicate.services.channelkart.models.UserContext;
import com.applicate.services.channelkart.security.SecurityContextUtils;
import com.applicate.services.channelkart.utils.StringUtils;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.function.Supplier;

public class LoggerContext {

    public static final String USER_NAME = "userName";

    public static final String LOB = "lob";

    private LoggerContext() {
        throw new UnsupportedOperationException();
    }

    public static void setUserName(String userName) {
        setUserContext(userName, MDC.get(LOB));
    }

    public static void setLob(String lob) {
        setUserContext(MDC.get(USER_NAME), lob);
    }

    public static void setUserContext(String userName, String lob) {
        UserContext context = new UserContext(userName, lob);
        setUserContext(context);
    }

    public static void  setUserContext(String userName, String lob, String streamId) {
        UserContext context = new UserContext(userName, lob, null, streamId);
        setUserContext(context);
    }

    public static void setUserContext(UserContext context) {
        MDC.put(USER_NAME, context.getUserName());
        MDC.put(LOB, context.getLob());
        MDC.put("requestId", context.getStreamId());
        MDC.put("userContext", context.toString());
    }


    public static void clear() {
        MDC.clear();
    }

    public static Optional<UserContext> getCurrentContext() {
        String userName = MDC.get(USER_NAME);
        String lob = MDC.get(LOB);
        String requestId = MDC.get("requestId");
        if (StringUtils.isNotBlank(userName) || StringUtils.isNotBlank(lob) || StringUtils.isNotBlank(requestId)) {
            return Optional.of(new UserContext(userName, lob, null, requestId));
        }
        return Optional.empty();
    }

    public static void setUserContext() {
        setUserContext(SecurityContextUtils.getUserContext().orElse(new UserContext(SecurityContextUtils.getPrincipal(),SecurityContextUtils.getLob())));
    }


    public static <T> T withLoggerContext(Supplier<T> supplier) {
        setUserContext();
        try {
            return supplier.get();
        } finally {
            clear();
        }
    }
}
