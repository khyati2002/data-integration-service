package com.salescode.dis.insights.dto;

import com.salescode.dis.insights.enums.JobStatus;
import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Data
public class LobSummaryDTO {
    private String lob;
    private Integer masterCount;
    private JobStatus status;
    private Integer successJobCount = 0;
    private Integer inProgressJobCount = 0;
    private Integer failedJobCount = 0;
    private Integer publishedAverageThroughput;
    private Integer consumedAverageThroughput;
    private List<JobEntityResponseDtoWithFiles> jobs;

    public LobSummaryDTO(String lob, List<JobEntityResponseDtoWithFiles> jobs) {
        this.lob = lob;
        this.jobs = jobs;
        Set<Object> uniqueMasters = new HashSet<>();
        int totalPublishedThroughput = 0;
        int publishedCount = 0;

        int totalConsumedThroughput = 0;
        int consumedCount = 0;
        for (JobEntityResponseDtoWithFiles job : jobs){
            if (job.getMaster() != null) {
                uniqueMasters.add(job.getMaster());
            }

            if (Objects.equals(job.getStatus(), JobStatus.COMPLETED)){
                this.successJobCount++;
            } else if (Objects.equals(job.getStatus(), JobStatus.PENDING)){
                this.inProgressJobCount++;
            } else {
                this.failedJobCount++;
            }

            // Published throughput average
            Integer published = job.getPublishedAverageThroughput();
            if (published != null) {
                totalPublishedThroughput += published;
                publishedCount++;
            }

            // Consumed throughput average
            Integer consumed = job.getConsumedAverageThroughput();
            if (consumed != null) {
                totalConsumedThroughput += consumed;
                consumedCount++;
            }
        }

        this.publishedAverageThroughput = publishedCount == 0 ? 0 : totalPublishedThroughput / publishedCount;
        this.consumedAverageThroughput = consumedCount == 0 ? 0 : totalConsumedThroughput / consumedCount;
        this.masterCount = uniqueMasters.size();
        if (jobs.size() == successJobCount) {
            this.status = JobStatus.COMPLETED;
        } else if (jobs.size() == failedJobCount) {
            this.status = JobStatus.FAILED;
        } else {
            this.status = JobStatus.PENDING;
        }
    }
}
