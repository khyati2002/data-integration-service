package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.generated.tables.pojos.Productdetails;

import java.util.List;

public interface ProductDetailsRepo {
    public Productdetails findByBatchCode(String var1);
    public List<Productdetails> findByEanCode(String var1);
}
