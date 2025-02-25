package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
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
@Builder
public class StreamingRawData implements Serializable, KeyedKafkaSerialization {

    private static final long serialVersionUID = -1415214398611751644L;

    private String requestId;
    private String groupId;
    private String fileId;
    private String lob;
    private String loginId;
    private int batchNumber;

    //    private transient List<Future<MdmOperationResponse>> response; // Keep this transient for serialization purposes
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

    private List<CommonDataModel> transformedData;

    public void incrementRetryCount() {
        this.retryCount += 1;
    }

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