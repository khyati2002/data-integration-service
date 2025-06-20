package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.job.JobEntityResponseDto;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.enums.ProgressStatus;
import com.salescode.dis.insights.repository.FileStageMetricsRepository;
import com.salescode.dis.insights.service.JobService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/{lob}/lob-summary")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Job Management", description = "APIs for managing integration lobs")
public class LobController {

    private final JobService jobService;

    private final FileStageMetricsRepository fileStageMetricsRepository;
    @GetMapping()
    public ResponseEntity<Map<String, Object>> getLobSummary(@PathVariable String lob) {
        int pendingCount = jobService.countJobsByLobAndStatus(lob, ProgressStatus.PENDING);
        int completedCount = jobService.countJobsByLobAndStatus(lob, ProgressStatus.COMPLETED_SUCCESSFULLY) + jobService.countJobsByLobAndStatus(lob,ProgressStatus.COMPLETED_UNSUCCESSFULLY);
        int failedCount = jobService.countJobsByLobAndStatus(lob, ProgressStatus.FAILED);
        List<Object[]> results = fileStageMetricsRepository.findAggregateMetricsByLob(lob, ProgressStage.QUEUE,ProgressStage.SAVE);
        Double avg = 0.0;
        BigDecimal max = BigDecimal.valueOf(0);
        Map<String, Object> response = new HashMap<>();
        if (!results.isEmpty()) {
            Object[] row = results.get(0);
            avg = (Double) row[0];
            max = (BigDecimal) row[1];
            long queueSuccess = row[2] != null ? (Long) row[2] : 0;
            long queueFailed = row[3] != null ? (Long) row[3] : 0;
            long saveSuccess = row[4] != null ? (Long) row[4] : 0;
            long saveFailed = row[5] != null ? (Long) row[5] : 0;
            response.put("queueSuccess", queueSuccess);
            response.put("queueFailed", queueFailed);
            response.put("saveSuccess", saveSuccess);
            response.put("saveFailed", saveFailed);
        }
        response.put("name",lob);
        response.put("PENDING", pendingCount);
        response.put("COMPLETED", completedCount);
        response.put("FAILED", failedCount);
        response.put("avgThroughput", avg);
        response.put("maxThroughput", max);


        return ResponseEntity.ok(response);
    }

}
