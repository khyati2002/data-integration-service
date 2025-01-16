package com.applicate.services.channelkart.timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author : Jinu
 * Date    : 12/2/2020
 **/
public class TimerContext {

    private static final Logger log = LoggerFactory.getLogger(TimerContext.class);
    private static final ThreadLocal<TimeLogger> CONTEXT = new ThreadLocal<>();

    private TimerContext() {
        throw new UnsupportedOperationException();
    }

    public static Optional<TimeLogger> getTimeLogger() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void put(TimeLogger log) {
        CONTEXT.set(log);
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static void setTimer(TimeLogger logger) {
        getTimeLogger().ifPresent(timeLogger -> log.warn("Timer logger already set : {}", timeLogger));
        CONTEXT.set(logger);
    }

    public static Watch createWatch(String taskName) {
        Watch watch = new Watch(taskName, System.currentTimeMillis());
        getTimeLogger().ifPresent(timerLogger -> timerLogger.addWatch(watch));
        return watch;
    }
}
