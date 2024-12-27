package com.salescode.channelkart.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.channelkart.pojo.MdmOperationResponse;
import com.salescode.channelkart.pojo.TaskAttributeRequestTemplate;
import com.salescode.channelkart.pojo.TaskAttributeRequestTemplate.TransformerInfo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

@JsonInclude(Include.NON_NULL)
public class StreamingRawData implements Serializable {

  public String requestId;
  public String groupId;
  private String fileId;
  private String lob;
  private String loginId;
  private transient List<Future<MdmOperationResponse>> response;
  private String submittedBy;
  private List<TransformerInfo> transformerInfo;
  private ArrayNode features;
  private List<Response> responses;
  private String status;
  private String appId="integration";
  private String offset;

  public Map<String, String> getHeadersMap() {
    return headersMap;
  }

  public void setHeadersMap(Map<String, String> headersMap) {
    this.headersMap = headersMap;
  }

  private Map<String,String> headersMap;
  public String getOffset() { return offset; }

  public void setOffset(String offset) { this.offset = offset; }

  private int retryCount = 0;

  public int getRetryCount(){ return retryCount;}
  public void incrementRetryCount(){
    this.retryCount+=1;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public boolean isPreserveOnFailure() {
    return preserveOnFailure;
  }

  public void setPreserveOnFailure(boolean preserveOnFailure) {
    this.preserveOnFailure = preserveOnFailure;
  }

  private boolean preserveOnFailure;
  
  private boolean ignoreS3Log;
  private Integer batchSize;


  public String getTopicName() {
    return topicName;
  }

  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

  private String topicName;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getLoginId() {
    return loginId;
  }

  public void setLoginId(String loginId) {
    this.loginId = loginId;
  }

  public List<Future<MdmOperationResponse>> getResponse() {
    return response;
  }

  public void setResponse(
          List<Future<MdmOperationResponse>> response) {
    this.response = response;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }
  public String getFileId() {
    return fileId;
  }
  public void setFileId(String fileId) {
    this.fileId = fileId;
  }
  public String getLob() {
    return lob;
  }

  public void setLob(String lob) {
    this.lob = lob;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public void setSubmittedBy(String submittedBy) {
    this.submittedBy = submittedBy;
  }

  public List<TransformerInfo> getTransformerInfo() {
    return transformerInfo;
  }

  public void setTransformerInfo(
          List<TransformerInfo> transformerInfo) {
    this.transformerInfo = transformerInfo;
  }

  public ArrayNode getFeatures() {
    return features;
  }

  public List<Response> getResponses() {
    return responses;
  }

  public void setResponses(
          List<Response> responses) {
    this.responses = responses;
  }

  public void setFeatures(ArrayNode features) {
    this.features = features;
  }

  public Integer getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(Integer batchSize) {
    this.batchSize = batchSize;
  }

  public static class Response implements Serializable{
    private static final long serialVersionUID = 1L;

    private String status;

    private String message;

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public String getStatue() {
      return status;
    }

    public void setStatue(String status) {
      this.status = status;
    }


  }

/**
 * @return the ignoreS3Log
 */
public boolean isIgnoreS3Log() {
	return ignoreS3Log;
}

/**
 * @param ignoreS3Log the ignoreS3Log to set
 */
public void setIgnoreS3Log(boolean ignoreS3Log) {
	this.ignoreS3Log = ignoreS3Log;
}


}
