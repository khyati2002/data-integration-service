package com.salescode.dis.insights.orders.listner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dis.insights.dto.event.FileProgressEvent;
import com.salescode.dis.insights.orders.entity.OrderEntity;
import com.salescode.dis.insights.orders.dto.UpdateStageRequest;
import com.salescode.dis.insights.orders.service.OrderService;
import com.salescode.dis.insights.validation.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("kafka")
@RequiredArgsConstructor
@Slf4j
@EnableKafka
public class OrderProgressEventListener {

    private final OrderService orderService;
    private final ObjectMapper mapper;

    @KafkaListener(
            topics = "${insights.order.topic:order-progress-updates}",
            groupId = "${insights.order.group-id:order-progress-processor}",
            containerFactory = "orderProgressContainerFactory"
    )
    public void consumeOrderProgressEvents(String message) {
        try {

            UpdateStageRequest payload = mapper.readValue(message, UpdateStageRequest.class);
            if (payload == null || payload.getOrderNumber() == null) {
                log.warn("Received invalid payload (null or missing orderNumber): {}", message);
                return;
            }
            UpdateStageRequest.Stage stage = payload.getStage() != null ? payload.getStage() : UpdateStageRequest.Stage.READ;
            OrderEntity.Status status = payload.getStatus() != null ? payload.getStatus() : OrderEntity.Status.PENDING;
            String lob = payload.getLob() != null ? payload.getLob() : "UNKNOWN_LOB";
            String errorMessage=payload.getErrorMessage();
            UpdateStageRequest dto = new UpdateStageRequest(payload.getOrderNumber(), stage, status, lob, payload.getUser(), errorMessage);

            orderService.updateOrderStage(dto);
            log.info("Processed order progress event for orderNumber={}, lob={}, stage={}, status={}",
                    dto.getOrderNumber(), dto.getLob(), dto.getStage(), dto.getStatus());

        } catch (JsonProcessingException e) {
            log.error("Failed to parse order progress message: {}. error: {}", message, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error processing order progress event: {}. error: {}", message, e.getMessage(), e);
        }
    }
}
