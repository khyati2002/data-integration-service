package com.salescode.dim.jooq.impl;


import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;


@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TempMasterMapping extends com.salescode.dim.jooq.generated.tables.pojos.TempMasterMapping implements Serializable {

    public TempMasterMapping() {
        super();
    }

    private TempMasterMapping(TempMasterMapping tempMasterMapping) {
        super(tempMasterMapping);
    }

    public static TempMasterMapping of(TempMasterMapping tempMasterMapping) {
        if (tempMasterMapping == null) {
            return null;
        }
        return new TempMasterMapping(tempMasterMapping);
    }

}

