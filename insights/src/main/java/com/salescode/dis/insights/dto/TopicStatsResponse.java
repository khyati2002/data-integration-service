package com.salescode.dis.insights.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicStatsResponse {
    private Instant lastFetched;
    private List<TopicStats> lobStats;
}
