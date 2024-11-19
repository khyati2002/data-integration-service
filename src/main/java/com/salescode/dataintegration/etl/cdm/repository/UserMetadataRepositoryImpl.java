//package com.salescode.dataintegration.etl.cdm.repository;
//
//import com.salescode.dataintegration.etl.cdm.enums.UserMetadataType;
//import com.salescode.jooq.generated.tables.pojos.CkUserMetadata;
//import org.jooq.DSLContext;
//
//import java.util.List;
//import static com.salescode.jooq.generated.tables.CkUserMetadata.CK_USER_METADATA;
//public class UserMetadataRepositoryImpl implements  UserMetadataRepository{
//
//    private final DSLContext dsl;
//
//
//    public UserMetadataRepositoryImpl(DSLContext dsl) {
//        this.dsl = dsl;
//    }
//
//    @Override
//    public List<CkUserMetadata> getByTypeAndValue(UserMetadataType type, String value) {
//        return dsl.select()
//                .from(CK_USER_METADATA)
//                .where(CK_USER_METADATA.TYPE.eq(type.name()))  // assuming `type` column is a string that stores the enum name
//                .and(CK_USER_METADATA.VALUE.eq(value))
//                .fetchInto(CkUserMetadata.class);
//    }
//
//    @Override
//    public CkUserMetadata getByLoginIdAndTypeAndValue(String loginId, UserMetadataType type, String value) {
//        return dsl.select()
//                .from(CK_USER_METADATA)
//                .where(CK_USER_METADATA.LOGINID.eq(loginId))  // Assuming `LOGIN_ID` is the column name
//                .and(CK_USER_METADATA.TYPE.eq(type.name()))    // Assuming `TYPE` is a column storing the enum's name
//                .and(CK_USER_METADATA.VALUE.eq(value))        // Assuming `VALUE` is the column storing the value
//                .fetchOneInto(CkUserMetadata.class);
//    }
//
//    @Override
//    public List<CkUserMetadata> getByLoginId(String loginId) {
//       return List.of();
//    }
//}
