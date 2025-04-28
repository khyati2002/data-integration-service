package com.applicate.services.channelkart.repository;

import com.salescode.dim.cache.Cacheable;

import org.jooq.DSLContext;


import java.time.LocalDate;
import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.CK_REDEEM_ACTIVITY;

public class RedeemActivityRepository {

	private final DSLContext dsl;

	public RedeemActivityRepository(DSLContext dsl) {
		this.dsl = dsl;
	}

	public List<String> findPendingRedemptionIds() {
		return dsl.select(CK_REDEEM_ACTIVITY.REDEEM_ID).from(CK_REDEEM_ACTIVITY).where(CK_REDEEM_ACTIVITY.STATUS.eq("pending")).fetchInto(String.class);
	}
	public List<String> redeemActivityStatus() {
		return dsl.select(CK_REDEEM_ACTIVITY.REDEEM_ID)
				.from(CK_REDEEM_ACTIVITY)
				.where(CK_REDEEM_ACTIVITY.STATUS.eq("pending"))
				.fetchInto(String.class);
	}



}
