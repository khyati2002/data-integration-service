package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.generated.tables.pojos.ApprovalInfo;
import org.jooq.DSLContext;

import java.util.Collection;
import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.CK_APPROVAL_INFO;

public class ApprovalInfoRepository {
	private final DSLContext dsl;

	public ApprovalInfoRepository(DSLContext dsl) {
		this.dsl = dsl;
	}

	public List<ApprovalInfo> findByReferenceIdIn(Collection<String> referenceIds) {
		return dsl.selectFrom(CK_APPROVAL_INFO)
				.where(CK_APPROVAL_INFO.REFERENCE_ID.in(referenceIds))
				.fetchInto(ApprovalInfo.class);
	}
}
