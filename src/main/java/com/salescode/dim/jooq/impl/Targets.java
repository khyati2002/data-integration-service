package com.salescode.dim.jooq.impl;

import com.salescode.dim.jooq.impl.TargetResults;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Targets extends com.salescode.dim.jooq.generated.tables.pojos.Targets implements Serializable {

    private static final long serialVersionUID = 6364280713919356300L;


    @Getter(value = AccessLevel.NONE)
    private List<TargetResults> targetResults;

    public Targets(){
        super();
    }

    public List<TargetResults> getTargetResults() {
        return targetResults;
    }

    private Targets(com.salescode.dim.jooq.generated.tables.pojos.Targets targets) {
        super(targets);
    }

    public static Targets of(com.salescode.dim.jooq.generated.tables.pojos.Targets targets) {
        if(targets == null) {
            return null;
        }
        return new Targets(targets);
    }


}


