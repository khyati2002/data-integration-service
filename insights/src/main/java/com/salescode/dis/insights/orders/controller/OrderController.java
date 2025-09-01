package com.salescode.dis.insights.orders.controller;

import com.salescode.dis.insights.orders.entity.OrderEntity;
import com.salescode.dis.insights.orders.dto.OrderResponse;
import com.salescode.dis.insights.orders.dto.OrderSummaryResponse;
import com.salescode.dis.insights.orders.dto.UpdateStageRequest;
import com.salescode.dis.insights.orders.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderEntity entity) {
        logger.info("Creating order: {}", entity.getOrderNumber());
        OrderResponse response = orderService.createOrder(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrders(
            @RequestParam String lob,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderResponse> orders = orderService.getOrdersByLob(lob, pageable);
        return ResponseEntity.ok(orders);
    }


    //for updating stages throguh api
    @PatchMapping("/stage")
    public ResponseEntity<OrderResponse> updateStage(
            @Valid @RequestBody UpdateStageRequest request) {

        logger.info("Updating stage {} for order: {}", request.getStage(), request.getOrderNumber());
        OrderResponse response = orderService.updateOrderStage( request);
        return ResponseEntity.ok(response);
    }

    //for  lob wise getting order summary
    @GetMapping("/summary")
    public ResponseEntity<List<OrderSummaryResponse>> getOrdersSummary() {
        List<OrderSummaryResponse> summary = orderService.getOrdersSummary();
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<OrderResponse>> searchOrders(
            @RequestParam String lob,
            @RequestParam String field,
            @RequestParam String value,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) int size) {

        Pageable pageable = PageRequest.of(page, size);
        try {
            Page<OrderResponse> results = orderService.searchOrdersByField(lob, field, value, pageable);
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            logger.error("Error during search", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}