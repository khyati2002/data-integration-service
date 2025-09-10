package com.salescode.dis.insights.orders.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.salescode.dis.insights.orders.entity.OrderEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateStageRequest {

    public UpdateStageRequest(String orderNumber, Stage stage, OrderEntity.Status status, String lob, String user, String errorMessage) {
        this.stage = stage;
        this.status = status;
        this.lob = lob;
        this.orderNumber = orderNumber;
        this.user=user;
        this.errorMessage=errorMessage;
    }
    @NotNull(message = "OrderNumber is required")
    private String orderNumber;
    @NotNull(message = "Stage is required")
    private Stage stage;
    @NotNull(message = "Status is required")
    private OrderEntity.Status status;
    @NotNull(message = "Status is required")
    private String lob;
    @NotNull(message = "Status is required")
    private String user;
    private String errorMessage;

    public enum Stage {
        PUBLISH, READ, PROCESS, SAVE
    }

}