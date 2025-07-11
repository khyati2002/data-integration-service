package com.salescode.dis.insights.repository;

import com.salescode.dis.insights.entity.StageMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StageMetadataRepository extends JpaRepository<StageMetadata, String> {

}