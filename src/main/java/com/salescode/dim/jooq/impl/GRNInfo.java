package com.salescode.dim.jooq.impl;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.models.enums.GRNStatus;
import com.applicate.services.channelkart.validations.ValidationResponseMessage;
import org.hibernate.validator.constraints.Length;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;


public class GRNInfo extends com.salescode.dim.jooq.generated.tables.pojos.GrnInfo implements Serializable {

  @Column(name = "invoice_number", nullable = false, unique = true)
  @NotNull
  @Size(min = 3, max = 50)
  private String invoiceNumber; // The unique identifier for the GRN - invoice_number, also used to associate
  // with the Sales entity.

  @Column(name = "order_number")
  private String orderNumber; // The unique identifier for the GRN - order_number, also used to associate
  // with the Order entity.

  @Column(name = "login_id", length = 100, nullable = false)
  @NotNull
  @Size(min = 3, max = 50)
  private String loginId; // The ID of the user who is responsible for the GRN.

  @Column(name = "grn_status", length = 50, nullable = false)
  @NotNull
  private String grnStatus; // The current status of the GRN

  @Column(name = "rejection_reason", length = 200, nullable = true)
  private String rejectionReason; // holds the reason for the rejection.

  @Column(name = "grn_number", columnDefinition = "varchar(200) unique not null")
  @NotNull(message = ValidationResponseMessage.NOTNULL)
  @Length(min = 0, max = 200, message = ValidationResponseMessage.LENGTH)
  private String grnNumber; // holds auto generated number by enrichmenet

  public String getGrnNumber() {
    return this.grnNumber;
  }

  public void setGrnNumber(String grnNumber) {
    this.grnNumber = grnNumber;
  }

  public String getOrderNumber() {
    return orderNumber;
  }

  public void setOrderNumber(String orderNumber) {
    this.orderNumber = orderNumber;
  }

  public String getRejectionReason() {
    return this.rejectionReason;
  }

  public void setRejectionReason(String rejectionReason) {
    this.rejectionReason = rejectionReason;
  }

  public String getInvoiceNumber() {
    return this.invoiceNumber;
  }

  public void setInvoiceNumber(String invoiceNumber) {
    this.invoiceNumber = invoiceNumber;
  }

  public String getLoginId() {
    return loginId;
  }

  public void setLoginId(String loginId) {
    this.loginId = loginId;
  }

  public String getGrnStatus() {
    return grnStatus;
  }

  public void setGrnStatus(String grnStatus) {
    this.grnStatus = GRNStatus.getStatus(grnStatus).name();
  }

  public GRNInfo() {
    // Default constructor required by JPA
  }

  public GRNInfo(String invoiceNumber, String orderNumber, String loginId, String grnStatus) {
    this.invoiceNumber = invoiceNumber;
    this.loginId = loginId;
    this.grnStatus = GRNStatus.getStatus(grnStatus).name();
    this.orderNumber = orderNumber;
  }

}
