package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.generated.tables.pojos.Productdetails;
import com.salescode.dim.jooq.impl.SchemeDefination;

public interface ProductDetailsRepo {
    Productdetails findByBatchCode(String batchCode);
}
