package com.salescode.dis.insights.eventListener;

import com.salescode.dis.insights.dto.FileUpdateRequestDto;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.events.FileUpdateEvent;
import com.salescode.dis.insights.service.FileService;
import com.salescode.dis.insights.service.JobService;
import com.salescode.dis.insights.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.kafka.annotation.KafkaListener;

@Service
public class FileUpdateEventListener {
    private final FileService fileService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private JobService jobService;

    @Autowired
    public FileUpdateEventListener(FileService fileService) {
        this.fileService = fileService;
    }

    @KafkaListener(topics = "file-updates", groupId = "file-update-processor")
    public void consumeFileUpdateEvent(FileUpdateEvent event) {

        FileUpdateRequestDto req = event.getUpdateRequest();
        String fileId = event.getFileId();
        if(event.getFileId() == null) {
             fileId = redisService.getFileIdAndRefreshTtl(event.getLob(), event.getMasterName());

            if (fileId == null) {
                if (jobService.getJob(req.getJobId()) == null) {
                    JobEntity entity = new JobEntity();
                    entity.setId(req.getJobId());
                    entity.setLob(event.getLob());
                    entity.setMaster(event.getMasterName());
                    JobEntity saved = jobService.createJob(entity);
                    req.setJobId(saved.getId());
                }

                FileEntity fileEntity = new FileEntity();
                fileEntity.setLob(event.getLob());
                FileEntity savedFileEntity = fileService.register(req.getJobId(), fileEntity);
                fileId = savedFileEntity.getId();
                redisService.saveFileId(event.getLob(),event.getMasterName(), fileId, 10);
            }
        }

        if (req.isProgressUpdate()) {
            fileService.updateProgress(fileId, req.getProgress());
        }
        if (req.isStatusUpdate()) {
            fileService.updateStatus(fileId, req.getStatus().getConsumedStatus(),
                    req.getStatus().getPublishedStatus());
        }
    }
}