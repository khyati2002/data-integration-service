package com.salescode.channelkart.models.diff;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.HashMap;

@Getter
@Setter
public class RequestMetrics {

    private String uri;

    private String clientIp;

    private long duration;

    private String lob;

    private String streamId;

    private HashMap<String,String> parameters;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
    private Date eventTime;

    private String method;

    private int statusCode;

    private String user;

    private String body;

    private String  token;
}
