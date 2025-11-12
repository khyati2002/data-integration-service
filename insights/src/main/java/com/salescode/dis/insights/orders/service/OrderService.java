package com.salescode.dis.insights.orders.service;

import com.salescode.dis.insights.orders.entity.LobRetentionConfigEntity;
import com.salescode.dis.insights.orders.repository.LobRetentionConfigRepository;
import com.salescode.dis.insights.orders.entity.OrderEntity;
import com.salescode.dis.insights.orders.repository.OrderRepository;
import com.salescode.dis.insights.orders.dto.OrderResponse;
import com.salescode.dis.insights.orders.dto.OrderSummaryResponse;
import com.salescode.dis.insights.orders.dto.UpdateStageRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private  final JdbcTemplate jdbcTemplate;
    private final  LobRetentionConfigRepository lobRetentionConfigRepository;

    public OrderResponse createOrder(OrderEntity entity) {
        logger.debug("Creating order: {}", entity.getOrderNumber());
        jdbcTemplate.execute(
                "SELECT ensure_partition_for_order('" + entity.getLob() + "', '" + entity.getCreatedAt() + "')"
        );
        if(entity.getPublishStatus()!= OrderEntity.Status.NA)
            entity.setPublishStatus(OrderEntity.Status.SUCCESS);

        OrderEntity saved = orderRepository.save(entity);
        logger.debug("Created order with ID: {}", saved.getId());
        return new OrderResponse(saved);
    }

    public Page<OrderResponse> getOrdersByLob(String lob, Pageable pageable) {
        logger.debug("Fetching orders for lob: {} with pagination: {}", lob, pageable);

        return orderRepository.findByLobOrderByCreatedAtDesc(lob, pageable)
                .map(OrderResponse::new);
    }

    public OrderResponse updateOrderStage(UpdateStageRequest request) {
        logger.debug("Updating stage {} for order: {}", request.getStage(), request.getOrderNumber());

        OrderEntity entity = orderRepository.findByOrderNumberAndLob(request.getOrderNumber(), request.getLob())
                .orElseGet(() -> {
                    OrderEntity newEntity = new OrderEntity();
                    newEntity.setOrderNumber(request.getOrderNumber());
                    newEntity.setLob(request.getLob());
                    newEntity.setUser(request.getUser());
                    newEntity.setOperation(OrderEntity.Operation.UNKNOWN);
                    newEntity.setUser(request.getUser()!=null? request.getUser() :"unknown user");
                    newEntity.setPublishStatus(OrderEntity.Status.NA);
                    newEntity.setCreatedAt(OffsetDateTime.now());
                    newEntity.setErrorMessage(request.getErrorMessage());
                    createOrder(newEntity);
                    return newEntity;
                });

        switch (request.getStage()) {
            case READ:
                entity.setReadStatus(request.getStatus());
                appendErrorMessage(entity, request.getErrorMessage());
                break;
            case PROCESS:
                entity.setProcessStatus(request.getStatus());
                appendErrorMessage(entity, request.getErrorMessage());
                break;
            case SAVE:
                entity.setSaveStatus(request.getStatus());
                appendErrorMessage(entity, request.getErrorMessage());
                break;
                default:  appendErrorMessage(entity, request.getErrorMessage());
        }
        OrderEntity updated = orderRepository.save(entity);
        logger.debug("Updated order stage for: {}", request.getOrderNumber());
        return new OrderResponse(updated);
    }

    public List<OrderSummaryResponse> getOrdersSummary(List<String> lobs) {
        logger.debug("Generating orders summary");

        // Get retention config for all lobs
        Map<String, Integer> retentionMap = lobRetentionConfigRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        LobRetentionConfigEntity::getLob,
                        LobRetentionConfigEntity::getRetentionHours
                ));


        List<Object[]> summaryData = orderRepository.findOrderSummaryByLob(lobs);

        return summaryData.stream().map(row -> {
            String lob = (String) row[0];
            Long totalRecords = ((Number) row[1]).longValue();
            Long readSuccess = ((Number) row[2]).longValue();
            Long readFailure = ((Number) row[3]).longValue();
            Long readPending = ((Number) row[4]).longValue();
            Long processSuccess = ((Number) row[5]).longValue();
            Long processFailure = ((Number) row[6]).longValue();
            Long processPending = ((Number) row[7]).longValue();
            Long saveSuccess = ((Number) row[8]).longValue();
            Long saveFailure = ((Number) row[9]).longValue();
            Long savePending = ((Number) row[10]).longValue();
            Long publishSuccess = ((Number) row[11]).longValue();
            Long publishNA = ((Number) row[12]).longValue();

            Integer retentionHours = retentionMap.getOrDefault(lob, 24);

            return new OrderSummaryResponse(lob, retentionHours, totalRecords,
                    readSuccess, readFailure, readPending,
                    processSuccess, processFailure, processPending,
                    saveSuccess, saveFailure, savePending,publishSuccess,publishNA );
        }).toList();
    }
    public Page<OrderResponse> searchOrdersByField(String lob, String field, String value, Pageable pageable) {
        logger.debug("Searching orders for lob: {}, field: {}, value: {}", lob, field, value);

        Page<OrderEntity> pageResult = switch (field.toLowerCase()) {
            case "ordernumber" ->
                    orderRepository.findByLobAndOrderNumberContainingIgnoreCaseOrderByCreatedAtDesc(lob, value, pageable);
            case "user" ->
                    orderRepository.findByLobAndUserContainingIgnoreCaseOrderByCreatedAtDesc(lob, value, pageable);
            default -> throw new IllegalArgumentException("Unsupported search field: " + field);
        };

        return pageResult.map(OrderResponse::new);
    }

    private void appendErrorMessage(OrderEntity entity, String newError) {
        if (newError == null || newError.isBlank()) {
            return;
        }
        String existing = entity.getErrorMessage();
        if (existing == null || existing.isBlank()) {
            entity.setErrorMessage(newError);
        } else if(!existing.equals(newError)) {
            entity.setErrorMessage(existing + " | " + newError);
        }
    }




}