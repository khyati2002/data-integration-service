package com.salescode.dim.jooq.impl;

import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSetter;

@SuppressWarnings({"all", "unchecked", "rawtypes"})
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HierarchyMetadata extends com.salescode.dim.jooq.generated.tables.pojos.HierarchyMetadata {
    private static final long serialVersionUID = -7546424289965519236L;

    @JsonSetter("immediateParent")
    public void setImmediateParent(String parent) {
        setParent(parent);
    }

}
