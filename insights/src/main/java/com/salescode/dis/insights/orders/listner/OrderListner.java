package com.salescode.dis.insights.orders.listner;

import com.salescode.dis.insights.orders.entity.OrderEntity;
import com.salescode.dis.insights.orders.service.OrderService;
import com.salescode.dis.insights.service.PropertyService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

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
    private final PropertyService propertyService;


    @PostConstruct
    public void startListeners() {
        List<String> enabledLobs=propertyService.getEnabledLobs();
        for (String lob : enabledLobs) {
        String topic = lob + "-event-streams";
        ContainerProperties containerProps = new ContainerProperties(topic);
        containerProps.setGroupId("order-group-" + lob); // optional: per LOB group
        containerProps.setMessageListener((MessageListener<String, byte[]>) this::handleMessage);
        KafkaMessageListenerContainer<String, byte[]> container =
                new KafkaMessageListenerContainer<>(consumerFactory, containerProps);
        container.setBeanName("OrderListener-" + lob);
        container.start();
        log.info("Started listener for topic: {}", topic);
    }
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

