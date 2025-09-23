package com.salescode.dis.insights.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter
@Builder
public class MasterCard {

    private String masterName;
    private Long pendingJobCount;
    private Long completedJobCount;
    private Long failedJobCount;
    private Long queueCount;
    private Long saveCount;


}