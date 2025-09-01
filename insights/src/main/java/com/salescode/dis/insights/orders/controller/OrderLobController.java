package com.salescode.dis.insights.orders.controller;

import com.salescode.dis.insights.orders.entity.LobRetentionConfigEntity;
import com.salescode.dis.insights.orders.dto.UpdateRetentionRequest;
import com.salescode.dis.insights.orders.service.LobRetentionConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/orders/lob")
public class OrderLobController {

    private static final Logger logger = LoggerFactory.getLogger(OrderLobController.class);

    @Autowired
    private LobRetentionConfigService lobRetentionConfigService;

    @GetMapping
    public ResponseEntity<List<LobRetentionConfigEntity>> getAllLobConfigs() {
        logger.info("Fetching all LOB retention configurations");
        List<LobRetentionConfigEntity> configs = lobRetentionConfigService.getAllConfigs();
        return ResponseEntity.ok(configs);
    }

    @PutMapping("/{lob}/retention")
    public ResponseEntity<LobRetentionConfigEntity> updateRetention(
            @PathVariable String lob,
            @Valid @RequestBody UpdateRetentionRequest request) {

        logger.info("Updating retention for LOB: {} to {} hours", lob, request.getRetentionHours());
        LobRetentionConfigEntity updated = lobRetentionConfigService.updateRetention(lob, request);
        return ResponseEntity.ok(updated);
    }
}