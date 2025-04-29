package com.salescode.dis.insights.dto;

import lombok.Value;

import java.util.Map;

@Value
public class DeploymentInformation {

    String status;

    String version;

    String buildTime;

    CommitInfo lastCommitInfo;


    public DeploymentInformation(String status, Map<String, String> deploymentInfo) {
        this.status = status;
        this.buildTime = deploymentInfo.get("git.build.time");
        version = deploymentInfo.get("git.build.version");
        lastCommitInfo = new CommitInfo(deploymentInfo);
    }


    @Value
    public static class CommitInfo {

        String time;

        String commitId;

        String message;

        String branch;

        String tag;

        public CommitInfo(Map<String, String> deploymentInfo) {
            time = deploymentInfo.get("git.commit.time");
            message = deploymentInfo.get("git.commit.message.short");
            commitId = deploymentInfo.get("git.commit.id.full");
            tag = deploymentInfo.get("git.closest.tag.name");
            branch = deploymentInfo.get("git.branch");

        }
    }
}