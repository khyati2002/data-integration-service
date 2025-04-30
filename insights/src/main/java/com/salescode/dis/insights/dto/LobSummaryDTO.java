package com.salescode.dis.insights.dto;

import lombok.Data;

import java.util.List;

@Data
public class LobSummaryDTO {
    private String lob;
    private List<JobEntityResponseDtoWithFiles> jobs;

    public LobSummaryDTO(String lob, List<JobEntityResponseDtoWithFiles> jobs) {
        this.lob = lob;
        this.jobs = jobs;
    }
}
