package com.salescode.dis.insights.dto.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.enums.ProgressStatus;
import lombok.Data;
import lombok.Getter;

import java.time.Instant;

@Data
@Getter
public class JobStageAccumulatedData {

    private String jobId;
    private Instant creationTime;
    private Instant lastModifiedTime;
    private String lob;
    private JsonNode extendedAttributes;
    private Instant startTime;
    private Instant endTime;
    private ProgressStatus status;
    private String publisherJobUri;
    private String consumerJobUri;
    private ProgressStage stageType;
    private Long totalSuccessCount;


    public JobStageAccumulatedData(
            String jobId, Instant creationTime, Instant lastModifiedTime, String lob,
            String extendedAttributesJson,
            Instant startTime, Instant endTime, ProgressStatus status,String publisherJobUri,
            String consumerJobUri, ProgressStage stageType, Long totalSuccessCount) { // Use Number for counts to be safe
        this.jobId = jobId;
        this.creationTime = creationTime;
        this.lastModifiedTime = lastModifiedTime;
        this.lob = lob;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.publisherJobUri = publisherJobUri;
        this.consumerJobUri = consumerJobUri;
        this.stageType = stageType;
        this.totalSuccessCount = totalSuccessCount != null ? totalSuccessCount : 0L;

        if (extendedAttributesJson != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                this.extendedAttributes = mapper.readTree(extendedAttributesJson);
            } catch (Exception e) {
                this.extendedAttributes = null;
            }
        }
    }


}