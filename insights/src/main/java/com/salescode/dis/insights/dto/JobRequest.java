// File: dto/JobRequest.java
package com.salescode.dis.insights.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobRequest {
    @NotBlank
    private String lob;
    @NotBlank
    private String master;
}