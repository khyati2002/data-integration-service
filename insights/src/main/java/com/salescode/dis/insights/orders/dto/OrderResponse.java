package com.salescode.dis.insights.orders.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.salescode.dis.insights.orders.entity.OrderEntity;
import com.salescode.dis.insights.orders.service.OrderService;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
public class OrderResponse {

    // Getters and Setters
    private UUID id;
    private String orderNumber;
    private String lob;
    private String user;
    private String errorMessage;
    private OrderEntity.Operation operation;
    private OrderEntity.Status readStatus;
    private OrderEntity.Status processStatus;
    private OrderEntity.Status saveStatus;
    private OrderEntity.Status publishStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private OffsetDateTime createdAt;

    public OrderResponse(OrderEntity entity) {
        this.id = entity.getId();
        this.orderNumber = entity.getOrderNumber();
        this.lob = entity.getLob();
        this.user = entity.getUser();
        this.operation = entity.getOperation();
        this.readStatus = entity.getReadStatus();
        this.processStatus = entity.getProcessStatus();
        this.saveStatus = entity.getSaveStatus();
        this.publishStatus=entity.getPublishStatus();
        this.createdAt = entity.getCreatedAt();
        this.errorMessage=entity.getErrorMessage();
    }

}