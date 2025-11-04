/*
*Copyright Applicate(2021) To Present
*
*All rights reserved
*/
package com.applicate.services.channelkart.repository;


import com.salescode.dim.jooq.impl.SequenceInfo;
import org.jooq.DSLContext;

import static com.salescode.dim.jooq.generated.tables.CkSequenceInfo.CK_SEQUENCE_INFO;

public class SequenceInfoRepository {
    private final DSLContext dslContext;

    public SequenceInfoRepository(DSLContext dsl){
        this.dslContext=dsl;
    }
	public SequenceInfo findByEntityAndFieldNameAndType(String entity, String fieldName, String type){
        return dslContext.selectFrom(CK_SEQUENCE_INFO)
                .where(CK_SEQUENCE_INFO.ENTITY.eq(entity))
                .and(CK_SEQUENCE_INFO.FIELD_NAME.eq(fieldName))
                .and(CK_SEQUENCE_INFO.TYPE.eq(type))
                .fetchOneInto(SequenceInfo.class);
    }


}
