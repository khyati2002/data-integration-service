package com.applicate.services.channelkart.repository;

import org.jooq.DSLContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.salescode.dim.jooq.impl.ApprovalInfo;

import static com.salescode.dim.jooq.generated.Tables.CK_APPROVAL_INFO;

public class ApprovalInfoRepository {
	private final DSLContext dsl;

	public ApprovalInfoRepository(DSLContext dsl) {
		this.dsl = dsl;
	}

	//	public List<com.salescode.dim.jooq.impl.ApprovalInfo> findByReferenceIdIn(Collection<String> referenceIds) {
//
//		List approval = dsl.selectFrom(CK_APPROVAL_INFO).where(CK_APPROVAL_INFO.REFERENCE_ID.in(referenceIds)).fetchInto(com.salescode.dim.jooq.generated.tables.pojos.ApprovalInfo.class);
//		return com.salescode.dim.jooq.impl.ApprovalInfo.of(approval);
//	}
	public List<ApprovalInfo> findByReferenceIdIn(Collection<String> referenceIds) {
		// Step 1: Fetch POJO list from database
		List<com.salescode.dim.jooq.generated.tables.pojos.ApprovalInfo> approvalPojos = dsl.selectFrom(CK_APPROVAL_INFO).where(CK_APPROVAL_INFO.REFERENCE_ID.in(referenceIds)).fetchInto(com.salescode.dim.jooq.generated.tables.pojos.ApprovalInfo.class);

		// Step 2: Create a new list for impl.ApprovalInfo
		List<ApprovalInfo> approvalInfos = new ArrayList<>();

		// Step 3: Convert and add each object
		for (com.salescode.dim.jooq.generated.tables.pojos.ApprovalInfo pojo : approvalPojos) {
			approvalInfos.add(ApprovalInfo.of(pojo));
		}

		// Step 4: Return final list
		return approvalInfos;
	}

}
