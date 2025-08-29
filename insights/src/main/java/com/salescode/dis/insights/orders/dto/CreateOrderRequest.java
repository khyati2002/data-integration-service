package com.salescode.dis.insights.orders.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.salescode.dis.insights.orders.entity.OrderEntity;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@Setter
@Getter
public class CreateOrderRequest {

    // Getters and Setters
    @NotBlank(message = "Order number is required")
    private String orderNumber;

    @NotBlank(message = "LOB is required")
    private String lob;

    @NotBlank(message = "User is required")
    private String user;

    @NotNull(message = "Operation is required")
    private OrderEntity.Operation operation;

    // Optional status fields - will default to PENDING if not provided
    private OrderEntity.Status readStatus;
    private OrderEntity.Status processStatus;
    private OrderEntity.Status saveStatus;

    @NotNull(message = "Created at timestamp is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private OffsetDateTime createdAt;
}
