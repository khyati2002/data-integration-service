package com.salescode.dis.insights.orders.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "orders")
public class OrderEntity {

    // Getters and Setters
    @Id
    @GeneratedValue
    private UUID id;

    @NotNull
    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @NotNull
    @Column(name = "lob", nullable = false)
    private String lob;

    @NotNull
    @Column(name =  "\"user\"", nullable = false)  // "user" in quotes in SQL
    private String user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false)
    private Operation operation;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "publish_status", nullable = false)
    private Status publishStatus = Status.SUCCESS;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "read_status", nullable = false)
    private Status readStatus = Status.PENDING;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "process_status", nullable = false)
    private Status processStatus = Status.PENDING;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "save_status", nullable = false)
    private Status saveStatus = Status.PENDING;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public enum Operation {
        INSERT, UPDATE
    }
    public enum Status {
        PENDING, SUCCESS, FAILURE, NA
    }

    // Constructors
    public OrderEntity() {}

    public OrderEntity(String orderNumber, String lob, String user, Operation operation, OffsetDateTime createdAt) {
        this.orderNumber = orderNumber;
        this.lob = lob;
        this.user = user;
        this.operation = operation;
        this.createdAt = createdAt;
    }

}