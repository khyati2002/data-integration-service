package com.salescode.dis.insights.dto;

import com.salescode.dis.insights.enums.FileStatus;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileStatusRequestDto {

    private FileStatus consumedStatus;
    private FileStatus publishedStatus;

    @AssertTrue(message = "At least one of consumedStatus or publishedStatus must be set")
    public boolean isAtLeastOneStatusSet() {
        return consumedStatus != null || publishedStatus != null;
    }

} 