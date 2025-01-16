package com.applicate.services.channelkart.timer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

/**
 * @author : Jinu
 * Date    : 12/3/2020
 **/
@Getter
public class Watch {

    private final String taskName;

    private final long startingTime;

    private long endingTime;

    private long duration;

    public Watch(String taskName, long startingTime) {
        this.taskName = taskName;
        this.startingTime = startingTime;
    }

    public void stop() {
        this.endingTime = System.currentTimeMillis();
        this.duration = endingTime - startingTime;
    }

    @JsonIgnore
    public boolean isStopped() {
        return !isActive();
    }

    @JsonIgnore
    public boolean isActive() {
        return duration == 0L;
    }

    @Override
    public String toString() {
        return "taskName='" + taskName + '\'' + ", startingTime=" + startingTime + ", endingTime=" + endingTime + ", duration=" + duration;
    }
}
