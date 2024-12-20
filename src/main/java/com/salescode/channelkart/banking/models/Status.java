package com.salescode.channelkart.banking.models;

public enum Status {
  PENDING("PENDING"),
  ACTIVE("ACTIVE"),
  DECLINED("DECLINED"),
  CANCELED("CANCELLED"),
  ON_HOLD("ON HOLD"),
  INACTIVE("INACTIVE"),
  FAILURE("FAILURE"),
  SUCCESS("SUCCESS"),
  INPROGRESS("IN PROGRESS"),
  EXPIRED("EXPIRED"),
  ACCOUNT_DETAILS_UPDATED("ACCOUNT_DETAILS_UPDATED"),
  REJECTED("REJECTED"),
  APPROVED("APPROVED"),
  ACCOUNT_CREATED("ACCOUNT_CREATED");

  private final String statusDesc;

  Status(String statusDesc){
    this.statusDesc = statusDesc;
  }

  public String getStatusDesc() {
    return statusDesc;
  }
}

