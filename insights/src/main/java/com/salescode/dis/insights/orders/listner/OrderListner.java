package com.salescode.dis.insights.orders.listner;

import com.salescode.dis.insights.orders.entity.OrderEntity;
import com.salescode.dis.insights.orders.service.OrderService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.EnableKafka;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Service;

@Service
@EnableKafka
@Log4j2
@RequiredArgsConstructor
@Profile("kafka")
public class OrderListner {

    private final OrderService ordersService;
    private final ConsumerFactory<String, byte[]> consumerFactory;
    private final JdbcTemplate jdbcTemplate;
    private final Map<String, KafkaMessageListenerContainer<String, byte[]>> activeContainers = new ConcurrentHashMap<>();


    public List<String> fetchEnabledLobValues() {
        String sql = "SELECT value FROM insights_metadata WHERE key = 'orderLobs' LIMIT 1";
        List<String> rows = jdbcTemplate.queryForList(sql, String.class);
        if (rows.isEmpty()) {
            log.warn("No 'orderLobs' entry found in insights_metadata. No listeners will be started.");
            return Collections.emptyList();
        }
        String value = rows.get(0);
        if (value == null || value.trim().isEmpty()) {
            log.warn("'orderLobs' value is empty.");
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
    @PostConstruct
    public List<String> refreshListeners()
    {
       return  startListeners();
    }

    private List<String> startListeners() {

        activeContainers.values().forEach(container -> {
            try {
                container.stop();
                log.info("Stopped listener: {}", container.getBeanName());
            } catch (Exception e) {
                log.warn("Error stopping container {}", container.getBeanName(), e);
            }
        });
        activeContainers.clear();

        List<String> enabledLobs = fetchEnabledLobValues();
        for (String lob : enabledLobs) {
            String topic = lob + "-event-streams";
            ContainerProperties containerProps = new ContainerProperties(topic);
            containerProps.setGroupId("order-group-" + lob);
            containerProps.setMessageListener((MessageListener<String, byte[]>) this::handleMessage);
            KafkaMessageListenerContainer<String, byte[]> container =
                    new KafkaMessageListenerContainer<>(consumerFactory, containerProps);
            container.setBeanName("OrderListener-" + lob);
            container.start();
            activeContainers.put(lob, container);
            log.info("Started listener for topic: {}", topic);
        }
        return enabledLobs;
    }
    private void handleMessage(ConsumerRecord<String, byte[]> rec) {
        try {
            Header h = rec.headers().lastHeader("insights-json");
            if (h == null) return;

            String headerJson = new String(h.value(), StandardCharsets.UTF_8);
            Map<String, Object> headerMap = new ObjectMapper().readValue(headerJson, Map.class);
            if(!headerMap.get("class").equals("Order")) return;

            OrderEntity order = setCommonProps(headerMap);
            headerMap.keySet().stream()
                    .filter(k -> k.startsWith("id"))
                    .forEach(idKey -> {
                        OrderEntity newOrder = setOrderId(order, headerMap.get(idKey).toString());
                        ordersService.createOrder(newOrder);
                    });

        } catch (Exception e) {
            log.error("Error processing message", e);
        }
    }

    private OrderEntity setOrderId(OrderEntity order, String idKey)
    {
        order.setOrderNumber(idKey);
        return order;
    }
    private OrderEntity setCommonProps(Map<String, Object> map)
    {
        OrderEntity order =new OrderEntity();
        order.setLob(map.get("lob").toString());
        order.setUser(map.get("user").toString());
        order.setOperation(map.get("operation").toString().equals("INSERT")?OrderEntity.Operation.INSERT: OrderEntity.Operation.UPDATE);
        order.setReadStatus(OrderEntity.Status.PENDING);
        order.setProcessStatus( OrderEntity.Status.PENDING);
        order.setSaveStatus(OrderEntity.Status.PENDING);
        order.setCreatedAt(OffsetDateTime.now());
        return order;
    }
}

