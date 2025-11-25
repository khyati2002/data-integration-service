package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.repository.ProductDetailsRepository;
import com.salescode.dim.jooq.generated.tables.pojos.Productdetails;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ProductDetailsService extends AbstractCDMService<Productdetails> {

    private static ProductDetailsRepository productDetailsRepository ;

    public ProductDetailsService(){
        if(productDetailsRepository==null)
            productDetailsRepository = new ProductDetailsRepository(getDslContext());
    }


    public boolean checkIfBatchCodeExists(String batchCode){
        return productDetailsRepository.existsByBatchCode(batchCode);
    }

    public Productdetails findByBatchCode(String batchCode){
        return productDetailsRepository.findByBatchCode(batchCode);
    }

    public List<String> findSKUCodesByCondition(String whereClause) {
        if (StringUtils.isBlank(whereClause)) {
            return Collections.emptyList();
        }

        String query = "SELECT sku_code FROM ck_productdetails WHERE " + whereClause;

        try {
            return getDslContext()
                    .fetch(query)
                    .map(record -> record.get("sku_code", String.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch SKU codes", e);
        }
    }
    public List<Map<String, Object>> executeQueryForMultipleColumns(String query) {
        if (StringUtils.isBlank(query)) {
            return Collections.emptyList();
        }

        try {
            return getDslContext()
                    .fetch(query)
                    .intoMaps();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute query: " + query, e);
        }
    }

}
