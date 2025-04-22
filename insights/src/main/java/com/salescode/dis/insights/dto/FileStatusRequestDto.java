package com.salescode.dis.insights.dto;

import com.salescode.dis.insights.enums.FileStatus;
import com.salescode.dis.insights.enums.JobStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileStatusRequestDto {

    private FileStatus consumedStatus;

    private FileStatus publishedStatus;

    public boolean hasConsumedStatus() {
        return consumedStatus != null;
    }

    public boolean hasPublishedStatus() {
        return publishedStatus != null;
    }
}
