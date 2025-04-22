package com.salescode.dis.insights.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileUpdateRequestDto {

    private FileProgressRequest progress;

    private FileStatusRequestDto status;

    public boolean isProgressUpdate() {
        return progress != null;
    }

    public boolean isStatusUpdate() {
        return status != null;
    }
}

