package com.salescode.dis.insights.service;

import com.salescode.dis.insights.dto.DeploymentInformation;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SystemInfoService {
    private final DeploymentInformationProvider deploymentInformationProvider;
    public DeploymentInformation getDeploymentInfo() {
        return deploymentInformationProvider.getDeploymentInformation();
    }
}