package com.salescode.dis.insights.eventListener;

import com.salescode.dis.insights.dto.FileUpdateRequestDto;
import com.salescode.dis.insights.events.FileUpdateEvent;
import com.salescode.dis.insights.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.kafka.annotation.KafkaListener;

@Service
public class FileUpdateEventListener {
    private final FileService fileService;

    @Autowired
    public FileUpdateEventListener(FileService fileService) {
        this.fileService = fileService;
    }

    @KafkaListener(topics = "file-updates", groupId = "file-update-processor")
    public void consumeFileUpdateEvent(FileUpdateEvent event) {
        FileUpdateRequestDto req = event.getUpdateRequest();
        String fileId = event.getFileId();

        if (req.isProgressUpdate()) {
            fileService.updateProgress(fileId, req.getProgress());
        }
        if (req.isStatusUpdate()) {
            fileService.updateStatus(fileId, req.getStatus().getConsumedStatus(),
                    req.getStatus().getPublishedStatus());
        }
    }
}