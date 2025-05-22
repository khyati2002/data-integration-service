package com.salescode.dis.insights.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRequestResponseDto {
    private String requestId;
    private String status;
    private String message;
    private String fileId;
    private String master;
}