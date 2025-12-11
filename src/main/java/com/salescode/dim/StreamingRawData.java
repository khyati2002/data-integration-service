package com.salescode.dim;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class StreamingRawData implements Serializable {

    private static final long serialVersionUID = -1415214398611751644L;

    private String requestId;
    private String groupId;
    private String fileId;
    private String lob;
    private String loginId;
    private int batchNumber;

    private String submittedBy;
    private List<TransformerInfo> transformerInfo;
    private ArrayNode features;
    private List<Response> responses;
    private String status;
    private String appId = "integration";
    private String offset;

    private Map<String, String> headersMap;
    private int retryCount = 0;
    private boolean preserveOnFailure;
    private boolean ignoreS3Log;
    private Integer batchSize;
    private String topicName;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response implements Serializable {
        private static final long serialVersionUID = 1L;
        private String status;
        private String message;
    }

}