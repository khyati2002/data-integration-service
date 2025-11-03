package com.salescode.dis.insights.scheduler;


import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.enums.ProgressStatus;
import com.salescode.dis.insights.repository.FileStageMetricsRepository;
import com.salescode.dis.insights.service.strategy.ApiClientBasedFileOperationStrategy;
import com.salescode.dis.insights.service.strategy.MdmKafkaFileOperationStrategy;
import com.salescode.dis.insights.service.strategy.StreamletSyncOperationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class FileStageStatusScheduler {

    // Define constants for time window
    @Value("${file-status-scheduler.stale-threshold-seconds:300}")
    public int STALE_THRESHOLD_SECONDS;

    @Value("${file-status-scheduler.too-old-threshold-seconds:900}")
    public int TOO_OLD_THRESHOLD_SECONDS;

    private final FileStageMetricsRepository fileStageMetricsRepository;

    private final ApiClientBasedFileOperationStrategy apiClientBasedFileOperationStrategy;

    private final StreamletSyncOperationStrategy streamletSyncOperationStrategy;
    private final MdmKafkaFileOperationStrategy mdmKafkaFileOperationStrategy;

    @Scheduled(fixedRateString = "${file-status-scheduler.rate-millis:60000}") // Run every 1 minute (60000 ms)
    @Transactional
    public void updateAllFileStatus() {
//        log.info("Starting scheduled update of all file statuses");

        // Define the time window for staleness
        Instant staleCutoffTime = Instant.now().minus(STALE_THRESHOLD_SECONDS, ChronoUnit.SECONDS); // e.g., 10 mins ago
        Instant tooOldCutoffTime = Instant.now().minus(TOO_OLD_THRESHOLD_SECONDS, ChronoUnit.SECONDS); // e.g., 15 mins ago

        List<FileStageMetrics> pendingStages = fileStageMetricsRepository.findStalePendingStages(staleCutoffTime, tooOldCutoffTime, ProgressStatus.PENDING);

        if (pendingStages.isEmpty()) {
            log.info("No stale files found in the {}-{} minute window.", STALE_THRESHOLD_SECONDS, TOO_OLD_THRESHOLD_SECONDS);
            return;
        }
        log.warn("Found {} potentially stale stages (PENDING, modified between {}-{} mins ago). Marking as FAILED.", pendingStages.size(), STALE_THRESHOLD_SECONDS, TOO_OLD_THRESHOLD_SECONDS);

        log.info("Found {} stages with PENDING status modified in the last 10 minutes", pendingStages.size());

        for (FileStageMetrics stage : pendingStages) {
            if(stage.getModeOfIntegration().equals(ModeOfIntegration.CK_STREAMLET_SYNC)){
                streamletSyncOperationStrategy.updateStatus(stage);
            }
            else if(stage.getModeOfIntegration().equals(ModeOfIntegration.CK_MDM_KAFKA)){
               mdmKafkaFileOperationStrategy.updateStatus(stage);
            }
            else {
                apiClientBasedFileOperationStrategy.updateStatus(stage);
            }
            correctPublishIfPresent(stage);
        }
    }

    private  void correctPublishIfPresent(FileStageMetrics stage)
    {
        if(stage.getStageType()!=ProgressStage.PUBLISH || stage.getFile().getTotalCount()==0) return;
        List<FileStageMetrics> stages=stage.getFile().getFileStageMetrics();
        stages.sort(FileStageMetrics.STAGE_ORDER_COMPARATOR);
        if(stages.get(0).getStageType()==ProgressStage.PUBLISH){
            stage.setServerFailureCount(stage.getFile().getTotalCount()- stage.getSuccessCount() );
            return ;
        }
        for(int i=1;i< stages.size();i++) {
            if(stages.get(i).getStageType()== ProgressStage.PUBLISH) {
                long serverFailureCount=stages.get(i-1).getSuccessCount() - stage.getSuccessCount();
                stage.setServerFailureCount(Math.max(0,serverFailureCount));
                break;
            }
        }
    }
}

