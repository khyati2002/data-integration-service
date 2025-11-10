package com.applicate.services.channelkart.repository;

import com.salescode.dim.jooq.impl.UserMetadata;
import org.jooq.DSLContext;
import java.util.List;
import static com.salescode.dim.jooq.generated.Tables.CK_USER_METADATA;

public class UserMetadataRepository {
    private final DSLContext dsl;

    public UserMetadataRepository(DSLContext dsl){
        this.dsl = dsl;
    }

    public List<UserMetadata> getByTypeAndValue(String type, String value) {
        return dsl.selectFrom(CK_USER_METADATA)
                .where(CK_USER_METADATA.TYPE.eq(type))
                .and(CK_USER_METADATA.VALUE.eq(value))
                .fetchInto(UserMetadata.class);
    }

    public UserMetadata getByLoginIdAndTypeAndValue(String loginId, String type, String value) {
        return dsl.selectFrom(CK_USER_METADATA)
                .where(CK_USER_METADATA.LOGINID.eq(loginId))
                .and(CK_USER_METADATA.TYPE.eq(type))
                .and(CK_USER_METADATA.VALUE.eq(value))
                .fetchOneInto(UserMetadata.class);
    }

    public List<UserMetadata> getByLoginId(String loginId) {
        return dsl.selectFrom(CK_USER_METADATA)
                .where(CK_USER_METADATA.LOGINID.eq(loginId))
                .fetchInto(UserMetadata.class);
    }
}