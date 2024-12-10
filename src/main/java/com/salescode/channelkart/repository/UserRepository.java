/*
 * Copyright (c) Applicate 2021. All rights reserved.
 * Use is subject to license terms.
 */
package com.salescode.channelkart.repository;

import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.jooq.impl.CkUser;
import com.salescode.jooq.generated.tables.pojos.CkUserMessengerInfo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface UserRepository {

    CkUser findByLoginId(String loginId);

    List<CkUser> findByLoginIdIn(List<String> loginId);

    CkUser findByHierarchy(String hierarchy);

    int updateUserContext(String loginId, String userContext);

    int updateDeviceId(String loginId, String deviceId);

    int updateUserContextAndDevideId(String loginId, String userContext, String deviceId);

    Optional<String> getUserContext(String loginId);

    List<Map<String, Object>> getUserAndVerification(List<String> loginIds);

    List<CkUser> findByMobile(String mobile);

    List<CkUser> findByEmail(String email);

    List<CkUser> findByActiveMobile(String mobile);

    List<CkUser> findByActiveEmail(String email);

    CkUser findByFacebookPSID(String facebookPSID);

    CkUser findByMessengerInfoChannelId(String channelId);

    CkUser findByLoginIdAndMessengerInfoChannel(String loginId, String channel);

    List<CkUserMessengerInfo> getMessengerInfos(String loginId, String channel);

    List<CkUser> findByUserContext(String token);

    Long countByDesignationIs(String designation);

    List<CkUser> findUserContextAndLoginIdByLoginIdIn(List<String> loginId);

    List<CkUser> findByMobileIn(List<String> mobileNumbers);

    void updateBlocked(String loginId, boolean blocked, String hash);

     int updateVerified(String loginId, boolean verified, boolean blocked, String hash);

     int updateActiveStatusAndReason(String loginId, ActiveStatus activeStatus, String activeStatusReason);

    List<String> getExistingUsers(List<String> loginIdList);

    List<Map<String, Object>> getUserHierarchy(List<String> loginIds);

    Optional<String> findLoginIdByReferenceId(String externalReferenceId);

    int updateReportPassword(String loginid, String reportPassword);
}
