package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.impl.SequenceInfo;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;

import static com.salescode.dim.jooq.generated.Tables.CK_SEQUENCE_INFO;

public class SequenceInfoRepository {
    private final DSLContext dsl;

    public SequenceInfoRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public SequenceInfo findByEntityAndFieldNameAndType(String entity, String fieldName, String type) {
        return dsl.selectFrom(CK_SEQUENCE_INFO)
                .where(CK_SEQUENCE_INFO.ENTITY.eq(entity))
                .and(CK_SEQUENCE_INFO.FIELD_NAME.eq(fieldName))
                .and(CK_SEQUENCE_INFO.TYPE.eq(type))
                .fetchOneInto(SequenceInfo.class);
    }

    public String executeSequenceProcedure(String seqName) {
        Result<Record> result = dsl.fetch("CALL getnextval(?, @sequencenumber)", seqName);
        // Depending on your DB, you may need to query the variable back:
        Record output = dsl.fetchOne("SELECT @sequencenumber AS sequencenumber");
        return output != null ? output.get("sequencenumber", String.class) : null;
    }

    public int createSequenceProcedure(String seqName, Long startValue) {
        dsl.execute("CALL createsequence(?, ?)", seqName, startValue);
        return 1; // indicate success
    }
}
