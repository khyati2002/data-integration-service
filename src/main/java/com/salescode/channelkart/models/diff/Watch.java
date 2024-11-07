package com.salescode.channelkart.models.diff;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Watch {

    private String taskName;

    private long startingTime;

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
        return  "taskName='" + taskName + '\'' +
                ", startingTime=" + startingTime +
                ", endingTime=" + endingTime +
                ", duration=" + duration ;
    }
}

