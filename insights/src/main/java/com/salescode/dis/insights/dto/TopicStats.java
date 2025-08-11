package com.salescode.dis.insights.dto;
import lombok.Data;

@Data
public class TopicStats {
    private String lob;
    private long committedOffset;
    private long latestOffset;
    private long pending;
}
