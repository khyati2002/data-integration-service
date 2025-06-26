package com.salescode.dis.insights.dto.file.progress;

import com.salescode.dis.insights.enums.ModeOfIntegration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class FileProgressResponse {
    private String requestId;
    private String status;
    private String message;
    private String fileId;
    private String master;
    private ModeOfIntegration modeOfIntegration;
}