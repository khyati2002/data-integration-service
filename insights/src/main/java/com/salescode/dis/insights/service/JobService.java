package com.salescode.dis.insights.service;

import com.salescode.dis.insights.dto.AccumulatedJobsAndMasterDto;
import com.salescode.dis.insights.dto.MasterCard;
import com.salescode.dis.insights.dto.MetadataEntry;
import com.salescode.dis.insights.dto.job.JobStageAccumulatedData;
import com.salescode.dis.insights.dto.file.stage.AccumulatedStageDataDto;
import com.salescode.dis.insights.dto.job.JobEntityResponseDtoWithStages;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.enums.ModeOfIntegration;
import com.salescode.dis.insights.enums.ProgressStage;
import com.salescode.dis.insights.enums.ProgressStatus;
import com.salescode.dis.insights.exception.ResourceNotFoundException;
import com.salescode.dis.insights.mapper.JobEntityMapper;
import com.salescode.dis.insights.repository.FileStageMetricsRepository;
import com.salescode.dis.insights.repository.InsightsMetadataRepository;
import com.salescode.dis.insights.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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
    private final InsightsMetadataRepository metadataRepository;
    private final JobEntityMapper jobEntityMapper;
    private final FileStageMetricsRepository fileStageMetricsRepository;

    @Autowired
    private ApplicationContext applicationContext;

    public int countJobsByLobAndStatus(String lob, ProgressStatus status) {
        return jobRepo.countByLobAndStatus(lob, status);
    }

    public JobEntity saveJob(JobEntity req) {
        JobEntity jobEntity = jobRepo.save(req);
        if(req.getStatus() == ProgressStatus.COMPLETED_SUCCESSFULLY) {
            FileService fileService = applicationContext.getBean(FileService.class);
            FileEntity file = new FileEntity();
            file.setModeOfIntegration(ModeOfIntegration.CK_WORKFLOW_JOB);
            file.setLob(req.getLob());
            file.setJob(jobEntity);
            file.setMaster("undefined");
            fileService.createFile(req.getId(),file);
        }
        return jobEntity;
    }

    @Transactional(readOnly = true)
    public JobEntity getJob(String id) {
        return jobRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
    }

    public JobEntity updateStatus(String id, ProgressStatus status) {
        JobEntity job = getJob(id);
        job.setStatus(status);
        log.info("Job {} status -> {}", id, status);
        return job;
    }

    public void recalcStatus(JobEntity job) {
        if (job.getFiles() == null) return;

        Map<ProgressStatus, Long> statusCountMap = job.getFiles().stream()
                .collect(Collectors.groupingBy(FileEntity::getStatus, Collectors.counting()));

        if(statusCountMap.containsKey(ProgressStatus.PENDING)) job.setStatus(ProgressStatus.PENDING);
        else if(statusCountMap.containsKey(ProgressStatus.FAILED)) job.setStatus(ProgressStatus.FAILED);
        else if(statusCountMap.containsKey(ProgressStatus.COMPLETED_UNSUCCESSFULLY)) job.setStatus(ProgressStatus.COMPLETED_UNSUCCESSFULLY);
        else{
            job.setStatus(ProgressStatus.COMPLETED_SUCCESSFULLY);
        }
    }

    @Transactional(readOnly = true)
    public Page<JobEntity> getAllJobsByLob(String lob, Pageable pageable) {
        return jobRepo.getJobEntitiesByLob(lob, pageable);
    }


    public JobEntity createJobIfNotExists(String jobId, String lob, ModeOfIntegration modeOfIntegration) {
        Optional<JobEntity> job = jobRepo.findByIdAndLob(jobId,lob);
        return job.orElseGet(()->{
            JobEntity jobEntity = new JobEntity();
            jobEntity.setId(jobId);
            jobEntity.setLob(lob);
            return saveJob(jobEntity);
        });
    }

    public List<JobEntityResponseDtoWithStages> getJobsWithAggregatedStages(String lob, LocalDateTime startDate, LocalDateTime endDate, String mode) {
        Instant startInstant = startDate.atZone(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.atZone(ZoneId.systemDefault()).toInstant();
        List<JobStageAccumulatedData> queryResults = new ArrayList<>();
        if(mode != null) {
             ModeOfIntegration modeOfIntegration = ModeOfIntegration.valueOf(mode);
             queryResults = fileStageMetricsRepository.findJobsWithAggregatedStagesByLobAndMode(lob, startInstant, endInstant,modeOfIntegration);
        }
        else{
            queryResults = fileStageMetricsRepository.findJobsWithAggregatedStagesByLob(lob, startInstant, endInstant);
        }
        Map<String, List<JobStageAccumulatedData>> resultsByJobId = queryResults.stream()
                .collect(Collectors.groupingBy(JobStageAccumulatedData::getJobId));

        return resultsByJobId.entrySet().stream()
                .map(entry -> {
                    List<JobStageAccumulatedData> jobResults = entry.getValue();
                    JobStageAccumulatedData firstResult = jobResults.get(0);

                    List<String> uniqueMastersForJob = jobResults.stream()
                            .map(JobStageAccumulatedData::getMaster) // Get the 'master' for each stage
                            .filter(master -> master != null && !master.trim().isEmpty()) // Filter out null or empty masters
                            .distinct() // Ensure uniqueness (similar to collecting to a Set and then to a List)
                            .collect(Collectors.toList()); // Collect into a List

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
                            .masters(uniqueMastersForJob)
                            .modeOfIntegration(firstResult.getModeOfIntegration())
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



//    public AccumulatedJobsAndMasterDto getJobsWithAggregatedStagesAndMasters(String lob, LocalDateTime startDate, LocalDateTime endDate, String mode) {
//        Instant startInstant = startDate.atZone(ZoneId.systemDefault()).toInstant();
//        Instant endInstant = endDate.atZone(ZoneId.systemDefault()).toInstant();
//        List<JobStageAccumulatedData> queryResults = new ArrayList<>();
//
//        if(mode != null) {
//            ModeOfIntegration modeOfIntegration = ModeOfIntegration.valueOf(mode);
//            queryResults = fileStageMetricsRepository.findJobsWithAggregatedStagesByLobAndMode(lob, startInstant, endInstant, modeOfIntegration);
//        } else {
//            queryResults = fileStageMetricsRepository.findJobsWithAggregatedStagesByLob(lob, startInstant, endInstant);
//        }
//
//        Map<String, List<JobStageAccumulatedData>> resultsByJobId = queryResults.stream()
//                .collect(Collectors.groupingBy(JobStageAccumulatedData::getJobId));
//
//        List<JobEntityResponseDtoWithStages> results = resultsByJobId.entrySet().stream()
//                .map(entry -> {
//                    List<JobStageAccumulatedData> jobResults = entry.getValue();
//                    JobStageAccumulatedData firstResult = jobResults.get(0);
//
//                    // Get unique masters for this job
//                    List<String> uniqueMastersForJob = jobResults.stream()
//                            .map(JobStageAccumulatedData::getMaster)
//                            .filter(master -> master != null && !master.trim().isEmpty())
//                            .distinct()
//                            .collect(Collectors.toList());
//
//                    // Group by stage type and aggregate across all masters for this job
//                    Map<ProgressStage, List<JobStageAccumulatedData>> stageGroups = jobResults.stream()
//                            .filter(result -> result.getStageType() != null)
//                            .collect(Collectors.groupingBy(JobStageAccumulatedData::getStageType));
//
//                    List<AccumulatedStageDataDto> stages = new ArrayList<>();
//                    if(!stageGroups.isEmpty()){
//                    stages = stageGroups.entrySet().stream()
//                            .map(stageEntry -> {
//                                List<JobStageAccumulatedData> stageResults = stageEntry.getValue();
//
//                                // Sum up all counts for this stage type across all masters
//                                Long totalSuccessCount = stageResults.stream()
//                                        .mapToLong(result -> result.getTotalSuccessCount() != null ? result.getTotalSuccessCount() : 0L)
//                                        .sum();
//
//                                Long totalServerFailureCount = stageResults.stream()
//                                        .mapToLong(result -> result.getServerFailureCount() != null ? result.getServerFailureCount() : 0L)
//                                        .sum();
//
//                                Long totalLogicalFailureCount = stageResults.stream()
//                                        .mapToLong(result -> result.getLogicalFailureCount() != null ? result.getLogicalFailureCount() : 0L)
//                                        .sum();
//
//                                return AccumulatedStageDataDto.builder()
//                                        .stageType(stageEntry.getKey())
//                                        .totalSuccessCount(totalSuccessCount)
//                                        .totalFailureCount(totalServerFailureCount + totalLogicalFailureCount)
//                                        .build();
//                            })
//                            .collect(Collectors.toList());
//                    }
//
//                    return JobEntityResponseDtoWithStages.builder()
//                            .id(firstResult.getJobId())
//                            .masters(uniqueMastersForJob)
//                            .modeOfIntegration(firstResult.getModeOfIntegration())
//                            .creationTime(firstResult.getCreationTime())
//                            .lastModifiedTime(firstResult.getLastModifiedTime())
//                            .lob(firstResult.getLob())
//                            .extendedAttributes(firstResult.getExtendedAttributes())
//                            .startTime(firstResult.getStartTime())
//                            .endTime(firstResult.getEndTime())
//                            .status(firstResult.getStatus())
//                            .publisherJobUri(firstResult.getPublisherJobUri())
//                            .consumerJobUri(firstResult.getConsumerJobUri())
//                            .stages(stages)
//                            .build();
//                })
//                .collect(Collectors.toList());
//
//        // Master processing remains the same
//        Map<String, List<JobStageAccumulatedData>> resultsByMaster = queryResults.stream()
//                .collect(Collectors.groupingBy(JobStageAccumulatedData::getMaster));
//
//        List<MasterCard> resultsMaster = resultsByMaster.entrySet().stream()
//                .map(entry -> {
//                    List<JobStageAccumulatedData> jobResults = entry.getValue();
//
//                    Map<ProgressStatus, Long> statusCounts = jobResults.stream()
//                            .collect(Collectors.groupingBy(
//                                    JobStageAccumulatedData::getStatus,
//                                    Collectors.mapping(
//                                            JobStageAccumulatedData::getJobId,
//                                            Collectors.collectingAndThen(
//                                                    Collectors.toSet(),
//                                                    set -> (long) set.size()
//                                            )
//                                    )
//                            ));
//
//                    Long completed_success = statusCounts.get(ProgressStatus.COMPLETED_SUCCESSFULLY) != null ? statusCounts.get(ProgressStatus.COMPLETED_SUCCESSFULLY) : 0L;
//                    Long completed_unsuccess = statusCounts.get(ProgressStatus.COMPLETED_UNSUCCESSFULLY) != null ? statusCounts.get(ProgressStatus.COMPLETED_UNSUCCESSFULLY) : 0L;
//                    Long pending = statusCounts.get(ProgressStatus.PENDING) != null ? statusCounts.get(ProgressStatus.PENDING) : 0L;
//                    Long failed = statusCounts.get(ProgressStatus.FAILED) != null ? statusCounts.get(ProgressStatus.FAILED) : 0L;
//                    Long completed = completed_success + completed_unsuccess;
//
//                    List<AccumulatedStageDataDto> stages = jobResults.stream()
//                            .filter(result -> result.getStageType() != null)
//                            .map(result -> AccumulatedStageDataDto.builder()
//                                    .stageType((result.getStageType()))
//                                    .totalSuccessCount(result.getTotalSuccessCount() != null ? result.getTotalSuccessCount() : 0L)
//                                    .totalFailureCount((result.getServerFailureCount() != null ? result.getServerFailureCount() : 0L) + (result.getLogicalFailureCount() != null ? result.getLogicalFailureCount() : 0L))
//                                    .build())
//                            .collect(Collectors.toList());
//
//                    Long queueCount = stages.stream()
//                            .filter(stage -> stage.getStageType() == ProgressStage.QUEUE)
//                            .findFirst()
//                            .map(queueStage -> queueStage.getTotalSuccessCount() + queueStage.getTotalFailureCount())
//                            .orElse(0L);
//
//                    Long saveCount = stages.stream()
//                            .filter(stage -> stage.getStageType() == ProgressStage.SAVE)
//                            .findFirst()
//                            .map(saveStage -> saveStage.getTotalSuccessCount() + saveStage.getTotalFailureCount())
//                            .orElse(0L);
//
//                    return MasterCard.builder()
//                            .queueCount(queueCount)
//                            .saveCount(saveCount)
//                            .completedJobCount(completed)
//                            .pendingJobCount(pending)
//                            .failedJobCount(failed)
//                            .build();
//                })
//                .collect(Collectors.toList());
//
//        AccumulatedJobsAndMasterDto dto = new AccumulatedJobsAndMasterDto();
//        dto.setJobs(results);
//        dto.setMasters(resultsMaster);
//        return dto;
//    }

    public AccumulatedJobsAndMasterDto getJobsWithAggregatedStagesAndMasters(String lob, LocalDateTime startDate, LocalDateTime endDate, String mode) {
        Instant startInstant = startDate.atZone(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.atZone(ZoneId.systemDefault()).toInstant();
        List<JobStageAccumulatedData> queryResults = new ArrayList<>();

        if(mode != null) {
            ModeOfIntegration modeOfIntegration = ModeOfIntegration.valueOf(mode);
            queryResults = fileStageMetricsRepository.findJobsWithAggregatedStagesByLobAndMode(lob, startInstant, endInstant, modeOfIntegration);
        } else {
            queryResults = fileStageMetricsRepository.findJobsWithAggregatedStagesByLob(lob, startInstant, endInstant);
        }

        // Filter out records with null jobId before grouping
        Map<String, List<JobStageAccumulatedData>> resultsByJobId = queryResults.stream()
                .filter(data -> data.getJobId() != null && !data.getJobId().trim().isEmpty())
                .collect(Collectors.groupingBy(JobStageAccumulatedData::getJobId));

        List<JobEntityResponseDtoWithStages> results = resultsByJobId.entrySet().stream()
                .map(entry -> {
                    List<JobStageAccumulatedData> jobResults = entry.getValue();
                    JobStageAccumulatedData firstResult = jobResults.get(0);

                    // Get unique masters for this job - filter nulls
                    List<String> uniqueMastersForJob = jobResults.stream()
                            .map(JobStageAccumulatedData::getMaster)
                            .filter(master -> master != null && !master.trim().isEmpty())
                            .distinct()
                            .collect(Collectors.toList());

                    // Group by stage type and aggregate across all masters for this job
                    // Filter out null stageTypes before grouping
                    Map<ProgressStage, List<JobStageAccumulatedData>> stageGroups = jobResults.stream()
                            .filter(result -> result.getStageType() != null)
                            .collect(Collectors.groupingBy(JobStageAccumulatedData::getStageType));

                    List<AccumulatedStageDataDto> stages = new ArrayList<>();
                    if(!stageGroups.isEmpty()) {
                        stages = stageGroups.entrySet().stream()
                                .map(stageEntry -> {
                                    List<JobStageAccumulatedData> stageResults = stageEntry.getValue();

                                    // Sum up all counts for this stage type across all masters
                                    Long totalSuccessCount = stageResults.stream()
                                            .mapToLong(result -> result.getTotalSuccessCount() != null ? result.getTotalSuccessCount() : 0L)
                                            .sum();

                                    Long totalServerFailureCount = stageResults.stream()
                                            .mapToLong(result -> result.getServerFailureCount() != null ? result.getServerFailureCount() : 0L)
                                            .sum();

                                    Long totalLogicalFailureCount = stageResults.stream()
                                            .mapToLong(result -> result.getLogicalFailureCount() != null ? result.getLogicalFailureCount() : 0L)
                                            .sum();

                                    return AccumulatedStageDataDto.builder()
                                            .stageType(stageEntry.getKey())
                                            .totalSuccessCount(totalSuccessCount)
                                            .totalFailureCount(totalServerFailureCount + totalLogicalFailureCount)
                                            .build();
                                })
                                .collect(Collectors.toList());
                    }

                    return JobEntityResponseDtoWithStages.builder()
                            .id(firstResult.getJobId())
                            .masters(uniqueMastersForJob)
                            .modeOfIntegration(firstResult.getModeOfIntegration())
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

        // Master processing - filter out null masters before grouping
        Map<String, List<JobStageAccumulatedData>> resultsByMaster = queryResults.stream()
                .filter(data -> data.getMaster() != null && !data.getMaster().trim().isEmpty())
                .collect(Collectors.groupingBy(JobStageAccumulatedData::getMaster));

        List<MasterCard> resultsMaster = resultsByMaster.entrySet().stream()
                .map(entry -> {
                    List<JobStageAccumulatedData> jobResults = entry.getValue();

                    // Filter out null status before grouping
                    Map<ProgressStatus, Long> statusCounts = jobResults.stream()
                            .filter(data -> data.getStatus() != null)
                            .collect(Collectors.groupingBy(
                                    JobStageAccumulatedData::getStatus,
                                    Collectors.mapping(
                                            JobStageAccumulatedData::getJobId,
                                            Collectors.collectingAndThen(
                                                    Collectors.toSet(),
                                                    set -> (long) set.size()
                                            )
                                    )
                            ));

                    Long completed_success = statusCounts.getOrDefault(ProgressStatus.COMPLETED_SUCCESSFULLY, 0L);
                    Long completed_unsuccess = statusCounts.getOrDefault(ProgressStatus.COMPLETED_UNSUCCESSFULLY, 0L);
                    Long pending = statusCounts.getOrDefault(ProgressStatus.PENDING, 0L);
                    Long failed = statusCounts.getOrDefault(ProgressStatus.FAILED, 0L);
                    Long completed = completed_success + completed_unsuccess;

                    List<AccumulatedStageDataDto> stages = jobResults.stream()
                            .filter(result -> result.getStageType() != null)
                            .map(result -> AccumulatedStageDataDto.builder()
                                    .stageType((result.getStageType()))
                                    .totalSuccessCount(result.getTotalSuccessCount() != null ? result.getTotalSuccessCount() : 0L)
                                    .totalFailureCount((result.getServerFailureCount() != null ? result.getServerFailureCount() : 0L) + (result.getLogicalFailureCount() != null ? result.getLogicalFailureCount() : 0L))
                                    .build())
                            .collect(Collectors.toList());

                    Long queueCount = stages.stream()
                            .filter(stage -> stage.getStageType() == ProgressStage.QUEUE)
                            .findFirst()
                            .map(queueStage -> queueStage.getTotalSuccessCount() + queueStage.getTotalFailureCount())
                            .orElse(0L);

                    Long saveCount = stages.stream()
                            .filter(stage -> stage.getStageType() == ProgressStage.SAVE)
                            .findFirst()
                            .map(saveStage -> saveStage.getTotalSuccessCount() + saveStage.getTotalFailureCount())
                            .orElse(0L);

                    return MasterCard.builder()
                            .queueCount(queueCount)
                            .saveCount(saveCount)
                            .completedJobCount(completed)
                            .pendingJobCount(pending)
                            .failedJobCount(failed)
                            .build();
                })
                .collect(Collectors.toList());

        AccumulatedJobsAndMasterDto dto = new AccumulatedJobsAndMasterDto();
        dto.setJobs(results);
        dto.setMasters(resultsMaster);
        return dto;
    }


    public Map<String, String> getModesPerLob(String lob) {
        return metadataRepository.findKeyValueByLobPrefix(lob).stream()
                .collect(Collectors.toMap(MetadataEntry::getKey, MetadataEntry::getValue));
    }

    @Transactional
    public int updateStaleJobs(String lob, ProgressStatus currentStatus,
                               ProgressStatus newStatus, Instant cutoffTime) {

        log.debug("Updating stale jobs for LOB: {}, currentStatus: {}, newStatus: {}, cutoffTime: {}",
                lob, currentStatus, newStatus, cutoffTime);

        int updatedCount = jobRepo.updateStaleJobsByLobAndStatus(
                lob, currentStatus, newStatus, cutoffTime);

        log.debug("Successfully updated {} jobs from {} to {} for LOB: {}",
                updatedCount, currentStatus, newStatus, lob);

        return updatedCount;
    }

}