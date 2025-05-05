package com.salescode.dis.insights.service;

import com.salescode.dis.insights.dto.JobEntityResponseDtoWithFiles;
import com.salescode.dis.insights.dto.LobSummaryDTO;
import com.salescode.dis.insights.entity.FileEntity;
import com.salescode.dis.insights.entity.JobEntity;
import com.salescode.dis.insights.mapper.JobEntityMapper;
import com.salescode.dis.insights.repository.JobRepository;
import com.salescode.dis.insights.specification.JobSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class IntegrationSummaryService {

    private final JobRepository jobRepository;

    @Autowired
    private JobEntityMapper jobEntityMapper;

    @Autowired
    public IntegrationSummaryService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public Map<String, Map<String, Object>> getLobSummary(List<String> lobs, Map<String, String> jobFilters) {
        List<Map<String, Object>> rawList = jobRepository.getFilteredLobSummary(lobs, jobFilters);
        return groupByJob(rawList);
    }

    public Map<String, Map<String, Object>> getOnlyLobDetails(List<String> lobs, Map<String, String> jobFilters) {
        List<Map<String, Object>> rawList = jobRepository.getFilteredLobDetails(lobs, jobFilters);
        return groupByLob(rawList);
    }

    private Map<String, Map<String, Object>> groupByLob(List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> grouped = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String lob = (String) row.get("lob");
            if (lob == null) continue;

            // Optional: make a copy if you want to remove the "lob" field
            Map<String, Object> rowCopy = new HashMap<>(row);
            rowCopy.remove("lob");

            grouped.put(lob, rowCopy);
        }
        return grouped;
    }

    private Map<String, Map<String, Object>> groupByJob(List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> grouped = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String id = (String) row.get("job_id");
            if (id == null) continue;

            // Optional: make a copy if you want to remove the "lob" field
            Map<String, Object> rowCopy = new HashMap<>(row);
            rowCopy.remove("job_id");

            grouped.put(id, rowCopy);
        }
        return grouped;
    }



    public List<LobSummaryDTO> getIntegrationSummary(
            List<String> lobs,
            Map<String, String> jobFilters,
            Map<String, String> fileFilters
    ) {
        Specification<JobEntity> spec = JobSpecification.getJobWithFileSpecification(
                lobs, jobFilters, fileFilters
        );

        List<JobEntity> jobs = jobRepository.findAll(spec);

        // Group by LOB
        Map<String, List<JobEntityResponseDtoWithFiles>> groupedByLob = jobs.stream()
                .map(job ->jobEntityMapper.toJobWithFilesDto(job))
                .collect(Collectors.groupingBy(JobEntityResponseDtoWithFiles::getLob)); // here you might want to group by "LOB" instead of jobName?

        return groupedByLob.entrySet().stream()
                .map(entry -> new LobSummaryDTO(entry.getKey(), entry.getValue()))
                .toList();
    }

    public Object getOnlyLobDetails(List<String> lobList) {
        List<Map<String, Object>> resp;

        if (lobList == null || lobList.isEmpty()) {
            resp = jobRepository.getLobDetailsAll(); // returns List<Map<String, Object>>
        } else {
            resp = jobRepository.getLobDetails(lobList); // returns List<Map<String, Object>>
        }

        Map<String, Map<String, Object>> result = new HashMap<>();

        for (Map<String, Object> row : resp) {
            String lob = (String) row.get("lob");
            int completed = ((Number) row.get("COMPLETED")).intValue();
            int inProgress = ((Number) row.get("PENDING")).intValue();
            int failed = ((Number) row.get("FAILED")).intValue();
            int distinctMasters = ((Number) row.get("distinct_master_count")).intValue();

            Map<String, Object> detailsMap = new HashMap<>();
            detailsMap.put("COMPLETED", completed);
            detailsMap.put("PENDING", inProgress);
            detailsMap.put("FAILED", failed);
            detailsMap.put("DISTINCT_MASTERS", distinctMasters);

            Map<String, Object> lobMap = new HashMap<>();
            lobMap.put("details", detailsMap);

            result.put(lob, lobMap);
        }

        return result;
    }

    public Object accumulateLobSummary(List<Map<String, Object>>  resp) {
        Map<String, Map<String, Object>> aggregatedData = resp.stream()
                .collect(Collectors.groupingBy(
                        record -> (String) record.get("lob"), // Group by lob
                        Collectors.toMap(
                                record -> (String) record.get("job_id"), // Key by job_id
                                record -> {
                                    Map<String, Object> jobData = new HashMap<>();

                                    // Null check before accessing Number values
                                    jobData.put("consumed_fail_count", getSafeInt(record, "consumed_fail_count"));
                                    jobData.put("consumed_success_count", getSafeInt(record, "consumed_success_count"));
                                    jobData.put("total_count", getSafeInt(record, "total_count"));
                                    jobData.put("server_fail_count", getSafeInt(record, "server_fail_count"));
                                    jobData.put("logical_fail_count", getSafeInt(record, "logical_fail_count"));
                                    jobData.put("published_fail_count", getSafeInt(record, "published_fail_count"));
                                    jobData.put("status",record.get("status"));
                                    jobData.put("master_name",record.get("master"));
                                    Instant instant = (Instant) record.get("start_time");
                                    ZonedDateTime localDateTime = instant.atZone(ZoneId.systemDefault());
                                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS z");

                                    Instant instantEND =  (Instant)record.get("end_time");
                                    ZonedDateTime localDateTimeEnd = instant.atZone(ZoneId.systemDefault());
                                    DateTimeFormatter formatterEnd = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS z");
                                    jobData.put("startTime", (String) localDateTime.format(formatter));
                                    jobData.put("endTime", (String) localDateTimeEnd.format(formatterEnd));
                                    return jobData;
                                },
                                (existing, replacement) -> {
                                    Map<String, Object> existingMap = (Map<String, Object>) existing;
                                    Map<String, Object> replacementMap = (Map<String, Object>) replacement;

                                    // Aggregate values with null safety
                                    existingMap.put("consumed_fail_count", aggregateSafeInt(existingMap, replacementMap, "consumed_fail_count"));
                                    existingMap.put("consumed_success_count", aggregateSafeInt(existingMap, replacementMap, "consumed_success_count"));
                                    existingMap.put("total_count", aggregateSafeInt(existingMap, replacementMap, "total_count"));
                                    existingMap.put("server_fail_count", aggregateSafeInt(existingMap, replacementMap, "server_fail_count"));
                                    existingMap.put("logical_fail_count", aggregateSafeInt(existingMap, replacementMap, "logical_fail_count"));
                                    existingMap.put("published_fail_count", aggregateSafeInt(existingMap, replacementMap, "published_fail_count"));
                                    return existingMap;
                                }
                        )
                ));

        return aggregatedData; // Return the aggregated data
    }

    public Object getLobSummary(List<String> lobList){
        List<Map<String, Object>>  resp;

        if(lobList == null || lobList.isEmpty()){
            resp = jobRepository.getLobSummaryAll();
        } else {
            resp =  jobRepository.getLobSummary(lobList);
        }

        // Using Stream API to aggregate data
        Map<String, Map<String, Object>> aggregatedData = resp.stream()
                .collect(Collectors.groupingBy(
                        record -> (String) record.get("lob"), // Group by lob
                        Collectors.toMap(
                                record -> (String) record.get("job_id"), // Key by job_id
                                record -> {
                                    Map<String, Object> jobData = new HashMap<>();

                                    // Null check before accessing Number values
                                    jobData.put("consumed_fail_count", getSafeInt(record, "consumed_fail_count"));
                                    jobData.put("consumed_success_count", getSafeInt(record, "consumed_success_count"));
                                    jobData.put("total_count", getSafeInt(record, "total_count"));
                                    jobData.put("server_fail_count", getSafeInt(record, "server_fail_count"));
                                    jobData.put("logical_fail_count", getSafeInt(record, "logical_fail_count"));
                                    jobData.put("published_fail_count", getSafeInt(record, "published_fail_count"));
                                    jobData.put("status",record.get("status"));
                                    jobData.put("master_name",record.get("master"));
                                    Instant instant = (Instant) record.get("start_time");
                                    ZonedDateTime localDateTime = instant.atZone(ZoneId.systemDefault());
                                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS z");

                                    Instant instantEND =  (Instant)record.get("end_time");
                                    ZonedDateTime localDateTimeEnd = instant.atZone(ZoneId.systemDefault());
                                    DateTimeFormatter formatterEnd = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS z");
                                    jobData.put("startTime", (String) localDateTime.format(formatter));
                                    jobData.put("endTime", (String) localDateTimeEnd.format(formatterEnd));
                                    return jobData;
                                },
                                (existing, replacement) -> {
                                    Map<String, Object> existingMap = (Map<String, Object>) existing;
                                    Map<String, Object> replacementMap = (Map<String, Object>) replacement;

                                    // Aggregate values with null safety
                                    existingMap.put("consumed_fail_count", aggregateSafeInt(existingMap, replacementMap, "consumed_fail_count"));
                                    existingMap.put("consumed_success_count", aggregateSafeInt(existingMap, replacementMap, "consumed_success_count"));
                                    existingMap.put("total_count", aggregateSafeInt(existingMap, replacementMap, "total_count"));
                                    existingMap.put("server_fail_count", aggregateSafeInt(existingMap, replacementMap, "server_fail_count"));
                                    existingMap.put("logical_fail_count", aggregateSafeInt(existingMap, replacementMap, "logical_fail_count"));
                                    existingMap.put("published_fail_count", aggregateSafeInt(existingMap, replacementMap, "published_fail_count"));
                                    return existingMap;
                                }
                        )
                ));

        return aggregatedData; // Return the aggregated data
    }

    // Helper method to safely get an integer value from the map
    private int getSafeInt(Map<String, Object> record, String key) {
        Number value = (Number) record.get(key);
        return (value != null) ? value.intValue() : 0; // Default to 0 if null
    }

    // Helper method to safely aggregate integer values
    private int aggregateSafeInt(Map<String, Object> existingMap, Map<String, Object> replacementMap, String key) {
        int existingValue = getSafeInt(existingMap, key);
        int replacementValue = getSafeInt(replacementMap, key);
        return existingValue + replacementValue; // Aggregate with null safety
    }

}
