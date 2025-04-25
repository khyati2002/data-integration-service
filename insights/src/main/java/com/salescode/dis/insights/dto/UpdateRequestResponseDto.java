package com.salescode.dis.insights.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRequestResponseDto {
    private String requestId;
    private String status;
    private String message;
    private String fileId;
}