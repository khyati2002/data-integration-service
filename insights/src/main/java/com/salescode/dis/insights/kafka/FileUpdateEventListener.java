package com.salescode.dis.insights.kafka;


import com.salescode.dis.insights.dto.FileUpdateRequestDto;
import com.salescode.dis.insights.dto.FileProgressRequest;

import com.salescode.dis.insights.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@EnableKafka
public class FileUpdateEventListener {

    private final FileService fileService;

    @Autowired
    public FileUpdateEventListener(FileService fileService) {
        this.fileService = fileService;
    }

    @KafkaListener(topics = "file-updates", groupId = "file-update-processor",containerFactory = "kafkaListenerContainerFactory")
    public void consumeFileUpdateEvents(List<FileUpdateEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        Map<String, FileUpdateEvent> aggregatedUpdatesProgress = new HashMap<>();
        for (FileUpdateEvent event : events) {
            String fileId = event.getFileId();
            FileUpdateRequestDto req = event.getUpdateRequest();

            FileUpdateEvent agg = aggregatedUpdatesProgress.get(fileId);
            if (agg == null) {
                agg = new FileUpdateEvent();

                FileUpdateRequestDto updateRequest = new FileUpdateRequestDto();
                FileProgressRequest progressRequest = new FileProgressRequest();

                // Set all counts to 0
                progressRequest.setConsumerSuccessCount(0);
                progressRequest.setConsumerFailCount(0);
                progressRequest.setPublishedSuccessCount(0);
                progressRequest.setPublishedFailCount(0);
                progressRequest.setServerFailCount(0);
                progressRequest.setLogicalFailCount(0);
                updateRequest.setProgress(progressRequest);

                agg.setUpdateRequest(updateRequest);

                aggregatedUpdatesProgress.put(fileId, agg);
            }

            if (req.getProgress() != null) {
                  if(req.getProgress().getConsumerSuccessCount() != null) {
                      agg.getUpdateRequest().getProgress().setConsumerSuccessCount(
                              agg.getUpdateRequest().getProgress().getConsumerSuccessCount() + req.getProgress()
                                      .getConsumerSuccessCount()
                      );
                  }
                if(req.getProgress().getConsumerFailCount() != null) {
                    agg.getUpdateRequest().getProgress().setConsumerFailCount(
                            agg.getUpdateRequest().getProgress().getConsumerFailCount() + req.getProgress()
                                    .getConsumerFailCount()
                    );
                }
                if(req.getProgress().getPublishedSuccessCount() != null) {
                    agg.getUpdateRequest().getProgress().setPublishedSuccessCount(
                            agg.getUpdateRequest().getProgress().getPublishedSuccessCount() + req.getProgress()
                                    .getPublishedSuccessCount()
                    );
                }
                if(req.getProgress().getPublishedFailCount()!= null) {
                    agg.getUpdateRequest().getProgress().setPublishedFailCount(
                            agg.getUpdateRequest().getProgress().getPublishedFailCount() + req.getProgress()
                                    .getPublishedFailCount()
                    );
                }
                if(req.getProgress().getServerFailCount() != null) {
                    agg.getUpdateRequest().getProgress().setServerFailCount(
                            agg.getUpdateRequest().getProgress().getServerFailCount() + req.getProgress()
                                    .getServerFailCount()
                    );
                }
                if(req.getProgress().getLogicalFailCount() != null) {
                    agg.getUpdateRequest().getProgress().setLogicalFailCount(
                            agg.getUpdateRequest().getProgress().getLogicalFailCount() + req.getProgress()
                                    .getLogicalFailCount()
                    );
                }

            }
            agg.setFileId(fileId);
            agg.setLob(event.getLob());
            agg.getUpdateRequest().setJobId(req.getJobId());
            agg.setMasterName(event.getMasterName());

            aggregatedUpdatesProgress.put(fileId, agg);
        }

        for (Map.Entry<String, FileUpdateEvent> entry : aggregatedUpdatesProgress.entrySet()) {
            String fileId = entry.getKey();
            FileUpdateEvent agg = entry.getValue();

            fileService.updateProgress(fileId, agg.getUpdateRequest().getProgress(), agg.getUpdateRequest().getJobId(), agg.getLob(), agg.getMasterName());
        }

        for(FileUpdateEvent event : events) {
            if(event.getUpdateRequest().getStatus() != null) {
                fileService.updateStatus(event.getFileId(),event.getUpdateRequest().getStatus().getConsumedStatus(), event.getUpdateRequest().getStatus().getPublishedStatus());
            }
        }
    }
}