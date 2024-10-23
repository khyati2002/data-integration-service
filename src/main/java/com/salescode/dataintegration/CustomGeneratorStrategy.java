package com.salescode.dataintegration;

import org.jooq.codegen.DefaultGeneratorStrategy;
import org.jooq.meta.Definition;

public class CustomGeneratorStrategy extends DefaultGeneratorStrategy {
    @Override
    public String getJavaClassExtends(Definition definition, Mode mode) {
        if (mode == Mode.POJO  || mode == Mode.RECORD) {
            return "com.salescode.channelkart.models.CommonDataModel";
        }
        return super.getJavaClassExtends(definition, mode);
    }
}