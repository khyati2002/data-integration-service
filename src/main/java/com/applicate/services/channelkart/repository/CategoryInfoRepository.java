package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.impl.CategoryInfo;
import org.jooq.DSLContext;

import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.*;

public class CategoryInfoRepository {
    private final DSLContext dsl;

    public CategoryInfoRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<CategoryInfo> findByCategoryCodeAndCategoryValueAndFeature(String categoryCode, String categoryValue, String feature) {
        return dsl.selectFrom(CK_CATEGORY_INFO)
                .where(CK_CATEGORY_INFO.CATEGORY_CODE.eq(categoryCode))
                .and(CK_CATEGORY_INFO.CATEGORY_VALUE.eq(categoryValue))
                .and(CK_CATEGORY_INFO.FEATURE.eq(feature))
                .fetchInto(CategoryInfo.class);
    }

    public List<CategoryInfo> findByCategoryCodeAndCategoryValueAndName(String categoryCode, String categoryValue, String name) {
        return dsl.selectFrom(CK_CATEGORY_INFO)
                .where(CK_CATEGORY_INFO.CATEGORY_CODE.eq(categoryCode))
                .and(CK_CATEGORY_INFO.CATEGORY_VALUE.eq(categoryValue))
                .and(CK_CATEGORY_INFO.NAME.eq(name))
                .fetchInto(CategoryInfo.class);
    }

    public List<CategoryInfo> findByNameAndFeature(String name, String feature) {
        return dsl.selectFrom(CK_CATEGORY_INFO)
                .where(CK_CATEGORY_INFO.NAME.eq(name))
                .and(CK_CATEGORY_INFO.FEATURE.eq(feature))
                .fetchInto(CategoryInfo.class);
    }

    public CategoryInfo findByCategoryCode(String categoryCode) {
        return dsl.selectFrom(CK_CATEGORY_INFO)
                .where(CK_CATEGORY_INFO.CATEGORY_CODE.eq(categoryCode))
                .fetchOneInto(CategoryInfo.class);
    }

    public List<CategoryInfo> findByCategoryCodeAndFeature(String categoryCode, String feature) {
        return dsl.selectFrom(CK_CATEGORY_INFO)
                .where(CK_CATEGORY_INFO.CATEGORY_CODE.eq(categoryCode))
                .and(CK_CATEGORY_INFO.FEATURE.eq(feature))
                .fetchInto(CategoryInfo.class);
    }
}