package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.LobSummaryDto;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.repository.FileStageMetricsRepository;
import com.salescode.dis.insights.service.JobService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lob-summary")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Job Management", description = "APIs for managing integration lobs")
public class LobController {

    private final JobService jobService;

    private final FileStageMetricsRepository fileStageMetricsRepository;

    @GetMapping()
    public ResponseEntity<List<LobSummaryDto>> getLobSummary(
            @RequestParam(required = false) List<String> lobs,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        LocalDateTime queryEndTime = (endDate != null) ? endDate : LocalDateTime.now();
        LocalDateTime queryStartTime = (startDate != null) ? startDate : queryEndTime.minusDays(10);

        Instant startInstant = queryStartTime.atZone(ZoneId.systemDefault()).toInstant();
        Instant endInstant = queryEndTime.atZone(ZoneId.systemDefault()).toInstant();

        if (lobs == null || lobs.isEmpty()) {
            LobSummaryDto defaultValue = new LobSummaryDto(null, 0.0, BigDecimal.ZERO, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
            return ResponseEntity.ok(List.of(defaultValue));
        }

        List<LobSummaryDto> results = fileStageMetricsRepository.findAggregateMetricsAndJobCountsByLobAndDateRange(
                lobs, ProgressStage.QUEUE, ProgressStage.SAVE, startInstant, endInstant);

        return ResponseEntity.ok(results);
    }

}
