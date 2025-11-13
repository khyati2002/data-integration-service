package com.applicate.services.channelkart.repository;

import org.jooq.*;
import org.jooq.impl.DSL;
import java.util.*;
import static com.salescode.dim.jooq.generated.Tables.CK_CATEGORY_INFO;
import static com.salescode.dim.jooq.generated.Tables.CK_PRODUCTDETAILS;

public class CategoryInfoRepository {

    private final DSLContext dsl;

    public CategoryInfoRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Fetch batch codes from ck_productdetails by matching category filters.
     */
    public List<String> findBatchCodesByCategoryFilters(Map<String, String> freeProductCategoryMap) {

        // Start with no conditions
        Condition condition = DSL.trueCondition();

        for (Map.Entry<String, String> entry : freeProductCategoryMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // Decide which category_code to use based on key
            String categoryCode = null;
            switch (key.toLowerCase()) {
                case "item_type":
                    categoryCode = "1";
                    break;
                case "brand":
                    categoryCode = "2";
                    break;
                case "piece_size_desc":
                    categoryCode = "3";
                    break;
                case "piece_size":
                    categoryCode = "4";
                    break;
                default:
                    categoryCode = null;
                    break;
            }

            if (categoryCode != null) {
                // Build subquery for that category
                Select<Record1<String>> subQuery = dsl
                        .select(CK_CATEGORY_INFO.NEW_DESCRIPTION)
                        .from(CK_CATEGORY_INFO)
                        .where(CK_CATEGORY_INFO.CATEGORY_CODE.eq(categoryCode))
                        .and(CK_CATEGORY_INFO.NAME.eq("ProductMapping"))
                        .and(CK_CATEGORY_INFO.CATEGORY_VALUE.eq(value));

                condition = condition.and(DSL.field(DSL.name(key)).in(subQuery));
            }
        }

        // Execute main query
        return dsl.select(CK_PRODUCTDETAILS.BATCH_CODE)
                .from(CK_PRODUCTDETAILS)
                .where(condition)
                .fetch(CK_PRODUCTDETAILS.BATCH_CODE);
    }
}

