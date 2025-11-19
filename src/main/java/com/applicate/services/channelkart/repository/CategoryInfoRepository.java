package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.impl.CategoryInfo;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import org.jooq.DSLContext;

import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.CK_CATEGORY_INFO;

public class CategoryInfoRepository {

    private final DSLContext dsl;

    public CategoryInfoRepository(DSLContext dsl){
        this.dsl = dsl;
    }

    public List<CategoryInfo> findByCategoryCodeAndFeature(String categoryCode, String feature) {
        return dsl.selectFrom(CK_CATEGORY_INFO)
                .where(CK_CATEGORY_INFO.CATEGORY_CODE.eq(categoryCode).and(CK_CATEGORY_INFO.FEATURE.eq(feature)))
                .fetchInto(CategoryInfo.class);

    }



}
