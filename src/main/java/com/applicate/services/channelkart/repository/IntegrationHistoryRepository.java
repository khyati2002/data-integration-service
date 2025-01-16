package com.applicate.services.channelkart.repository;

import com.applicate.services.channelkart.models.IntegrationHistory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntegrationHistoryRepository extends CommonJpaRepository<IntegrationHistory, String> {

    List<IntegrationHistory> findByRequestId(String requestId);

    List<IntegrationHistory> findByGroupId(String groupId);

}
