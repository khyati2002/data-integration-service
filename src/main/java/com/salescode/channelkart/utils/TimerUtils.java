package com.salescode.channelkart.utils;

import com.salescode.channelkart.models.diff.Watch;
import com.salescode.channelkart.services.SpringContext;
import com.salescode.channelkart.models.diff.RequestMetrics;
import com.salescode.dataintegration.logging.RemoteMetricStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.ssl.SslProperties;
import java.util.Calendar;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class TimerUtils {
    static boolean isEsLogging = isPropertyEnabled("logToESConditional","true");

    public static final String DEBUG_KEY = "debug";

    static Logger logger = LoggerFactory.getLogger(TimerUtils.class);

    private static final boolean ENABLE_TIMERS = Boolean.parseBoolean(System.getProperty("enableTimers","false"));

    static RemoteMetricStream metricStream = SpringContext.getBeanSafely(RemoteMetricStream.class).orElse(null);

    private TimerUtils() {
        throw new UnsupportedOperationException();
    }

    private static void logToESConditional(String tag, long time) {

        if (time < 1000) {
            return;
        }
        if(isEsLogging) {
            RequestMetrics rm = new RequestMetrics();
            rm.setClientIp("system");
            rm.setDuration((time));
            rm.setUri(tag);
            rm.setUser(SecurityContextUtils.getPrincipal());
            rm.setLob(SecurityContextUtils.getLob());
            rm.setEventTime(Calendar.getInstance().getTime());
            rm.setMethod("");
            rm.setStatusCode(0);
            rm.setStreamId("");
            metricStream.sendAsync(rm);
        }

    }

    public static <T> T withTime(String tag, Function<Void, T> loggingFunction) {
        SslProperties.Bundles.Watch watch = TimerContext.createWatch(tag);
        try {
            return loggingFunction.apply(null);
        } finally {
            watch.stop();
            logTime(tag, watch);
        }
    }

    private static void logTime(String tag, SslProperties.Bundles.Watch watch) {

        if (SecurityContextUtils.isDebugEnabled()) {
            logger.info("{} -> TimeTaken -> {} milliseconds", tag, watch.getDuration());
        } else if(ENABLE_TIMERS){
            logger.debug("{} -> TimeTaken -> {} milliseconds", tag, watch.getDuration());
        }
        if(ENABLE_TIMERS || SecurityContextUtils.isDebugEnabled()){
            logToESConditional(tag, watch.getDuration());
        }
    }


    public static void withTime(String tag, Action loggingFunction) {
        Watch watch = TimerContext.createWatch(tag);
        try {
            loggingFunction.action();
        } finally {
            watch.stop();
            logTime(tag, watch);
        }
    }

    public static <T> T withTime(String tag, Supplier<T> supplier) {
        Watch watch = TimerContext.createWatch(tag);
        try {
            return supplier.get();
        } finally {
            watch.stop();
            logTime(tag, watch);
        }
    }

    public static <T, K> T withTime(String tag, K k, Function<K, T> loggingFunction) {
        Watch watch = TimerContext.createWatch(tag);
        try {
            return loggingFunction.apply(k);
        } finally {
            watch.stop();
            logTime(tag, watch);
        }
    }

    public static <T> T withTime(Map<String, ?> queryParams, String tag, Supplier<T> supplier) {
        if (!isDebugRequest(queryParams)) {
            return supplier.get();
        }
        return withTime(tag, supplier);
    }

    public static boolean isDebugRequest(Map<String, ?> queryParams) {
        if (queryParams == null) {
            return false;
        }
        Object o = queryParams.get(DEBUG_KEY);
        String value = o != null ? o.toString() : "false";
        return "true".equalsIgnoreCase(value);
    }

    public static boolean isDebugEnabled(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getParameter(DEBUG_KEY));
    }

    public static boolean isPropertyEnabled(String name,String defaultValue){
        return Boolean.parseBoolean(System.getProperty(name, Optional.ofNullable(System.getenv(name)).orElse(defaultValue)));
    }

}

