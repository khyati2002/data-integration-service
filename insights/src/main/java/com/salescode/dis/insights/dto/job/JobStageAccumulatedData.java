package com.salescode.dis.insights.dto.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.enums.ProgressStatus;
import lombok.Data;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Data
@Getter
public class JobStageAccumulatedData {

    private String jobId;
    String master;
    private ModeOfIntegration modeOfIntegration;
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
    private Long serverFailureCount;
    private Long logicalFailureCount;

    public JobStageAccumulatedData(
            String jobId, String master, ModeOfIntegration modeOfIntegration, Instant creationTime, Instant lastModifiedTime, String lob,
            String extendedAttributesJson,
            Instant startTime, Instant endTime, ProgressStatus status, String publisherJobUri,
            String consumerJobUri, ProgressStage stageType, Long totalSuccessCount, Long serverFailureCount, Long logicalFailureCount) { // Use Number for counts to be safe
        this.jobId = jobId;
        this.master = master;
        this.modeOfIntegration = modeOfIntegration;
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
        this.serverFailureCount = serverFailureCount != null ? serverFailureCount : 0L;
        this.logicalFailureCount = logicalFailureCount != null ? logicalFailureCount : 0L;

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