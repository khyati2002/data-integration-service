package com.applicate.services.channelkart.services;
import com.applicate.services.channelkart.repository.ProductDetailsRepo;
import com.salescode.dim.cache.Cacheable;
import com.salescode.dim.jooq.generated.tables.pojos.Productdetails;

import java.util.List;

public class ProductDetailsService {
    private final ProductDetailsRepo productDetailsRepo;

    public ProductDetailsService(ProductDetailsRepo productDetailsRepo) {
        this.productDetailsRepo = productDetailsRepo;
    }

    @Cacheable
    public Productdetails findByBatchCode(String batchCode) {
        return this.productDetailsRepo.findByBatchCode(batchCode);
    }

    @Cacheable
    public List<Productdetails> findByEanCode(String eanCode) {
        return this.productDetailsRepo.findByEanCode(eanCode);
    }
}
