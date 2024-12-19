package com.salescode.channelkart.repository;

import com.salescode.channelkart.models.IntegrationHistory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntegrationHistoryRepository extends CommonJpaRepository<com.salescode.channelkart.models.IntegrationHistory, String> {

    List<IntegrationHistory> findByRequestId(String requestId);

    List<IntegrationHistory> findByGroupId(String groupId);

}
