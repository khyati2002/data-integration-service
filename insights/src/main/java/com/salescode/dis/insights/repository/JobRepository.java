package com.salescode.dis.insights.repository;

import com.salescode.dis.insights.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<JobEntity, Long> { }