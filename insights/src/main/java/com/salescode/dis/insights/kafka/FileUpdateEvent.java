package com.salescode.dis.insights.kafka;

import com.salescode.dis.insights.dto.FileUpdateRequestDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileUpdateEvent {
    private String fileId;
    private String lob;
    private String masterName;
    private FileUpdateRequestDto updateRequest;
    private long timestamp;
}