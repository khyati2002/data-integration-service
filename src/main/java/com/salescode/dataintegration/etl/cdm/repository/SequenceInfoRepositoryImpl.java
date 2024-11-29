package com.salescode.dataintegration.etl.cdm.repository;

import com.salescode.jooq.generated.tables.pojos.CkSequenceInfo;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static com.salescode.jooq.generated.Tables.CK_SEQUENCE_INFO;

@Repository
public class SequenceInfoRepositoryImpl implements SequenceInfoRepository{

    private final DSLContext dsl;

    public SequenceInfoRepositoryImpl(DSLContext dsl){
        this.dsl = dsl;
    }

    @Override
    public CkSequenceInfo findByEntityAndFieldNameAndType(String entity, String fieldName, String type) {
        return dsl.selectFrom(CK_SEQUENCE_INFO)
                .where(CK_SEQUENCE_INFO.ENTITY.eq(entity))
                .and(CK_SEQUENCE_INFO.FIELD_NAME.eq(fieldName))
                .and(CK_SEQUENCE_INFO.TYPE.eq(type))
                .fetchOneInto(CkSequenceInfo.class);
    }

    @Override
    public String executeSequenceProcedures(String seqname) {
            // Assuming the stored procedure returns a Long/BigInteger
            return dsl.resultQuery("CALL getnextval(?, @sequencenumber)", seqname)
                    .fetchOneInto(String.class);

    }

    @Override
    public int createSequenceProcedure(String seqname, Long startvalue) {
        return dsl.resultQuery("CALL createsequence(?, ?)", seqname, startvalue)
                .execute();
    }
}
