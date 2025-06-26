package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.generated.tables.pojos.ScoreDetails;
import org.jooq.DSLContext;


import java.util.List;

import static com.salescode.dim.jooq.generated.Tables.CK_SCORE_DETAILS;


public class ScoreDetailsRepository {

	private final DSLContext dsl;

	public ScoreDetailsRepository(DSLContext dsl) {
		this.dsl = dsl;
	}


	public int deleteByOutletCodeAndLoginIdAndProgramNumber(String outletCode, String loginId, String programNumber) {
		return dsl.deleteFrom(CK_SCORE_DETAILS)
				.where(CK_SCORE_DETAILS.OUTLETCODE.eq(outletCode))
				.and(CK_SCORE_DETAILS.LOGINID.eq(loginId))
				.and(CK_SCORE_DETAILS.PROGRAM_NUMBER.eq(programNumber))
				.execute();
	}


	public int deleteByOutletCode(String outletCode) {
		return dsl.deleteFrom(CK_SCORE_DETAILS)
				.where(CK_SCORE_DETAILS.OUTLETCODE.eq(outletCode))
				.execute();
	}

	public List<ScoreDetails> findByOutletCodeAndLoginIdAndProgramNumber(String outletCode, String loginId, String programNumber) {
		return dsl.selectFrom(CK_SCORE_DETAILS)
				.where(CK_SCORE_DETAILS.OUTLETCODE.eq(outletCode))
				.and(CK_SCORE_DETAILS.LOGINID.eq(loginId))
				.and(CK_SCORE_DETAILS.PROGRAM_NUMBER.eq(programNumber))
				.fetchInto(ScoreDetails.class);
	}

	public ScoreDetails findByLoginId(String loginid) {
		return dsl.selectFrom(CK_SCORE_DETAILS)
				.where(CK_SCORE_DETAILS.LOGINID.eq(loginid))
				.fetchOneInto(ScoreDetails.class);
	}


	public Integer getVersionById(String id) {
		return dsl.select(CK_SCORE_DETAILS.VERSION)
				.from(CK_SCORE_DETAILS)
				.where(CK_SCORE_DETAILS.ID.eq(id))
				.fetchOneInto(Integer.class);
	}
}
