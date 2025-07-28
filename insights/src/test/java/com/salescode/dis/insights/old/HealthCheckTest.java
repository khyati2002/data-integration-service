package com.salescode.dis.insights.old;


import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dis.insights.utils.JsonUtils;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles({"postgres", "dev", "debug", "kafka", "test"})
public class HealthCheckTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @SneakyThrows
    @Test
    @Order(1)
    void testCreateJob() {
        // Make request
        String response = restTemplate.getForObject(
                "/hckeck",
                String.class
        );

        String status = JsonUtils.fromJson(response, ObjectNode.class).get("status").asText();

        // Assertions
        assertEquals("UP",status);

    }
}
