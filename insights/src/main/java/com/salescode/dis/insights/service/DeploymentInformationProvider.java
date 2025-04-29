package com.salescode.dis.insights.service;

import com.salescode.dis.insights.dto.DeploymentInformation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DeploymentInformationProvider {

    private final DeploymentInformation deploymentInformation;

    public DeploymentInformationProvider() {
        Map<String, String> properties = readGitProperties();
        this.deploymentInformation = new DeploymentInformation("UP", properties);
    }

    public DeploymentInformation getDeploymentInformation() {
        return deploymentInformation;
    }


    private Map<String, String> readGitProperties() {
        Properties properties = new Properties();
        try (var in = DeploymentInformationProvider.class.getClassLoader().getResourceAsStream("git.properties")) {
            properties.load(in);
        } catch (Exception e) {
            log.error("Could not get build information:{}", e.getMessage());
        }
        return properties.entrySet()
                .stream()
                .collect(Collectors.toUnmodifiableMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
    }
}