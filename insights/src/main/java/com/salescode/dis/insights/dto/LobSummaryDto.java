package com.salescode.dis.insights.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter
public class LobSummaryDto {
    private String lob;
    private Double avgThroughput;
    private BigDecimal maxThroughput; // Assuming throughput can be large, use Long or Double
    private Long queueSuccessCount;
    private Long queueFailureCount;
    private Long saveSuccessCount;
    private Long saveFailureCount;
    private Long pendingJobCount;
    private Long completedJobCount;
    private Long failedJobCount;

    public LobSummaryDto(
            String lob,
            Double avgThroughput, BigDecimal maxThroughput,
            Long queueSuccessCount, Long queueFailureCount,
            Long saveSuccessCount, Long saveFailureCount,
            Long pendingJobCount, Long completedJobCount, Long failedJobCount) {
        this.lob = lob;
        this.avgThroughput = avgThroughput;
        this.maxThroughput = maxThroughput;
        this.queueSuccessCount = queueSuccessCount;
        this.queueFailureCount = queueFailureCount;
        this.saveSuccessCount = saveSuccessCount;
        this.saveFailureCount = saveFailureCount;
        this.pendingJobCount = pendingJobCount;
        this.completedJobCount = completedJobCount;
        this.failedJobCount = failedJobCount;
    }

}