package com.salescode.dim.jooq.impl;

import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSetter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDateTime;

import java.io.Serializable;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TargetResults extends com.salescode.dim.jooq.generated.tables.pojos.TargetResults implements Serializable {

    private static final long serialVersionUID = 6364280713919356300L;


    public TargetResults(){
        super();
    }


    private TargetResults(com.salescode.dim.jooq.generated.tables.pojos.TargetResults targetsResults) {
        super(targetsResults);
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

    @JsonSetter("loginId")
    public void setLoginId(String loginId) {
        setLoginid(loginId);
    }

    public String getLoginId() {
        return getLoginid();
    }

    @JsonSetter("outletCode")
    public void setOutletCode(String outletCode) {
        setOutletcode(outletCode);
    }
    public String getOutletCode() {
        return getOutletcode();
    }

    public static TargetResults of(com.salescode.dim.jooq.generated.tables.pojos.TargetResults targetResults) {
        if(targetResults == null) {
            return null;
        }
        return new TargetResults(targetResults);
    }

}


