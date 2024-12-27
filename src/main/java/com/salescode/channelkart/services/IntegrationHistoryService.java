package com.salescode.channelkart.services;

import com.salescode.channelkart.abstractdatasource.DatabaseProfileRegistry;
import com.salescode.channelkart.models.IntegrationHistory;
import com.salescode.channelkart.repository.IntegrationHistoryRepository;
import com.salescode.channelkart.repository.NativeCDMMapper;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.utils.JdbcUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Service
public class IntegrationHistoryService {

    private static final String STATUS = "status";
    private static final String DEFAULT_INTEGRATION_STATUS_QUERY = "select entity_name,status,count from(select attributes->>'$.entityName' entity_name,'total' status, ifNull(attributes->>'$.sourceCount',0) count from ck_task where type='INTEGRATION_AUDIT' and creation_time>=FROM_UNIXTIME('{{startTime}}'/1000) and creation_time<FROM_UNIXTIME('{{endTime}}'/1000) union select entity_name,status,count(1) as count from ck_integration_history where timestamp>={{startTime}} and timestamp<{{endTime}} group by entity_name,status) res1";
    @Autowired
    NativeEntityManager nativeEntityManager;
    @Autowired
    IntegrationHistoryRepository repository;
    NativeCDMMapper nm = new NativeCDMMapper();

    public void update(List<IntegrationHistory> objects) {
        objects.forEach(object -> {
            if (object.getId() == null) {
                object.setId(UUID.randomUUID().toString());
            }
            object.setLob(SecurityContextUtils.getLob());
            object.setModifiedBy(SecurityContextUtils.getPrincipal());
        });

        nativeEntityManager.create(objects, " ON DUPLICATE KEY UPDATE request_id=VALUES(request_id), timestamp = VALUES(timestamp), group_id=VALUES(group_id),entity_id=VALUES(entity_id), action=VALUES(action),status=VALUES(status),extended_attributes=VALUES(extended_attributes),description=VALUES(description),offset=VALUES(offset)");
    }

    public void createFailureRecord(List<IntegrationHistory> integrationDataList, String errorDescription) {
        String lob = SecurityContextUtils.getLob();
        JdbcTemplate jdbcTemplate = JdbcUtils.createJdbcTemplate((DataSource) DatabaseProfileRegistry.getDataSourceHashMap().get(lob));
        jdbcTemplate.batchUpdate("insert into ck_integration_history (id,lob,group_id,request_id,timestamp,status,description)" + "values (?,?,?,?,?,?,?)", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                IntegrationHistory ih = integrationDataList.get(i);
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, lob);
                ps.setString(3, ih.getGroupId());
                ps.setString(4, ih.getRequestId());
                ps.setLong(5, ih.getTimestamp());
                ps.setString(6, ih.getStatus());
                ps.setString(7, "Failed to save request in integration_history, " + errorDescription);
            }

            @Override
            public int getBatchSize() {
                return integrationDataList.size();
            }
        });
    }

}