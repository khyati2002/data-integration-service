package com.salescode.dim.jooq.impl;

import com.salescode.dim.jooq.generated.tables.pojos.Productmetadata;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductMetaData extends Productmetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    @Getter(AccessLevel.NONE)
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

    /**
     * The supplier field maps to the database column 'loginid'
     */
    private String supplier;

    @JsonProperty("supplier")
    public String getSupplier() {
        return getLoginid(); // maps DB column 'loginid'
    }

    @JsonProperty("supplier")
    public void setSupplier(String supplier) {
        setLoginid(supplier); // sets DB field 'loginid'
        this.supplier = supplier;
    }

    public Location getLocation() {
        return locationHierarchy;
    }
}