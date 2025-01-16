package com.applicate.services.channelkart.utils;

import com.applicate.services.channelkart.timer.TimerContext;
import com.applicate.services.channelkart.timer.Watch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class TimerUtils {
    public static final String DEBUG_KEY = "debug";
    private static final boolean ENABLE_TIMERS = Boolean.parseBoolean(System.getProperty("enableTimers", "false"));
    static boolean isEsLogging = isPropertyEnabled("logToESConditional", "true");
    static Logger logger = LoggerFactory.getLogger(TimerUtils.class);

    private TimerUtils() {
        throw new UnsupportedOperationException();
    }

    public static <T> T withTime(String tag, Function<Void, T> loggingFunction) {
        Watch watch = TimerContext.createWatch(tag);
        try {
            return loggingFunction.apply(null);
        } finally {
            watch.stop();
            logTime(tag, watch);
        }
    }

    private static void logTime(String tag, Watch watch) {
        if (ENABLE_TIMERS) {
            logger.debug("{} -> TimeTaken -> {} milliseconds", tag, watch.getDuration());
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

    public static boolean isPropertyEnabled(String name, String defaultValue) {
        return Boolean.parseBoolean(System.getProperty(name, Optional.ofNullable(System.getenv(name)).orElse(defaultValue)));
    }

}
