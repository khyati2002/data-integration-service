package com.salescode.dis.insights.dto;

import com.salescode.dis.insights.enums.ModeOfIntegration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class FileProgressRequest implements Serializable {
    String fileId;
    String masterName;
    String stageName;
    Long successCount;
    Long failureCount;
    Long minProcessingTimeMs;
    Long maxProcessingTimeMs;

    public static FileProgressRequest createNewInstance() {
        return new FileProgressRequest();
    }
}