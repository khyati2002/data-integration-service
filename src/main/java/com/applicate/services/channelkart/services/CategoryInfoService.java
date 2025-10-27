package com.applicate.services.channelkart.services;

import com.salescode.dim.jooq.impl.CategoryInfo;
import com.applicate.services.channelkart.repository.CategoryInfoRepository;
import java.util.List;
import java.util.Optional;

public class CategoryInfoService extends AbstractCDMService<CategoryInfo>{

    private CategoryInfoRepository categoryInfoRepository;

    public CategoryInfoService() {
        super();
        this.categoryInfoRepository = new CategoryInfoRepository(getDslContext());
    }

    public Optional<CategoryInfo> findCategoryById(String id) {
        return categoryInfoRepository.findById(Long.valueOf(id));
    }

    public List<CategoryInfo> findByCategoryCodeAndCategoryValueAndFeature(String categoryCode, String categoryValue, String feature) {
        return categoryInfoRepository.findByCategoryCodeAndCategoryValueAndFeature(categoryCode, categoryValue, feature);

    }
    public CategoryInfo findByCategoryCode(String categoryCode) {
        return categoryInfoRepository.findByCategoryCode(categoryCode);

    }
    public List<CategoryInfo> findByCategoryCodeAndFeature(String categoryCode, String feature) {
        return this.categoryInfoRepository.findByCategoryCodeAndFeature(categoryCode, feature);
    }

    public List<CategoryInfo> findByCategoryCodeAndCategoryValueAndName(String categoryCode, String categoryValue, String name) {
        return categoryInfoRepository.findByCategoryCodeAndCategoryValueAndName(categoryCode, categoryValue, name);

    }
    public List<CategoryInfo> findByNameandFeature(String name, String feature) {
        return categoryInfoRepository.findByNameAndFeature(name, feature);

    }


}

