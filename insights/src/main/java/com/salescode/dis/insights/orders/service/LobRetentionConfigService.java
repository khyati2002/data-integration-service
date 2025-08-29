package com.salescode.dis.insights.orders.service;
import com.salescode.dis.insights.orders.entity.LobRetentionConfigEntity;
import com.salescode.dis.insights.orders.repository.LobRetentionConfigRepository;
import com.salescode.dis.insights.orders.dto.UpdateRetentionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class LobRetentionConfigService {

    private static final Logger logger = LoggerFactory.getLogger(LobRetentionConfigService.class);

    @Autowired
    private LobRetentionConfigRepository repository;

    public List<LobRetentionConfigEntity> getAllConfigs() {
        logger.debug("Fetching all LOB retention configurations");
        return repository.findAll();
    }

    public LobRetentionConfigEntity updateRetention(String lob, UpdateRetentionRequest request) {
        logger.debug("Updating retention for LOB: {} to {} hours", lob, request.getRetentionHours());
        LobRetentionConfigEntity config = repository.findById(lob)
                .orElse(new LobRetentionConfigEntity(lob, request.getRetentionHours(), request.getUpdatedBy()));

        config.setRetentionHours(request.getRetentionHours());
        config.setUpdatedBy(request.getUpdatedBy());

        LobRetentionConfigEntity saved = repository.save(config);
        logger.debug("Updated retention configuration for LOB: {}", lob);

        return saved;
    }


}
