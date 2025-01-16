package com.applicate.services.channelkart.timer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : Jinu
 * Date    : 12/2/2020
 **/
public class TimeLogger {

    private final Map<String, List<Watch>> watches;

    public TimeLogger() {
        watches = new LinkedHashMap<>();
    }

    public Map<String, List<Watch>> getWatches() {
        return this.watches;
    }

    public void addWatch(Watch watch) {
        String taskName = watch.getTaskName();
        List<Watch> taskWatches = this.watches.computeIfAbsent(taskName, key -> new ArrayList<>());
        taskWatches.add(watch);
    }

    public List<Watch> getActiveWatches(String taskName) {
        return this.watches.getOrDefault(taskName, Collections.emptyList()).stream().filter(Watch::isActive).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "TimeLogger{" + "watches=" + watches + '}';
    }
}
