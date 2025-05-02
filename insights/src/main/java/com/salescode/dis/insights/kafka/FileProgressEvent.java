package com.salescode.dis.insights.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.salescode.dis.insights.dto.FileProgressRequest;
import lombok.Data;

import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileProgressEvent {
    private String eventId;
    private String fileId;
    private String jobId;
    private String lob;
    private String masterName;
    private FileProgressRequest progress;
    private long timestamp = System.currentTimeMillis();

    private String errorMessage;

}