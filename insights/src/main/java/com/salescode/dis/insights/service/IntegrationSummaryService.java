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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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

    public Object getOnlyLobDetails(List<String> lobList) {
        List<Map<String, Object>> resp;

        if (lobList == null || lobList.isEmpty()) {
            resp = jobRepository.getLobDetailsAll(); // optionally modify to support date filter
        } else {
            resp = jobRepository.getLobDetails(lobList); // optionally modify to support date filter
        }

        Map<String, Map<String, Object>> lobSummary = (Map<String, Map<String, Object>>)
                getLobSummary(lobList, Collections.emptyList(), null,null);

        Map<String, Map<String, Object>> result = new HashMap<>();

        for (Map<String, Object> row : resp) {
            String lob = (String) row.get("lob");
            int completed = ((Number) row.get("COMPLETED")).intValue();
            int inProgress = ((Number) row.get("PENDING")).intValue();
            int failed = ((Number) row.get("FAILED")).intValue();

            Map<String, Object> detailsMap = new HashMap<>();
            detailsMap.put("COMPLETED", completed);
            detailsMap.put("PENDING", inProgress);
            detailsMap.put("FAILED", failed);

            // Calculate average throughput per LOB and put inside details
            Map<String, Object> jobsMap = lobSummary.get(lob);
            if (jobsMap != null) {
                double totalConsumerThroughput = 0.0;
                double totalPublisherThroughput = 0.0;
                int jobCount = 0;

                for (Map.Entry<String, Object> jobEntry : jobsMap.entrySet()) {
                    Map<String, Object> jobData = (Map<String, Object>) jobEntry.getValue();

                    totalConsumerThroughput += getSafeDouble(jobData, "avg_consumer_throughput");
                    totalPublisherThroughput += getSafeDouble(jobData, "avg_publisher_throughput");
                    jobCount++;
                }

                double consumerAvg = jobCount > 0 ? totalConsumerThroughput / jobCount : 0.0;
                double publisherAvg = jobCount > 0 ? totalPublisherThroughput / jobCount : 0.0;

                detailsMap.put("consumed_throughput", Double.isFinite(consumerAvg)
                        ? BigDecimal.valueOf(consumerAvg).setScale(2, RoundingMode.HALF_UP).doubleValue()
                        : 0.0);

                detailsMap.put("published_throughput", Double.isFinite(publisherAvg)
                        ? BigDecimal.valueOf(publisherAvg).setScale(2, RoundingMode.HALF_UP).doubleValue()
                        : 0.0);
            }

            Map<String, Object> lobMap = new HashMap<>();
            lobMap.put("details", detailsMap);

            result.put(lob, lobMap);
        }

        return result;
    }


    public Object getLobSummary(List<String> lobList, List<String> status,LocalDateTime startTime,LocalDateTime endTime) {
        List<Map<String, Object>> resp;

        // Ensure the startDate and endDate are converted to Timestamp if they are not null
        Timestamp startTimestamp = (startTime != null) ? Timestamp.valueOf(startTime) : null;
        Timestamp endTimestamp = (endTime != null) ? Timestamp.valueOf(endTime) : null;

        // Call the repository method
        resp = jobRepository.getLobSummary(lobList, status, startTimestamp,endTimestamp);

        // Using Stream API to aggregate data
        Map<String, Map<String, Object>> aggregatedData = resp.stream()
                .collect(Collectors.groupingBy(
                        record -> (String) record.get("lob"), // Group by lob
                        Collectors.toMap(
                                record -> (String) record.get("job_id"), // Key by job_id
                                record -> {
                                    Map<String, Object> jobData = new HashMap<>();

                                    // Your existing counts...
                                    jobData.put("consumed_fail_count", getSafeInt(record, "consumed_fail_count"));
                                    jobData.put("consumed_success_count", getSafeInt(record, "consumed_success_count"));
                                    jobData.put("published_success_count", getSafeInt(record, "published_success_count"));
                                    jobData.put("total_count", getSafeInt(record, "total_count"));
                                    jobData.put("server_fail_count", getSafeInt(record, "server_fail_count"));
                                    jobData.put("logical_fail_count", getSafeInt(record, "logical_fail_count"));
                                    jobData.put("published_fail_count", getSafeInt(record, "published_fail_count"));
                                    jobData.put("status", record.get("status"));

                                    Instant startTime_job = (Instant) record.get("start_time");
                                    Instant endTime_job = (Instant) record.get("end_time");
                                    jobData.put("startTime", startTime_job != null ? startTime_job.toString() : null);
                                    jobData.put("endTime", endTime_job != null ? endTime_job.toString() : null);

                                    jobData.put("consumer_throughput_sum", getSafeDouble(record, "consumer_throughput"));
                                    jobData.put("publisher_throughput_sum", getSafeDouble(record, "publisher_throughput"));
                                    jobData.put("throughput_count", 1);

                                    // NEW: Add master to a list
                                    Set<Object> masters = new HashSet<>();
                                    Object master = record.get("master");
                                    if (master != null) {
                                        masters.add(master);
                                    }
                                    jobData.put("masters", masters);

                                    return jobData;
                                },
                                (existing, replacement) -> {
                                    Map<String, Object> existingMap = (Map<String, Object>) existing;
                                    Map<String, Object> replacementMap = (Map<String, Object>) replacement;

                                    // Sum counts
                                    existingMap.put("consumed_fail_count", aggregateSafeInt(existingMap, replacementMap, "consumed_fail_count"));
                                    existingMap.put("consumed_success_count", aggregateSafeInt(existingMap, replacementMap, "consumed_success_count"));
                                    existingMap.put("published_success_count", aggregateSafeInt(existingMap, replacementMap, "published_success_count"));
                                    existingMap.put("total_count", aggregateSafeInt(existingMap, replacementMap, "total_count"));
                                    existingMap.put("server_fail_count", aggregateSafeInt(existingMap, replacementMap, "server_fail_count"));
                                    existingMap.put("logical_fail_count", aggregateSafeInt(existingMap, replacementMap, "logical_fail_count"));
                                    existingMap.put("published_fail_count", aggregateSafeInt(existingMap, replacementMap, "published_fail_count"));

                                    existingMap.put("consumer_throughput_sum", aggregateSafeDouble(existingMap, replacementMap, "consumer_throughput_sum"));
                                    existingMap.put("publisher_throughput_sum", aggregateSafeDouble(existingMap, replacementMap, "publisher_throughput_sum"));

                                    int count1 = (int) existingMap.getOrDefault("throughput_count", 0);
                                    int count2 = (int) replacementMap.getOrDefault("throughput_count", 0);
                                    existingMap.put("throughput_count", count1 + count2);

                                    Set<Object> existingMasters = new HashSet<>((Collection<?>) existingMap.getOrDefault("masters", new HashSet<>()));
                                    Set<Object> newMasters = new HashSet<>((Collection<?>) replacementMap.getOrDefault("masters", new HashSet<>()));
                                    existingMasters.addAll(newMasters);
                                    existingMap.put("master", existingMasters);
                                    existingMap.remove("masters");

                                    return existingMap;
                                }
                        )
                ));

        // After aggregation, compute average throughput per job
        aggregatedData.forEach((lob, jobMap) -> {
            for (Map.Entry<String, Object> entry : jobMap.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    Map<String, Object> jobData = (Map<String, Object>) entry.getValue();
                    double consumerSum = getSafeDouble(jobData, "consumer_throughput_sum");
                    double publisherSum = getSafeDouble(jobData, "publisher_throughput_sum");
                    int count = (int) jobData.getOrDefault("throughput_count", 1);

                    double avgConsumer = (count > 0) ? (consumerSum / count) : 0.0;
                    if (Double.isFinite(avgConsumer)) {
                        jobData.put("avg_consumer_throughput", BigDecimal.valueOf(avgConsumer).setScale(2, RoundingMode.HALF_UP).doubleValue());
                    } else {
                        jobData.put("avg_consumer_throughput", 0.0);
                    }

                    double avgPublisher = (count > 0) ? (publisherSum / count) : 0.0;
                    if (Double.isFinite(avgPublisher)) {
                        jobData.put("avg_publisher_throughput", BigDecimal.valueOf(avgPublisher).setScale(2, RoundingMode.HALF_UP).doubleValue());
                    } else {
                        jobData.put("avg_publisher_throughput", 0.0);
                    }


                    // Remove intermediate sum and count if not needed
                    jobData.remove("consumer_throughput_sum");
                    jobData.remove("publisher_throughput_sum");
                    jobData.remove("throughput_count");
                }
            }
        });

        return aggregatedData;
    }


    private double getSafeDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0; // Or log the issue and return default
            }
        }
        return 0.0;
    }


    private double aggregateSafeDouble(Map<String, Object> existing, Map<String, Object> incoming, String key) {
        double existingVal = getSafeDouble(existing, key);
        double incomingVal = getSafeDouble(incoming, key);
        return existingVal + incomingVal;
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
