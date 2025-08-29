package com.salescode.dis.insights.orders.repository;

import com.salescode.dis.insights.orders.entity.LobRetentionConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LobRetentionConfigRepository extends JpaRepository<LobRetentionConfigEntity, String> {
}