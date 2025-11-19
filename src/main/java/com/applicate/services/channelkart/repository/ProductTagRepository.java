package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.generated.tables.pojos.Producttag;
import org.jooq.DSLContext;

import java.util.List;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_PRODUCTTAG;

public class ProductTagRepository {

    private final DSLContext dsl;

    public ProductTagRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Producttag save(Producttag productTag) {
        if (productTag.getId() == null) {
            productTag.setId(java.util.UUID.randomUUID().toString());
            dsl.insertInto(CK_PRODUCTTAG)
                    .set(CK_PRODUCTTAG.ID, productTag.getId())
                    .set(CK_PRODUCTTAG.ACTIVE_STATUS, productTag.getActiveStatus())
                    .set(CK_PRODUCTTAG.ACTIVE_STATUS_REASON, productTag.getActiveStatusReason())
                    .set(CK_PRODUCTTAG.CREATED_BY, productTag.getCreatedBy())
                    .set(CK_PRODUCTTAG.CREATION_TIME, productTag.getCreationTime())
                    .set(CK_PRODUCTTAG.EXTENDED_ATTRIBUTES, productTag.getExtendedAttributes())
                    .set(CK_PRODUCTTAG.HASH, productTag.getHash())
                    .set(CK_PRODUCTTAG.LAST_MODIFIED_TIME, productTag.getLastModifiedTime())
                    .set(CK_PRODUCTTAG.LOB, productTag.getLob())
                    .set(CK_PRODUCTTAG.MODIFIED_BY, productTag.getModifiedBy())
                    .set(CK_PRODUCTTAG.SOURCE, productTag.getSource())
                    .set(CK_PRODUCTTAG.VERSION, productTag.getVersion())
                    .set(CK_PRODUCTTAG.COUNTRY_CODE, productTag.getCountryCode())
                    .set(CK_PRODUCTTAG.END_DATE, productTag.getEndDate())
                    .set(CK_PRODUCTTAG.START_DATE, productTag.getStartDate())
                    .set(CK_PRODUCTTAG.TAG_CODE, productTag.getTagCode())
                    .set(CK_PRODUCTTAG.TAG_DESCRIPTION, productTag.getTagDescription())
                    .set(CK_PRODUCTTAG.OUTLET_CODE, productTag.getOutletCode())
                    .set(CK_PRODUCTTAG.SKU_CODE, productTag.getSkuCode())
                    .set(CK_PRODUCTTAG.PRODUCT_TYPE, productTag.getProductType())
                    .set(CK_PRODUCTTAG.PRODUCT_VALUE, productTag.getProductValue())
                    .set(CK_PRODUCTTAG.TAG_GROUP, productTag.getTagGroup())
                    .set(CK_PRODUCTTAG.CHANGED, productTag.getChanged())
                    .execute();
        } else {
            dsl.update(CK_PRODUCTTAG)
                    .set(CK_PRODUCTTAG.ACTIVE_STATUS, productTag.getActiveStatus())
                    .set(CK_PRODUCTTAG.ACTIVE_STATUS_REASON, productTag.getActiveStatusReason())
                    .set(CK_PRODUCTTAG.LAST_MODIFIED_TIME, productTag.getLastModifiedTime())
                    .set(CK_PRODUCTTAG.MODIFIED_BY, productTag.getModifiedBy())
                    .set(CK_PRODUCTTAG.VERSION, productTag.getVersion())
                    .set(CK_PRODUCTTAG.END_DATE, productTag.getEndDate())
                    .set(CK_PRODUCTTAG.START_DATE, productTag.getStartDate())
                    .set(CK_PRODUCTTAG.TAG_CODE, productTag.getTagCode())
                    .set(CK_PRODUCTTAG.TAG_DESCRIPTION, productTag.getTagDescription())
                    .set(CK_PRODUCTTAG.OUTLET_CODE, productTag.getOutletCode())
                    .set(CK_PRODUCTTAG.SKU_CODE, productTag.getSkuCode())
                    .set(CK_PRODUCTTAG.PRODUCT_TYPE, productTag.getProductType())
                    .set(CK_PRODUCTTAG.PRODUCT_VALUE, productTag.getProductValue())
                    .set(CK_PRODUCTTAG.TAG_GROUP, productTag.getTagGroup())
                    .set(CK_PRODUCTTAG.CHANGED, productTag.getChanged())
                    .where(CK_PRODUCTTAG.ID.eq(productTag.getId()))
                    .execute();
        }
        return productTag;
    }

    public Producttag refresh(Producttag productTag) {
        return dsl.selectFrom(CK_PRODUCTTAG)
                .where(CK_PRODUCTTAG.ID.eq(productTag.getId()))
                .fetchOneInto(Producttag.class);
    }

    public List<Producttag> findByTagGroup(String tagGroup) {
        return dsl.selectFrom(CK_PRODUCTTAG)
                .where(CK_PRODUCTTAG.TAG_GROUP.eq(tagGroup))
                .fetchInto(Producttag.class);
    }

    public List<Producttag> findByOutlet(String outletCode) {
        return dsl.selectFrom(CK_PRODUCTTAG)
                .where(CK_PRODUCTTAG.OUTLET_CODE.eq(outletCode))
                .fetchInto(Producttag.class);
    }

    public List<Producttag> findBySkuCodeAndTagGroup(String skuCode, String tagGroup) {
        return dsl.selectFrom(CK_PRODUCTTAG)
                .where(CK_PRODUCTTAG.SKU_CODE.eq(skuCode))
                .and(CK_PRODUCTTAG.TAG_GROUP.eq(tagGroup))
                .fetchInto(Producttag.class);
    }

    public int delete(List<Producttag> productTags) {
        if (productTags == null || productTags.isEmpty()) {
            return 0;
        }
        List<String> ids = productTags.stream()
                .map(Producttag::getId)
                .collect(Collectors.toList());
        return dsl.deleteFrom(CK_PRODUCTTAG)
                .where(CK_PRODUCTTAG.ID.in(ids))
                .execute();
    }

    public int deleteAll(List<Producttag> productTags) {
        return delete(productTags);
    }

    public Producttag findById(String id) {
        return dsl.selectFrom(CK_PRODUCTTAG)
                .where(CK_PRODUCTTAG.ID.eq(id))
                .fetchOneInto(Producttag.class);
    }

    public Integer getVersionById(String id) {
        return dsl.select(CK_PRODUCTTAG.VERSION)
                .from(CK_PRODUCTTAG)
                .where(CK_PRODUCTTAG.ID.eq(id))
                .fetchOneInto(Integer.class);
    }
}