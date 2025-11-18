package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.repository.ProductTagRepository;
import com.salescode.dim.jooq.generated.tables.pojos.Producttag;

import java.util.List;

public class ProductTagService extends AbstractCDMService<Producttag> {

    private ProductTagRepository productTagRepository;

    public ProductTagService() {
        if (productTagRepository == null) {
            productTagRepository = new ProductTagRepository(getDslContext());
        }
    }

    public Producttag save(Producttag producttag) {
        return productTagRepository.save(producttag);
    }

    public Producttag refresh(Producttag producttag) {
        return productTagRepository.refresh(producttag);
    }

    public List<Producttag> findByTagGroup(String tagGroup) {
        return productTagRepository.findByTagGroup(tagGroup);
    }

    public List<Producttag> findByOutlet(String outletCode) {
        return productTagRepository.findByOutlet(outletCode);
    }

    public List<Producttag> findBySkuCodeAndTagGroup(String skuCode, String tagGroup) {
        return productTagRepository.findBySkuCodeAndTagGroup(skuCode, tagGroup);
    }

    public int delete(List<Producttag> productTags) {
        return productTagRepository.delete(productTags);
    }

    public int deleteAll(List<Producttag> productTags) {
        return productTagRepository.deleteAll(productTags);
    }

    public Producttag findById(String id) {
        return productTagRepository.findById(id);
    }

    public Integer getVersionById(String id) {
        return productTagRepository.getVersionById(id);
    }
}