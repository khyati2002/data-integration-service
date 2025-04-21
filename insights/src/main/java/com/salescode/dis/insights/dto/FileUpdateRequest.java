package com.salescode.dis.insights.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileUpdateRequest {
    private FileProgressRequest progress;
    private String status;

    // Getters and Setters

    public boolean isProgressUpdate() {
        return progress != null;
    }

    public boolean isStatusUpdate() {
        return status != null;
    }
}

