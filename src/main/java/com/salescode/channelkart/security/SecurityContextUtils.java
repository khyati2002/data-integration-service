package com.salescode.channelkart.security;

import com.salescode.channelkart.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class SecurityContextUtils {

    private static final Logger log = LoggerFactory.getLogger(SecurityContextUtils.class);

    private static final ThreadLocal<UserContext> userContextHolder = new ThreadLocal<>();

    private SecurityContextUtils() {
        throw new IllegalStateException("Utility Class");
    }

    public static String getPrincipal() {
        return getTempUserName();
    }

    public static String getTimeZone() {
        return getTempTimeZone();
    }

    public static Optional<UserContext> getUserContext() {
        return Optional.ofNullable(userContextHolder.get());
    }

    private static String getTempUserName() {
        return getUserContext().map(UserContext::getUserName).orElse(null);
    }

    private static String getTempTimeZone() {
        return getUserContext().map(UserContext::getTimeZone).orElse(null);
    }

    public static String getLob() {
        String lob = getTempLOB();
        if (StringUtils.isNotEmpty(lob)) {
            return lob;
        }
        if (lob == null) {
            lob = "default";
        }
        return lob;
    }

    public static String getDBPrivilege() {
        return getTempDBPrivilege();
    }

    private static String getLobFromRequest() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String lob = null;
        if ("root".equalsIgnoreCase(lob)) {
            lob = "default";
        }
        return lob;
    }

    public static void setTempContext(String loginId, String lob) {
        UserContext userContext = new UserContext(loginId, lob);
    }

    public static void clearContext() {
        userContextHolder.remove();
    }

    public static String getTempLOB() {
        return getUserContext().map(UserContext::getLob).orElse(null);
    }

    public static void setTempLOB(String lob) {
        UserContext context = getUserContext().orElseGet(() -> new UserContext(null, lob)).setLob(lob);
        userContextHolder.set(context);
    }

    private static String getTempDBPrivilege() {
        return getUserContext().map(UserContext::getDbPrivilege).orElse(null);
    }

    public static <T> T switchWithLOB(String lob, Function<T> function) {
        return switchWithLOB(lob, function, false);
    }

    public static <T> T switchWithDBPrivilege(String privilege, Function<T> function) {
        return switchWithDBPrivilege(privilege, function, false);
    }


    public static <R> R switchWithUser(UserContext userContext, Function<R> function) {
        Optional<UserContext> currentContext = getUserContext();
        try {
            userContextHolder.set(userContext);
            return function.invoke();
        } finally {
            if (currentContext.isPresent()) {
                UserContext context = currentContext.get();
                userContextHolder.set(context);
            } else {
                userContextHolder.remove();
            }
        }
    }

    public static void switchWithUser(UserContext userContext, Runnable runnable) {
        switchWithUser(userContext, () -> {
            runnable.run();
            return null;
        });
    }

    public static <T> T switchWithUser(UserContext context, Function<T> function, boolean runInNewThread) {
        if (runInNewThread) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                return CompletableFuture.supplyAsync(() -> {
                    ClassLoader vmLoader = Thread.currentThread().getContextClassLoader();
                    try {
                        Thread.currentThread().setContextClassLoader(cl);
                        return switchWithUser(context, function);
                    } finally {
                        Thread.currentThread().setContextClassLoader(vmLoader);
                    }
                }).join();
            } catch (CompletionException e) {
                throw (RuntimeException) e.getCause();
            }
        } else {
            return switchWithUser(context, function);
        }
    }

    public static <T> T switchWithLOB(String lob, Function<T> function, boolean runInNewThread) {
        UserContext context = UserContext.from(SecurityContextUtils.getPrincipal(), lob);
        context.setStreamId(SecurityContextUtils.getCurrentRequestId());
        return switchWithUser(context, function, runInNewThread);
    }

    public static <T> T switchWithDBPrivilege(String privilege, Function<T> function, boolean runInNewThread) {
        UserContext context = UserContext.fromPrivilege(SecurityContextUtils.getPrincipal(), SecurityContextUtils.getLob(), privilege);
        context.setStreamId(SecurityContextUtils.getCurrentRequestId());
        return switchWithUser(context, function, runInNewThread);
    }

    public static boolean isRootLob() {
        String lob = getLob();
        return isRootLob(lob);
    }

    public static boolean isRootLob(String lob) {
        return "root".equalsIgnoreCase(lob) || "default".equals(lob);
    }

    public static <T> T switchWithRoot(Function<T> function, boolean runInNewThread) {
        return switchWithLOB("default", function, runInNewThread);
    }

    public static void ensureNotInARootContext() {
        if (isRootLob()) {
            throw new IllegalStateException("Cannot run as root user");
        }
    }

    public static String getCurrentRequestId() {
        Optional<String> streamId = getUserContext().map(UserContext::getStreamId);
        return streamId.orElse(null);
    }

    /**
     * Run in new thread async.
     *
     * @param <T>      the generic type
     * @param function the function
     */
    public static <T> void runInNewThreadAsync(Function<T> function) {
        final UserContext context = UserContext.from(SecurityContextUtils.getPrincipal(), SecurityContextUtils.getLob());
        context.setStreamId(getCurrentRequestId());
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            CompletableFuture.supplyAsync(() -> {
                ClassLoader vmLoader = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(cl);
                    return switchWithUser(context, function);
                } finally {
                    Thread.currentThread().setContextClassLoader(vmLoader);
                }
            });
        } catch (CompletionException e) {
            throw (RuntimeException) e.getCause();
        }
    }

}
