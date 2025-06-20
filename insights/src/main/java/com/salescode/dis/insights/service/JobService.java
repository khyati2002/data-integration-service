package com.salescode.dis.insights.service;

import com.salescode.dis.insights.dto.AccumulatedStageDataDto;
import com.salescode.dis.insights.dto.job.JobEntityResponseDtoWithStages;
import com.salescode.dis.insights.entity.FileStageMetrics;
import com.salescode.dis.insights.entity.JobEntity;
//import com.salescode.dis.insights.enums.JobStatus;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.enums.ProgressStatus;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.exception.ResourceNotFoundException;
import com.salescode.dis.insights.mapper.JobEntityMapper;
import com.salescode.dis.insights.repository.FileStageMetricsRepository;
import com.salescode.dis.insights.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

//    public JobEntity updateStatus(String id, JobStatus status) {
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
        //    jobEntity.setModeOfIntegration(modeOfIntegration);
            return saveJob(jobEntity);
        });
    }

    // Original method - kept for backward compatibility
    public Map<ModeOfIntegration,List<JobEntityResponseDtoWithStages>> groupByMode(List<JobEntity> jobs, Map<String, List<FileStageMetrics>> metricsByJobId){
        Map<ModeOfIntegration, List<JobEntityResponseDtoWithStages>> jobsByMode = new HashMap<>();

        for (JobEntity job : jobs) {
            List<FileStageMetrics> jobMetrics = metricsByJobId.getOrDefault(job.getId(), Collections.emptyList());

            Map<ModeOfIntegration, List<FileStageMetrics>> metricsByMode = jobMetrics.stream()
                    .collect(Collectors.groupingBy(FileStageMetrics::getModeOfIntegration));

            metricsByMode.forEach((mode, metricsForMode) -> {
                List<AccumulatedStageDataDto> stagesForMode = accumulateStageDataForMode(metricsForMode);

                List<String> mastersForMode = metricsForMode.stream()
                        .map(FileStageMetrics::getMaster)
                        .filter(Objects::nonNull)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());

                JobEntityResponseDtoWithStages dto = jobEntityMapper.toDtoWithStages(job);
                dto.setStages(stagesForMode);
                dto.setMasters(mastersForMode);

                jobsByMode.computeIfAbsent(mode, k -> new ArrayList<>()).add(dto);
            });
        }
        return jobsByMode;
    }

    // New method to group by master
    public Map<String, List<JobEntityResponseDtoWithStages>> groupByMaster(List<JobEntity> jobs, Map<String, List<FileStageMetrics>> metricsByJobId) {
        Map<String, List<JobEntityResponseDtoWithStages>> jobsByMaster = new HashMap<>();

        for (JobEntity job : jobs) {
            List<FileStageMetrics> jobMetrics = metricsByJobId.getOrDefault(job.getId(), Collections.emptyList());

            Map<String, List<FileStageMetrics>> metricsByMaster = jobMetrics.stream()
                    .filter(metric -> metric.getMaster() != null)
                    .collect(Collectors.groupingBy(FileStageMetrics::getMaster));

            metricsByMaster.forEach((master, metricsForMaster) -> {
                List<AccumulatedStageDataDto> stagesForMaster = accumulateStageDataForMode(metricsForMaster);

                // Get unique modes for this master
                List<ModeOfIntegration> modesForMaster = metricsForMaster.stream()
                        .map(FileStageMetrics::getModeOfIntegration)
                        .filter(Objects::nonNull)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());

                JobEntityResponseDtoWithStages dto = jobEntityMapper.toDtoWithStages(job);
                dto.setStages(stagesForMaster);
                dto.setMasters(Collections.singletonList(master)); // Single master for this grouping
                // You might want to add a setModes method to your DTO if you need to track modes per master

                jobsByMaster.computeIfAbsent(master, k -> new ArrayList<>()).add(dto);
            });
        }
        return jobsByMaster;
    }

    // Original method - returns only jobs grouped by mode
    public Map<ModeOfIntegration, List<JobEntityResponseDtoWithStages>> getAllJobsGroupedByModeForLob(String lob) {
        List<JobEntity> jobs = jobRepo.findByLob(lob);
        List<String> jobIds = jobs.stream()
                .map(JobEntity::getId)
                .toList();

        List<FileStageMetrics> allMetrics = fileStageMetricsRepository.findByJobIdIn(jobIds);
        Map<String, List<FileStageMetrics>> metricsByJobId = allMetrics.stream()
                .collect(Collectors.groupingBy(metrics -> metrics.getJob().getId()));

        return groupByMode(jobs, metricsByJobId);
    }

    // New method - returns array with both groupings
    public Object[] getAllJobsGroupedByModeAndMasterForLob(String lob) {
        List<JobEntity> jobs = jobRepo.findByLob(lob);
        List<String> jobIds = jobs.stream()
                .map(JobEntity::getId)
                .toList();

        List<FileStageMetrics> allMetrics = fileStageMetricsRepository.findByJobIdIn(jobIds);
        Map<String, List<FileStageMetrics>> metricsByJobId = allMetrics.stream()
                .collect(Collectors.groupingBy(metrics -> metrics.getJob().getId()));

        // Index 0: Jobs grouped by mode
        Map<ModeOfIntegration, List<JobEntityResponseDtoWithStages>> jobsByMode = groupByMode(jobs, metricsByJobId);

        // Index 1: Jobs grouped by master
        Map<String, List<JobEntityResponseDtoWithStages>> jobsByMaster = groupByMaster(jobs, metricsByJobId);

        return new Object[]{jobsByMode, jobsByMaster};
    }

    private List<AccumulatedStageDataDto> accumulateStageDataForMode(List<FileStageMetrics> jobMetrics) {
        Map<ProgressStage, List<FileStageMetrics>> groupedMetrics = jobMetrics.stream()
                .collect(Collectors.groupingBy(FileStageMetrics::getStageType));

        return groupedMetrics.entrySet().stream()
                .map(entry -> {
                    List<FileStageMetrics> stageMetrics = entry.getValue();
                    return aggregateStageMetrics(stageMetrics);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(AccumulatedStageDataDto::getStageType))
                .collect(Collectors.toList());
    }

    private AccumulatedStageDataDto aggregateStageMetrics(List<FileStageMetrics> stageMetrics) {
        if (stageMetrics.isEmpty()) {
            return null;
        }
        FileStageMetrics first = stageMetrics.getFirst();

        Long totalSuccessCount = stageMetrics.stream()
                .mapToLong(FileStageMetrics::getSuccessCount)
                .sum();

        long totalFailureCount = stageMetrics.stream()
                .mapToLong(metric -> metric.getLogicalFailureCount() + metric.getServerFailureCount())
                .sum();

        return new AccumulatedStageDataDto(first.getStageType(), first.getModeOfIntegration(),
                totalSuccessCount, totalFailureCount);
    }
}