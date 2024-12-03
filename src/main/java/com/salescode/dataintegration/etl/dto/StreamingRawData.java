package com.salescode.dataintegration.etl.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.channelkart.services.enums.OperationType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamingRawData implements Serializable {

    public String requestId;
    public String groupId;
    private String fileId;
    private String lob;
    private String loginId;
    private String submittedBy;
    private String topicName;
    private List<TransformerInfoRequest> transformerInfo;
    private ArrayNode features;
    private boolean preserveOnFailure;
    private String appId = "integration";
    private String status;
    private String offset;
    private int retryCount = 0;
    private boolean ignoreS3Log;

    //  private transient List<Future<MdmOperationResponse>> response;


    @Getter
    @Setter
    public static class TransformerInfoRequest implements Serializable {
        private String entityName;
        private String transformerId;
        private boolean skipPersist;
        private boolean skipPreprocessing;
        private OperationType operationType = OperationType.insert;
        private String preprocessValidationExcludeGroup;
    }
}
