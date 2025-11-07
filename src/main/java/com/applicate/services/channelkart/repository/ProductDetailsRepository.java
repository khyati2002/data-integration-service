package com.applicate.services.channelkart.repository;

import org.jooq.DSLContext;

import java.util.List;

import static com.salescode.dim.jooq.generated.tables.CkProductdetails.CK_PRODUCTDETAILS;

public class ProductDetailsRepository {

	private final DSLContext dsl;

	public ProductDetailsRepository(DSLContext dsl) {
		this.dsl = dsl;
	}

	public List<String> batchCodeExists(String batchCode) {
		return dsl.select(CK_PRODUCTDETAILS.BATCH_CODE).from(CK_PRODUCTDETAILS).where(CK_PRODUCTDETAILS.BATCH_CODE.eq(batchCode)).fetch(CK_PRODUCTDETAILS.BATCH_CODE);
	}
}
