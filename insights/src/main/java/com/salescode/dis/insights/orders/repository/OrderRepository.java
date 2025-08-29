package com.salescode.dis.insights.orders.repository;

import com.salescode.dis.insights.orders.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    Page<OrderEntity> findByLobOrderByCreatedAtDesc(String lob, Pageable pageable);

    Optional<OrderEntity> findByOrderNumber(String orderNumber);
    Page<OrderEntity> findByLobAndOrderNumberContainingIgnoreCaseOrderByCreatedAtDesc(
            String lob, String orderNumber, Pageable pageable);

    Page<OrderEntity> findByLobAndUserContainingIgnoreCaseOrderByCreatedAtDesc(
            String lob, String user, Pageable pageable);

    Page<OrderEntity> findByLobAndOperationOrderByCreatedAtDesc(
            String lob, String operation, Pageable pageable);

    Optional<OrderEntity> findByOrderNumberAndLob(String orderNumber, String lob);

    @Query("SELECT " +
            "o.lob as lob, " +
            "COUNT(*) as totalRecords, " +
            "SUM(CASE WHEN o.readStatus = 'SUCCESS' THEN 1 ELSE 0 END) as readSuccess, " +
            "SUM(CASE WHEN o.readStatus = 'FAILURE' THEN 1 ELSE 0 END) as readFailure, " +
            "SUM(CASE WHEN o.readStatus = 'PENDING' THEN 1 ELSE 0 END) as readPending, " +
            "SUM(CASE WHEN o.processStatus = 'SUCCESS' THEN 1 ELSE 0 END) as processSuccess, " +
            "SUM(CASE WHEN o.processStatus = 'FAILURE' THEN 1 ELSE 0 END) as processFailure, " +
            "SUM(CASE WHEN o.processStatus = 'PENDING' THEN 1 ELSE 0 END) as processPending, " +
            "SUM(CASE WHEN o.saveStatus = 'SUCCESS' THEN 1 ELSE 0 END) as saveSuccess, " +
            "SUM(CASE WHEN o.saveStatus = 'FAILURE' THEN 1 ELSE 0 END) as saveFailure, " +
            "SUM(CASE WHEN o.saveStatus = 'PENDING' THEN 1 ELSE 0 END) as savePending, " +
            "SUM(CASE WHEN o.publishStatus = 'SUCCESS' THEN 1 ELSE 0 END) as publishSuccess, " +
            "SUM(CASE WHEN o.publishStatus = 'NA' THEN 1 ELSE 0 END) as publishNA " +
            "FROM OrderEntity o GROUP BY o.lob")
    List<Object[]> findOrderSummaryByLob();
}