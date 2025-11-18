package com.salescode.dim.jooq.impl;

import com.salescode.dim.jooq.impl.TargetResults;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDateTime;

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

    @Override
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public void setStartDate(LocalDateTime startDate) {
        super.setStartDate(startDate);
    }

    @Override
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public void setEndDate(LocalDateTime endDate) {
        super.setEndDate(endDate);
    }

    @Override
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public void setCreationTime(LocalDateTime creationTime) {
        super.setCreationTime(creationTime);
    }

    @Override
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public void setLastModifiedTime(LocalDateTime lastModifiedTime) {
        super.setLastModifiedTime(lastModifiedTime);
    }

    public List<TargetResults> getTargetResults() {
        return targetResults;
    }

    public void setTargetResults(List<TargetResults> targetResults) {
        this.targetResults = targetResults;
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