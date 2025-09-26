package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.generated.tables.pojos.Productdetails;

public interface ProductDetailsRepo {
    public Productdetails findByBatchCode(String var1);
}
