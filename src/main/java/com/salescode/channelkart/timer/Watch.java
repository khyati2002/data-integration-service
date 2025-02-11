package com.salescode.channelkart.timer;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author : Jinu
 * Date    : 12/3/2020
 **/
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

   public long getStartingTime() {
      return startingTime;
   }

   public long getEndingTime() {
      return endingTime;
   }

   public long getDuration() {
      return duration;
   }

   public String getTaskName() {
      return taskName;
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
