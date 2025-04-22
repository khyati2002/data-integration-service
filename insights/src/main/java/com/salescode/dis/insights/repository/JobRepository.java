package com.salescode.dis.insights.repository;

import com.salescode.dis.insights.entity.JobEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<JobEntity, String> {

    Page<JobEntity> getJobEntitiesByLob(String lob, Pageable pageable);

    Page<JobEntity> getJobEntitiesByLobAndMaster(String lob, String master, Pageable pageable);
}