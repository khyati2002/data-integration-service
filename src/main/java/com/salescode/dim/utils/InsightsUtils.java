package com.salescode.dim.utils;

import com.salescode.dim.StreamingRawData;
import com.salescode.dis.insights.dto.event.FileProgressEvent;
import com.salescode.dis.insights.dto.file.progress.FileProgressRequest;
import com.salescode.dis.insights.enums.ProgressStage;

public class InsightsUtils {
    public static FileProgressEvent createRequest(StreamingRawData streamingRawData, long successCount, long logicalFailureCount, long serverFailureCount, ProgressStage stage){
      FileProgressEvent fileProgressEvent = new FileProgressEvent();
      fileProgressEvent.setFileId(streamingRawData.getFileId());
      fileProgressEvent.setEventId(streamingRawData.getRequestId());
      fileProgressEvent.setLob(streamingRawData.getLob());
      fileProgressEvent.setMasterName(streamingRawData.getTransformerInfo().get(0).getEntityName());
      fileProgressEvent.setProgress(FileProgressRequest.builder().stageType(stage).successCount(successCount).logicalFailureCount(logicalFailureCount).serverFailureCount(serverFailureCount).build());
      fileProgressEvent.setErrorMessage(streamingRawData.getResponses().toString());
      return fileProgressEvent;
    }
}
