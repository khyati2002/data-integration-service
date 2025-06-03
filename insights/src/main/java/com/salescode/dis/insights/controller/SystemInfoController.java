package com.salescode.dis.insights.controller;

import com.salescode.dis.insights.dto.DeploymentInformation;
import com.salescode.dis.insights.service.SystemInfoService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class SystemInfoController {

    private final SystemInfoService systemInfoService;

    @GetMapping("/hckeck")
    public DeploymentInformation getBuildInfo() {
        return systemInfoService.getDeploymentInfo();
    }
}