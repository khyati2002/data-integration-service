/*
 * Copyright (c) Applicate 2021. All rights reserved.
 * Use is subject to license terms.
 */
package com.salescode.channelkart.repository;

//import com.applicate.services.channelkart.models.User;
//import com.applicate.services.channelkart.models.UserMessengerInfo;
//import com.applicate.services.channelkart.models.enums.ActiveStatus;

import com.salescode.channelkart.models.enums.ActiveStatus;

import com.salescode.jooq.generated.tables.pojos.CkUser;
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

//    @Procedure(name = "user_hierarchy_procedure")
//    void executeProcedure(@Param("loginids") String loginId);
//
//    @Procedure(name = "all_user_hierarchy_procedure")
//    void executeProcedure();
//
//    @Modifying
//    @Query("update User u set userContext = ?2 where loginId = ?1")
    int updateUserContext(String loginId,String userContext);
//
//    @Modifying
//    @Query("update User u set deviceId = ?2 where loginId = ?1")
    int updateDeviceId(String loginId,String deviceId);
//
//    @Modifying
//    @Query("update User u set userContext = ?2, deviceId = ?3 where loginId = ?1")
     int updateUserContextAndDevideId(String loginId,String userContext,String deviceId);
//
//    @Query("select u.userContext from User u where u.loginId = ?1")
     Optional<String> getUserContext(String loginId);
//

  //  @Query("select new com.applicate.services.channelkart.sync.schduler.UserNameAndContext(u.loginId, u.userContext)  from User u where u.userContext is not null")
   // List<UserNameAndContext> getUserContexts();
//
//    @Query("select new com.applicate.services.channelkart.sync.schduler.UserNameAndContext(u.loginId, u.userContext)  from User u where u.userContext is not null and u.loginId in (?1)")
     // List<UserNameAndContext> getUserContexts(List<String> loginIds);
//
//    @Query("select u.loginId as loginId, u.verified as verified from User u where u.loginId in (?1)")
       List<Map<String, Object>> getUserAndVerification(List<String> loginIds);
//
       List<CkUser> findByMobile(String mobile);
//
       List<CkUser> findByEmail(String email);
//
//    @Query("select u from User u where u.mobile = ?1 and u.activeStatus in ('active','1')")
      List<CkUser> findByActiveMobile(String mobile);
//
//    @Query("select u from User u where u.email = ?1 and u.activeStatus in ('active','1')")
        List<CkUser> findByActiveEmail(String email);
//
        CkUser findByFacebookPSID(String facebookPSID);
//
//    /**
//     * Find by messenger info channel id.
//     *
//     * @param channelId the channel id
//     * @return the user
//     */
       CkUser findByMessengerInfoChannelId(String channelId);
//
//    /**
//     * Find by login id and messenger info channel.
//     *
//     * @param loginId the login id
//     * @param channel the channel
//     * @return the user
//     */
    public CkUser findByLoginIdAndMessengerInfoChannel(String loginId, String channel);
//
//    @Query("select mi from User u inner join u.messengerInfo mi where u.loginId = ?1 and mi.channel = ?2")
    public List<CkUserMessengerInfo> getMessengerInfos(String loginId, String channel);
//
     List<CkUser> findByUserContext(String token);
//
      public Long countByDesignationIs(String designation);
//
      List<CkUser> findUserContextAndLoginIdByLoginIdIn(List<String> loginId);
//
      List<CkUser> findByMobileIn(List<String> mobileNumbers);
//
//    @Modifying
//    @Query("update User u set blocked=?2, hash = ?3 where loginId = ?1")
      void updateBlocked(String loginId,boolean blocked, String hash);
//
//    @Modifying
//    @Query("update User u set verified=?2,blocked = ?3, hash = ?4 where loginId = ?1")
      int updateVerified(String loginId,boolean verified,boolean blocked,String hash);
//
//    @Modifying
//    @Query("update User u set activeStatus = ?2, activeStatusReason = ?3 where loginId = ?1")
    int updateActiveStatusAndReason(String loginId, ActiveStatus activeStatus, String activeStatusReason);
//
//    @Query(nativeQuery = true,value = "select u.loginId from ck_user u inner join ck_userdesignation ud on ud.login_id = u.loginid where ud.designation !=  'retailer' and loginid in (?1)")
      List<String> getExistingUsers(List<String> loginIdList);
//
//    @Query(nativeQuery = true,value = "select loginid,hierarchy from ck_user where loginid in (?1)")
      List<Map<String,Object>> getUserHierarchy(List<String> loginIds);
//
//    @Query("select loginId from User u  where u.externalReferenceId = ?1")
      Optional<String> findLoginIdByReferenceId(String externalReferenceId);
//
//    @Transactional
//    @Modifying
//    @Query("update User u set report_password = ?2 where loginId = ?1")
    int updateReportPassword(String loginid, String reportPassword);
}
