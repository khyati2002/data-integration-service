package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.StageDto;
import com.salescode.dis.insights.service.StageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/stages")
public class StageController {

    private final StageService stageService;

    @GetMapping()
    public ResponseEntity<List<StageDto>> getAllStages() {

        try {
            List<StageDto> stages = stageService.getStages();
            return ResponseEntity.ok(stages);
        } catch (Exception e) {
            log.error("Error fetching stages data", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}