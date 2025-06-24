package com.salescode.dis.insights.dto.file.progress;

import com.salescode.dis.insights.enums.ModeOfIntegration;
import lombok.*;

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