package com.salescode.channelkart.models.diff;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class EventMetrics {

    private String id;

    private int retryCount;

    private String type;

    private String lob;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
    private Date eventTime;

    private String createdBy;

    private JsonNode payload;
}

