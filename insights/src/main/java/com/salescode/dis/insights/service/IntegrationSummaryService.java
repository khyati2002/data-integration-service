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

}
