package com.salescode.jooq;

import org.jooq.codegen.DefaultGeneratorStrategy;
import org.jooq.meta.Definition;

public class CustomGeneratorStrategy extends DefaultGeneratorStrategy {
    @Override
    public String getJavaClassExtends(Definition definition, Mode mode) {
        if (mode == Mode.POJO) {
            return "com.salescode.channelkart.models.CommonDataModel";
        }
        return super.getJavaClassExtends(definition, mode);
    }

    @Override
    public String getJavaClassName(Definition definition, Mode mode) {
//        if (definition instanceof TableDefinition) {
//            TableDefinition table = (TableDefinition) definition;
//            String tableName = table.getName();
//
//            // Customize class names per entity
//            switch (tableName.toLowerCase()) {
//                case "ck_outlet_details":
//                    return "OutletDetails";
//                case "ck_user":
//                    return "User";
//                default:
//                    return super.getJavaClassName(definition, mode);
//            }
//        }
        return super.getJavaClassName(definition, mode);
    }



}