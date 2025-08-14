package com.salescode.dis.insights.event;

import ai.salescode.observability.toolkit.api.ObservabilityEventManager;
import com.salescode.dis.insights.observability.ProgressAggregatedEventLog;
import com.salescode.dis.insights.dto.event.FileProgressEvent;
import com.salescode.dis.insights.service.PropertyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class ObservabilityEventProducer {

    private final ObservabilityEventManager observabilityEventManager;
    private final PropertyService propertyService;

    public ObservabilityEventProducer(ApplicationContext context, PropertyService propertyService) {
        this.observabilityEventManager = getBeanSafely(context, ObservabilityEventManager.class).orElse(null);
        this.propertyService = propertyService;
    }

    private <T> Optional<T> getBeanSafely(ApplicationContext ctx, Class<T> clazz) {
        try {
            return Optional.of(ctx.getBean(clazz));
        } catch (Exception e) {
            log.debug("Bean of type {} not found in context", clazz.getName());
            return Optional.empty();
        }
    }

    public void emitProgressAggregatedEvent(FileProgressEvent aggregatedEvent) {
        if (aggregabilityEventManagerNullCheck()) return;

        if (aggregatedEvent == null) return;
        String env = propertyService.getEnvFromLob(aggregatedEvent.getLob());

        ProgressAggregatedEventLog event = new ProgressAggregatedEventLog()
                .setTraceId(UUID.randomUUID().toString())
                .setMasterName(aggregatedEvent.getMasterName())
                .setJobId(aggregatedEvent.getJobId())
                .setLob(aggregatedEvent.getLob())
                .setEnv(env)
                .setMinProcessingTime(aggregatedEvent.getProgress().getMinProcessingTimeMs())
                .setMaxProcessingTime(aggregatedEvent.getProgress().getMaxProcessingTimeMs())
                .setStageType(Optional.ofNullable(aggregatedEvent.getProgress().getStageType())
                        .map(Enum::name)
                        .orElse("UNKNOWN"))
                .setSuccessCount((aggregatedEvent.getProgress().getSuccessCount()))
                .setFailureCount((aggregatedEvent.getProgress().getLogicalFailureCount()+aggregatedEvent.getProgress().getServerFailureCount()))
                .setMessage(String.format("Aggregated progress for fileId=%s master=%s jobId=%s lob=%s progress=%s",
                        aggregatedEvent.getFileId(),
                        aggregatedEvent.getMasterName(),
                        aggregatedEvent.getJobId(),
                        aggregatedEvent.getLob(),
                        aggregatedEvent.getProgress()));

        log.debug("Sending observability event: {}", event);

        try {
            observabilityEventManager.emitEvent(event);
        } catch (Exception e) {
            log.error("Error while sending observability event: {}", e.getMessage(), e);
        }
    }

    // small helper to keep the null-check readable
    private boolean aggregabilityEventManagerNullCheck() {
        if (observabilityEventManager == null) {
            log.debug("ObservabilityEventManager not available, skipping event emission");
            return true;
        }
        return false;
    }
}
