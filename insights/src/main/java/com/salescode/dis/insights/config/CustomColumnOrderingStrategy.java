package com.salescode.dis.insights.config;

import com.salescode.dis.insights.entity.mapped.CommonEntity;
import com.salescode.dis.insights.entity.mapped.TimeAwareEntity;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.ColumnOrderingStrategyStandard;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.*;

@Configuration
public class CustomColumnOrderingStrategy extends ColumnOrderingStrategyStandard implements HibernatePropertiesCustomizer {

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.COLUMN_ORDERING_STRATEGY, this);
    }

    @Override
    public List<Column> orderTableColumns(Table table, Metadata metadata) {
        Map<String, Column> columnMap = new HashMap<>();
        for (Column column : table.getColumns()) {
            String columnNameLower = column.getName().toLowerCase();
            if (columnMap.putIfAbsent(columnNameLower, column) != null) {
                throw new IllegalStateException("Duplicate column detected: " + column.getName());
            }
        }
        List<Column> orderedColumns = new ArrayList<>();
        // Apply prioritized ordering based on entity field names
        orderedColumns.addAll(extractColumnsForEntity(CommonEntity.class, columnMap));
        orderedColumns.addAll(extractColumnsForEntity(TimeAwareEntity.class, columnMap));
        orderedColumns.addAll(super.orderColumns(columnMap.values(),metadata));
        return orderedColumns;
    }

    private List<Column> extractColumnsForEntity(Class<?> entityClass, Map<String, Column> columnMap) {
        List<Column> columns = new ArrayList<>();
        for (Field field : entityClass.getDeclaredFields()) {
            String fieldNameSnakeCase = toSnakeCase(field.getName());
            Column column = columnMap.remove(fieldNameSnakeCase);
            if (column != null) {
                columns.add(column);
            }
        }
        return columns;
    }

    private String toSnakeCase(String input) {
        return input.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }
}