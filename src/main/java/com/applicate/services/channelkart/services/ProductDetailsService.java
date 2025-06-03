package com.applicate.services.channelkart.services;
import com.applicate.services.channelkart.repository.ProductDetailsRepo;
import com.salescode.dim.jooq.generated.tables.pojos.Productdetails;

public class ProductDetailsService {
    private final ProductDetailsRepo productDetailsRepo;

    public ProductDetailsService(ProductDetailsRepo productDetailsRepo) {
        this.productDetailsRepo = productDetailsRepo;
    }

    public Productdetails findByBatchCode(String batchCode) {
         return productDetailsRepo.findByBatchCode(batchCode);
    }
}
