package com.salescode.dim.jooq.impl;

import com.salescode.dim.jooq.generated.tables.pojos.Discount;
import com.salescode.dim.jooq.generated.tables.pojos.Productdetails;
import com.salescode.dim.jooq.generated.tables.pojos.Productmetadata;
import com.salescode.dim.jooq.generated.tables.pojos.Stock;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSetter;

import java.io.Serializable;
import java.util.List;


@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDetails extends com.salescode.dim.jooq.generated.tables.pojos.Productdetails implements Serializable {


    private List<Productmetadata> productMetaData;
    private List<Discount> discount;
    private List<Stock> stock;

    public ProductDetails() {
        super();
    }

    private ProductDetails(Productdetails productdetails) {
        super(productdetails);
    }

    public static ProductDetails of(Productdetails productdetails) {
        if (productdetails == null) {
            return null;
        }
        return new ProductDetails(productdetails);
    }

    @JsonSetter("fileName_a")
    public void setFileName_a(String fileName_a) {
        super.setFileNameA(fileName_a);
    }

    @JsonSetter("fileName_b")
    public void setFileName_b(String fileName_b) {
        super.setFileNameB(fileName_b);
    }

    @JsonSetter("fileName_c")
    public void isFilename_c(String fileName_c) {
        super.setFileNameC(fileName_c);
    }

    @JsonSetter("fileName_f")
    public void setFileName_f(String fileName_f) {
        super.setFileNameF(fileName_f);
    }

    @JsonSetter("fileName_l")
    public void setFileName_l(String fileName_l) {
        super.setFileNameL(fileName_l);
    }

    @JsonSetter("blobKey_a")
    public void setBlobKey_a(String blobKey_a) {
        super.setBlobKeyA(blobKey_a);
    }

    @JsonSetter("blobKey_b")
    public void setBlobKey_b(String blobKey_b) {
        super.setBlobKeyB(blobKey_b);
    }

    @JsonSetter("blobKey_c")
    public void setBlobKey_c(String blobKey_c) {
        super.setBlobKeyC(blobKey_c);
    }

    @JsonSetter("blobKey_f")
    public void setBlobKey_f(String blobKey_f) {
        super.setBlobKeyF(blobKey_f);
    }

    @JsonSetter("blobKey_l")
    public void setBlobKey_l(String blobKey_l) {
        super.setBlobKeyL(blobKey_l);
    }

    @JsonSetter("mCode")
    public void setmCode(String mCode) {
        super.setMCode(mCode);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.deleteCharAt(sb.length() - 1);
        sb.append(", ").append(productMetaData);
        sb.append(", ").append(discount);
        sb.append(", ").append(stock);
        sb.append(")");
        return sb.toString();
    }

}
