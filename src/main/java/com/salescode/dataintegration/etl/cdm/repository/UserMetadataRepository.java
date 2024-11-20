//package com.salescode.dataintegration.etl.cdm.repository;
//
//import com.applicate.services.channelkart.models.UserMetadata;
//
//import com.salescode.dataintegration.etl.cdm.enums.UserMetadataType;
//import com.salescode.jooq.generated.tables.pojos.CkUserMetadata;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
//
//@Repository
//public interface UserMetadataRepository {
//
//    List<CkUserMetadata> getByTypeAndValue(UserMetadataType type, String value);
//
//    CkUserMetadata getByLoginIdAndTypeAndValue(String loginId, UserMetadataType type, String value);
//
//    List<CkUserMetadata> getByLoginId(String loginId);
//
//
//}
