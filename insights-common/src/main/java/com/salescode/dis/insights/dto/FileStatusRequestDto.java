package com.salescode.dis.insights.dto;

import com.salescode.dis.insights.enums.FileStatus;
import lombok.*;

import javax.validation.constraints.AssertTrue;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class FileStatusRequestDto {

    private FileStatus consumedStatus;
    private FileStatus publishedStatus;

    @AssertTrue(message = "At least one of consumedStatus or publishedStatus must be set")
    public boolean isAtLeastOneStatusSet() {
        return consumedStatus != null || publishedStatus != null;
    }

} 