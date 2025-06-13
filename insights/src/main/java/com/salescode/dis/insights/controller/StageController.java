package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.StageInfo;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.service.StageRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stages")
@RequiredArgsConstructor
public class StageController {
    private final StageRegistry stageRegistry;

    @GetMapping("/modes")
    public ResponseEntity<List<ModeOfIntegration>> getSupportedModes() {
        return ResponseEntity.ok(stageRegistry.getSupportedModes());
    }

    @GetMapping
    public ResponseEntity<Map<ModeOfIntegration, List<StageInfo>>> getAllStages() {
        return ResponseEntity.ok(stageRegistry.getAllStages());
    }
} 