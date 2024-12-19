package com.salescode.channelkart.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;


@Setter
@Getter
@Builder
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
@NoArgsConstructor
@AllArgsConstructor
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

    private String groupId;

  private String entityName;

  private String entityId;

  private String action;

  private long timestamp;

  @Column(name="message_key")
  private String messageKey;

    @Column(columnDefinition = "LONGTEXT")
  private String messageHash;

  private String status;

}
