package com.salescode.dis.insights.dto;

import com.salescode.dis.insights.dto.job.JobEntityResponseDtoWithStages;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Data
@Getter
@Setter
public class AccumulatedJobsAndMasterDto {
  private List<JobEntityResponseDtoWithStages> jobs;
  private List<MasterCard> masters;
}