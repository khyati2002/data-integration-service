package com.applicate.services.channelkart.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;


@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Table(name = "ck_integration_history", indexes = {
        @Index(name = "ck_index_reqid", columnList = "requestId"),
        @Index(name = "ck_index_groupid", columnList = "groupId"),
        @Index(name = "ck_index_appid", columnList = "appId"),
        @Index(name = "ck_index_entity_name", columnList = "entityName"),
        @Index(name = "ck_index_entity_id", columnList = "entityId"),
        @Index(name = "ck_index_action", columnList = "action"),
        @Index(name = "ck_index_timestamp", columnList = "timestamp"),
        @Index(name = "ck_index_messagekey", columnList = "message_key"),
        @Index(name = "ck_index_offset", columnList = "offset")
})
public class IntegrationHistory extends CommonDataModel {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  private String requestId;

  private String appId;

  @Column(columnDefinition = "LONGTEXT")
  private String description;

  @Column(columnDefinition = "DOUBLE")
  private Double offset;

  public Double getOffset() { return offset; }

  public void setOffset(Double offset) { this.offset = offset; }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getEntityName() {
    return entityName;
  }

  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  private String groupId;

  private String entityName;

  private String entityId;

  private String action;

  private long timestamp;

  @Column(name="message_key")
  private String messageKey;

  public String getMessageKey() {
    return messageKey;
  }

  public void setMessageKey(String messageKey) {
    this.messageKey = messageKey;
  }

  public String getMessageHash() {
    return messageHash;
  }

  public void setMessageHash(String messageHash) {
    this.messageHash = messageHash;
  }

  @Column(columnDefinition = "LONGTEXT")
  private String messageHash;

  private String status;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

}
