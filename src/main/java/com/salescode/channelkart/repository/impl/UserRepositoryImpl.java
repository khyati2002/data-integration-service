package com.salescode.channelkart.repository.impl;

import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.channelkart.repository.UserRepository;
import com.salescode.jooq.generated.tables.pojos.CkUser;
import com.salescode.jooq.generated.tables.pojos.CkUserMessengerInfo;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.salescode.jooq.generated.tables.CkToken.CK_TOKEN;
import static com.salescode.jooq.generated.tables.CkUser.CK_USER;
import static com.salescode.jooq.generated.tables.CkUserMessengerInfo.CK_USER_MESSENGER_INFO;
import static com.salescode.jooq.generated.tables.CkUserdesignation.CK_USERDESIGNATION;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final DSLContext dsl;


    public UserRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public CkUser findByLoginId(String loginID) {
        return dsl.selectFrom(CK_USER)
                .where(CK_USER.LOGINID.eq(loginID)) // Replace LOGIN_ID with the actual field name in your table
                .fetchOneInto(CkUser.class);

    }

    @Override
    public List<CkUser> findByLoginIdIn(List<String> loginId) {
        return dsl.selectFrom(CK_USER)
                .where(CK_USER.LOGINID.in(loginId)) // `in` condition for matching multiple login IDs
                .fetchInto(CkUser.class); // Maps each record to `CkUser` class

    }

    @Override
    public CkUser findByHierarchy(String hierarchy) {
        return dsl.selectFrom(CK_USER)
                .where(CK_USER.HIERARCHY.eq(hierarchy)) // Replace HIERARCHY with the actual field name in your table
                .fetchOneInto(CkUser.class);
    }

    @Override
    public int updateUserContext(String loginId, String userContext) {
        return dsl.update(CK_USER)
                .set(CK_USER.USERCONTEXT, userContext)
                .where(CK_USER.LOGINID.eq(loginId))
                .execute();
    }

    @Override
    public int updateDeviceId(String loginId, String deviceId) {
        return dsl.update(CK_USER)
                .set(CK_USER.DEVICE_ID, deviceId)
                .where(CK_USER.LOGINID.eq(loginId))
                .execute();
    }

    @Override
    public int updateUserContextAndDevideId(String loginId, String userContext, String deviceId) {
        return dsl.update(CK_USER)
                .set(CK_USER.USERCONTEXT, userContext)
                .set(CK_USER.DEVICE_ID, deviceId)
                .where(CK_USER.LOGINID.eq(loginId))
                .execute();
    }

    @Override
    public Optional<String> getUserContext(String loginId) {
        return dsl.select(CK_USER.USERCONTEXT)
                .from(CK_USER)
                .where(CK_USER.LOGINID.eq(loginId))
                .fetchOptionalInto(String.class);
    }


















    @Override
    public List<Map<String, Object>> getUserAndVerification(List<String> loginIds) {
        return dsl.select(CK_USER.LOGINID, CK_USER.VERIFIED)   // Select loginId and verified columns
                .from(CK_USER)                              // From the USER table
                .where(CK_USER.LOGINID.in(loginIds))       // Only where loginId is in the provided list
                .fetchMaps();                            // Fetch results as a List of Maps
    }

    public List<CkUser> findByMobile(String mobile) {
        return dsl.selectFrom(CK_USER)                  // Select from the CK_USER table
                .where(CK_USER.MOBILE.eq(mobile))     // Filter rows where mobile matches the provided value
                .fetchInto(CkUser.class);             // Fetch the results into a List of CkUser objects
    }

    public List<CkUser> findByEmail(String email) {
        return dsl.selectFrom(CK_USER)                  // Select from the CK_USER table
                .where(CK_USER.EMAIL.eq(email))     // Filter rows where mobile matches the provided value
                .fetchInto(CkUser.class);
    }

    public List<CkUser> findByActiveMobile(String mobile) {
        return dsl.selectFrom(CK_USER)                            // Select from the USER table
                .where(CK_USER.MOBILE.eq(mobile))               // Filter where the mobile matches the provided value
                .and(CK_USER.ACTIVE_STATUS.in(Arrays.asList("active", "1")))   // Filter where activeStatus is 'active' or '1'
                .fetchInto(CkUser.class);                      // Fetch the results into a List of User objects
    }

    public List<CkUser> findByActiveEmail(String email) {
        return dsl.selectFrom(CK_USER)                            // Select from the USER table
                .where(CK_USER.EMAIL.eq(email))               // Filter where the mobile matches the provided value
                .and(CK_USER.ACTIVE_STATUS.in(Arrays.asList("active", "1")))   // Filter where activeStatus is 'active' or '1'
                .fetchInto(CkUser.class);
    }

    public CkUser findByFacebookPSID(String facebookPSID) {
        return dsl.selectFrom(CK_USER)
                .where(CK_USER.FACEBOOKPSID.eq(facebookPSID))
                .fetchOneInto(CkUser.class);

    }

    public CkUser findByMessengerInfoChannelId(String channelId) {
        return dsl.select()
                .from(CK_USER)
                .join(CK_USER_MESSENGER_INFO)
                .on(CK_USER.LOGINID.eq(CK_USER_MESSENGER_INFO.LOGIN_ID)) // Adjust based on your FK relationship
                .where(CK_USER_MESSENGER_INFO.CHANNEL_ID.eq(channelId))
                .fetchOneInto(CkUser.class); // Map the result to your CkUser POJO
    }

    public CkUser findByLoginIdAndMessengerInfoChannel(String loginId, String channel) {
        return dsl.select()
                .from(CK_USER)
                .join(CK_USER_MESSENGER_INFO)
                .on(CK_USER.LOGINID.eq(CK_USER_MESSENGER_INFO.LOGIN_ID)) // Adjust based on your FK relationship
                .where(CK_USER_MESSENGER_INFO.CHANNEL.eq(channel))
                .and(CK_USER_MESSENGER_INFO.LOGIN_ID.eq(loginId))
                .fetchOneInto(CkUser.class); // Map the result to your CkUser POJO
    }

    public List<CkUserMessengerInfo> getMessengerInfos(String loginId, String channel) {
        return dsl.select()
                .from(CK_USER_MESSENGER_INFO)
                .where(CK_USER_MESSENGER_INFO.CHANNEL.eq(channel))
                .and(CK_USER_MESSENGER_INFO.LOGIN_ID.eq(loginId))
                .fetchInto(CkUserMessengerInfo.class);
    }

    public List<CkUser> findByUserContext(String token) {
        return dsl.select()
                .from(CK_USER)
                .join(CK_TOKEN)
                .on(CK_USER.ID.eq(CK_TOKEN.ID)) // Adjust based on your FK relationship
                .where(CK_TOKEN.TOKEN.eq(token))
                .fetchInto(CkUser.class);// Map the result to your CkUser POJO
    }

    public Long countByDesignationIs(String designation) {
        return dsl.selectCount()
                .from(CK_USERDESIGNATION)
                .where(CK_USERDESIGNATION.DESIGNATION.eq(designation))
                .fetchOne(0, Long.class); // Retrieves the count as a Long
    }

    public List<CkUser> findUserContextAndLoginIdByLoginIdIn(List<String> loginId) {
        return dsl.select()
                .from(CK_USER)
                .where(CK_USER.LOGINID.in(loginId))  // Filters by loginId values in the provided list
                .fetchInto(CkUser.class);  // Maps the result into a list of CkUser objects
    }

    public List<CkUser> findByMobileIn(List<String> mobileNumbers) {
        return dsl.select()
                .from(CK_USER)
                .where(CK_USER.MOBILE.in(mobileNumbers))  // Filters by loginId values in the provided list
                .fetchInto(CkUser.class);  // Maps the result into a list of CkUser objects
    }

    public void updateBlocked(String loginId, boolean blocked, String hash) {
        dsl.update(CK_USER)
                .set(CK_USER.BLOCKED, blocked)
                .set(CK_USER.HASH, hash)
                .where(CK_USER.LOGINID.eq(loginId))
                .execute();  // Executes the update without storing the result
    }

    public int updateVerified(String loginId, boolean verified, boolean blocked, String hash) {
        return dsl.update(CK_USER)
                .set(CK_USER.VERIFIED, verified ? (byte) 1 : (byte) 0)
                .set(CK_USER.BLOCKED, blocked)
                .set(CK_USER.HASH, hash)
                .where(CK_USER.LOGINID.eq(loginId))
                .execute();  // Executes the update without storing the result
    }

    @Override
    public int updateActiveStatusAndReason(String loginId, ActiveStatus activeStatus, String activeStatusReason) {
        return 0;
    }









    public List<String> getExistingUsers(List<String> loginIdList) {
        return dsl.select()
                .from(CK_USER)
                .innerJoin(CK_USERDESIGNATION)
                .on(CK_USERDESIGNATION.LOGIN_ID.eq(CK_USER.LOGINID))
                .where(CK_USERDESIGNATION.DESIGNATION.ne("retailer"))
                .and(CK_USER.LOGINID.in(loginIdList))
                .fetch(CK_USER.LOGINID);
    }

    public List<Map<String, Object>> getUserHierarchy(List<String> loginIds) {
        return dsl.select(CK_USER.LOGINID, CK_USER.HIERARCHY)
                .from(CK_USER)
                .where(CK_USER.LOGINID.in(loginIds))
                .fetchMaps();
    }

    public Optional<String> findLoginIdByReferenceId(String externalReferenceId) {
        return Optional.ofNullable(dsl.select(CK_USER.LOGINID)
                .from(CK_USER)
                .where(CK_USER.EXTERNAL_REFERENCE_ID.eq(externalReferenceId))
                .fetchOne(CK_USER.LOGINID));
    }

    public int updateReportPassword(String loginid, String reportPassword) {
        return dsl.update(CK_USER)
                .set(CK_USER.REPORT_PASSWORD, reportPassword)
                .where(CK_USER.LOGINID.eq(loginid))
                .execute();
    }


}
