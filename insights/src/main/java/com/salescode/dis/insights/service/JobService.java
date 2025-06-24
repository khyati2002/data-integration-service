package com.salescode.dis.insights.service;

import com.salescode.dis.insights.dto.job.JobStageAccumulatedData;
import com.salescode.dis.insights.dto.file.stage.AccumulatedStageDataDto;
import com.salescode.dis.insights.dto.job.JobEntityResponseDtoWithStages;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStatus;
import com.salescode.dis.insights.exception.ResourceNotFoundException;
import com.salescode.dis.insights.mapper.JobEntityMapper;
import com.salescode.dis.insights.repository.FileStageMetricsRepository;
import com.salescode.dis.insights.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class JobService {

    private final JobRepository jobRepo;
    private final JobEntityMapper jobEntityMapper;
    private final FileStageMetricsRepository fileStageMetricsRepository;

    public int countJobsByLobAndStatus(String lob, ProgressStatus status) {
        return jobRepo.countByLobAndStatus(lob, status);
    }

    public JobEntity saveJob(JobEntity req) {
        return jobRepo.save(req);
    }

    @Transactional(readOnly = true)
    public JobEntity getJob(String id) {
        return jobRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
    }

//    public JobEntity updateStatus(String id, ProgressStatus status) {
//        JobEntity job = getJob(id);
//        job.setStatus(status);
//        log.info("Job {} status -> {}", id, status);
//        return job;
//    }
    

    @Transactional(readOnly = true)
    public Page<JobEntity> getAllJobsByLob(String lob, Pageable pageable) {
        return jobRepo.getJobEntitiesByLob(lob, pageable);
    }


    public JobEntity createJobIfNotExists(String jobId, String lob, ModeOfIntegration modeOfIntegration) {
        Optional<JobEntity> job = jobRepo.findById(jobId);
        return job.orElseGet(()->{
            JobEntity jobEntity = new JobEntity();
            jobEntity.setId(jobId);
            jobEntity.setLob(lob);
            return saveJob(jobEntity);
        });
    }

    public List<JobEntityResponseDtoWithStages> getJobsWithAggregatedStages(String lob, LocalDateTime startDate, LocalDateTime endDate) {
        Instant startInstant = startDate.atZone(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.atZone(ZoneId.systemDefault()).toInstant();
        List<JobStageAccumulatedData> queryResults = fileStageMetricsRepository.findJobsWithAggregatedStagesByLob(lob, startInstant, endInstant);

        Map<String, List<JobStageAccumulatedData>> resultsByJobId = queryResults.stream()
                .collect(Collectors.groupingBy(JobStageAccumulatedData::getJobId));

        return resultsByJobId.entrySet().stream()
                .map(entry -> {
                    List<JobStageAccumulatedData> jobResults = entry.getValue();
                    JobStageAccumulatedData firstResult = jobResults.get(0); // Job-level data from any projection

                    List<AccumulatedStageDataDto> stages = jobResults.stream()
                            .filter(result -> result.getStageType() != null)
                            .map(result -> AccumulatedStageDataDto.builder()
                                    .stageType((result.getStageType()))
                                    .totalSuccessCount(result.getTotalSuccessCount() != null ? result.getTotalSuccessCount() : 0L)
                                    .totalFailureCount((result.getServerFailureCount() != null ? result.getServerFailureCount() : 0L) + (result.getLogicalFailureCount()!=null ? result.getLogicalFailureCount() : 0L))
                                    .build())
                            .collect(Collectors.toList());

                    return JobEntityResponseDtoWithStages.builder()
                            .id(firstResult.getJobId())
                            .creationTime(firstResult.getCreationTime())
                            .lastModifiedTime(firstResult.getLastModifiedTime())
                            .lob(firstResult.getLob())
                            .extendedAttributes(firstResult.getExtendedAttributes())
                            .startTime(firstResult.getStartTime())
                            .endTime(firstResult.getEndTime())
                            .status(firstResult.getStatus())
                            .publisherJobUri(firstResult.getPublisherJobUri())
                            .consumerJobUri(firstResult.getConsumerJobUri())
                            .stages(stages)
                            .build();
                })
                .collect(Collectors.toList());
    }



}