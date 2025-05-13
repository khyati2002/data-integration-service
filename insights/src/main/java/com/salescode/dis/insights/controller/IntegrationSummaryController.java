package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.LobSummaryDTO;
import com.salescode.dis.insights.service.IntegrationSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
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

//    @GetMapping("/integration-summary")
//    public ResponseEntity<List<LobSummaryDTO>> getIntegrationSummary(
//            @RequestParam(required = false) List<String> lobs,
//            @RequestParam(required = false) Map<String, String> commonFilters,
//            @RequestParam Map<String, String> allRequestParams
//    ) {
//        Map<String, String> jobFilters = new HashMap<>();
//        Map<String, String> fileFilters = new HashMap<>();
//
//        // Process common filters for jobs and files
//        if (commonFilters != null) {
//            for (Map.Entry<String, String> entry : commonFilters.entrySet()) {
//                String key = entry.getKey();
//                String value = entry.getValue();
//
//                if (key.startsWith("job.")) {
//                    // Apply the filter to job
//                    jobFilters.put(key.substring(4), value); // Remove 'job.' prefix
//                } else if (key.startsWith("file.")) {
//                    // Apply the filter to file
//                    fileFilters.put(key.substring(5), value); // Remove 'file.' prefix
//                }
//            }
//        }
//
//        // Handle specific filters (e.g., lobs)
//        for (Map.Entry<String, String> entry : allRequestParams.entrySet()) {
//            String key = entry.getKey();
//            String value = entry.getValue();
//
//            if ("lobs".equals(key)) continue; // already handled separately
//
//            if (isJobField(key)) {
//                jobFilters.put(key, value);
//            } else if (isFileField(key)) {
//                fileFilters.put(key, value);
//            }
//        }
//
//        List<LobSummaryDTO> summary = integrationSummaryService.getIntegrationSummary(lobs, jobFilters, fileFilters);
//        return ResponseEntity.ok(summary);
//    }

    @GetMapping("/lob-summary")
    public ResponseEntity<Object> getLobSummary(
            @RequestParam(required = false) List<String> lob,@RequestParam(required = false) List<String> status,@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        if (lob != null && lob.size() == 1) {
            lob = Collections.singletonList(lob.get(0));
        }
        if(status!=null && status.size()==1){
            status = Collections.singletonList(status.get(0));
        }
        return ResponseEntity.ok(integrationSummaryService.getLobSummary(lob,status,startTime,endTime));
    }

    @GetMapping("/lob-summary-only")
    public ResponseEntity<Object> getLobSummaryOnly(
            @RequestParam(required = false) List<String> lob) {
        // If lob is null or contains one element, we convert it to a list
        if (lob != null && lob.size() == 1) {
            lob = Collections.singletonList(lob.get(0));
        }
        return ResponseEntity.ok(integrationSummaryService.getOnlyLobDetails(lob));
    }


    private Map<String, String> extractJobFilters(Map<String, String> allParams) {
        Map<String, String> jobFilters = new HashMap<>();
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (isJobField(key)) {
                jobFilters.put(key, value);
            }
        }
        return jobFilters;
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
