package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.repository.ProductDetailsRepository;
import com.salescode.dim.jooq.generated.tables.pojos.Productdetails;

public class ProductDetailsService extends AbstractCDMService<Productdetails> {

    ProductDetailsRepository productDetailsRepository ;

    public ProductDetailsService(){
        if(productDetailsRepository==null)
            productDetailsRepository = new ProductDetailsRepository(getDslContext());
    }

    public boolean checkIfBatchCodeExists(String batchCode){
        return productDetailsRepository.existsByBatchCode(batchCode);
    }
}
