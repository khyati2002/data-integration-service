package com.salescode.dim.jooq.impl;

import com.salescode.dim.jooq.generated.tables.pojos.Productmetadata;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSetter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductMetaData extends com.salescode.dim.jooq.generated.tables.pojos.Productmetadata implements Serializable {


    @Getter(value = AccessLevel.NONE)
    private Location locationHierarchy;
    public ProductMetaData() {
        super();
    }
    public ProductMetaData(Productmetadata productMetaData) {
        super(productMetaData);
    }

    public static ProductMetaData of(ProductMetaData productMetaData) {
        if (productMetaData == null) {
            return null;
        }
        return new ProductMetaData(productMetaData);
    }
    public Location getLocation() {
        return locationHierarchy;
    }

}
