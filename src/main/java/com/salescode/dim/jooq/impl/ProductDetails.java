package com.salescode.dim.jooq.impl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDetails extends com.salescode.dim.jooq.generated.tables.pojos.Productdetails implements Serializable {

    private static final long serialVersionUID = 6364280713919356300L;



    public ProductDetails(){
        super();
    }

    private ProductDetails(com.salescode.dim.jooq.generated.tables.pojos.Productdetails productDetails) {
        super(productDetails);
    }

    public static ProductDetails of(com.salescode.dim.jooq.generated.tables.pojos.Productdetails productDetails) {
        if(productDetails == null) {
            return null;
        }
        return new ProductDetails(productDetails);
    }


    /**
     * @return the fileName_a
     */
    public String getFileName_a() {
        return super.getFileNameA();
    }

    /**
     * @param fileName_a the fileName_a to set
     */
    public void setFileName_a(String fileName_a) {
        super.setFileNameA(fileName_a);
    }

    /**
     * @return the fileName_b
     */
    public String getFileName_b() {
        return super.getFileNameB();
    }

    /**
     * @param fileName_b the fileName_b to set
     */
    public void setFileName_b(String fileName_b) {
        super.setFileNameB(fileName_b);

    }

    /**
     * @return the fileName_c
     */
    public String getFileName_c() {
        return super.getFileNameC();
    }

    /**
     * @param fileName_c the fileName_c to set
     */
    public void setFileName_c(String fileName_c) {
        super.setFileNameC(fileName_c);
    }

    /**
     * @return the fileName_f
     */
    public String getFileName_f() {
        return super.getFileNameF();
    }

    /**
     * @param fileName_f the fileName_f to set
     */
    public void setFileName_f(String fileName_f) {
        super.setFileNameF(fileName_f);
    }

    /**
     * @return the fileName_l
     */
    public String getFileName_l() {
        return super.getFileNameL();
    }

    /**
     * @param fileName_l the fileName_l to set
     */
    public void setFileName_l(String fileName_l) {
        super.setFileNameL(fileName_l);

    }


}


