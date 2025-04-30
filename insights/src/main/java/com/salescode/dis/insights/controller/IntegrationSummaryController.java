package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.LobSummaryDTO;
import com.salescode.dis.insights.service.IntegrationSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class IntegrationSummaryController {

    private final IntegrationSummaryService integrationSummaryService;

    @Autowired
    public IntegrationSummaryController(IntegrationSummaryService integrationSummaryService) {
        this.integrationSummaryService = integrationSummaryService;
    }

    @GetMapping("/integration-summary")
    public ResponseEntity<List<LobSummaryDTO>> getIntegrationSummary(
            @RequestParam(required = false) List<String> lobs,
            @RequestParam(required = false) Map<String, String> commonFilters,
            @RequestParam Map<String, String> allRequestParams
    ) {
        Map<String, String> jobFilters = new HashMap<>();
        Map<String, String> fileFilters = new HashMap<>();

        // Process common filters for jobs and files
        if (commonFilters != null) {
            for (Map.Entry<String, String> entry : commonFilters.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key.startsWith("job.")) {
                    // Apply the filter to job
                    jobFilters.put(key.substring(4), value); // Remove 'job.' prefix
                } else if (key.startsWith("file.")) {
                    // Apply the filter to file
                    fileFilters.put(key.substring(5), value); // Remove 'file.' prefix
                }
            }
        }

        // Handle specific filters (e.g., lobs)
        for (Map.Entry<String, String> entry : allRequestParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if ("lobs".equals(key)) continue; // already handled separately

            if (isJobField(key)) {
                jobFilters.put(key, value);
            } else if (isFileField(key)) {
                fileFilters.put(key, value);
            }
        }

        List<LobSummaryDTO> summary = integrationSummaryService.getIntegrationSummary(lobs, jobFilters, fileFilters);
        return ResponseEntity.ok(summary);
    }

    private boolean isJobField(String fieldName) {
        return hasField(com.salescode.dis.insights.entity.JobEntity.class, fieldName);
    }

    private boolean isFileField(String fieldName) {
        return hasField(com.salescode.dis.insights.entity.FileEntity.class, fieldName);
    }

    private boolean hasField(Class<?> clazz, String fieldName) {
        try {
            clazz.getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}
