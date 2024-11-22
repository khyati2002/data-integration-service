package com.salescode.jooq;

import org.jooq.codegen.DefaultGeneratorStrategy;
import org.jooq.meta.ColumnDefinition;
import org.jooq.meta.Definition;
import org.jooq.meta.mysql.MySQLTableDefinition;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CustomGeneratorStrategy extends DefaultGeneratorStrategy {
    @Override
    public String getJavaClassExtends(Definition definition, Mode mode) {
        boolean  flag;
        try {
            List<String> collect = ((MySQLTableDefinition) definition).getElements0().stream().map(s -> s.getName()).collect(Collectors.toList());
            flag = collect.stream().filter(s->s.contains("extended_attributes")||s.contains("hash")).count() == 2;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (mode == Mode.POJO && flag) {
            return "com.salescode.channelkart.models.CommonDataModel";
        }
        return super.getJavaClassExtends(definition, mode);
    }

}