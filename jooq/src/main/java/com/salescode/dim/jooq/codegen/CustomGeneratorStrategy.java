package com.salescode.dim.jooq.codegen;

import org.jooq.codegen.DefaultGeneratorStrategy;
import org.jooq.meta.Definition;
import org.jooq.meta.mysql.MySQLTableDefinition;

import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomGeneratorStrategy extends DefaultGeneratorStrategy {

    private static final String COMMON_DATA_MODEL_FQN = "com.applicate.services.channelkart.models.CommonDataModel";

    @Override
    public String getJavaClassExtends(Definition definition, Mode mode) {
        // Only modify POJO generation
        if (mode != Mode.POJO || !(definition instanceof MySQLTableDefinition)) {
            return super.getJavaClassExtends(definition, mode);
        }

        try {
            // Extract all column names from the table definition into a Set for faster lookup
            Set<String> columnNames = ((MySQLTableDefinition) definition)
                    .getElements0()
                    .stream()
                    .map(element -> element.getName())
                    .collect(Collectors.toSet());

            // Check if the table contains both "extended_attributes" and "hash" columns
            if (columnNames.contains("extended_attributes") && columnNames.contains("hash")) {
                return COMMON_DATA_MODEL_FQN;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving table elements for: " + definition.getName(), e);
        }

        return super.getJavaClassExtends(definition, mode);
    }


    @Override
    public String getJavaClassName(Definition definition, Mode mode) {
        // Only modify POJO generation
        if (mode == Mode.POJO && (definition instanceof MySQLTableDefinition)) {
            String javaClassName = super.getJavaClassName(definition, mode);
            if(javaClassName.startsWith("Ck")){
                javaClassName = javaClassName.substring(2);
            }
            return javaClassName;
        }
        return super.getJavaClassName(definition, mode);
    }
}