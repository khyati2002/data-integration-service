/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.services;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.channelkart.models.diff.Change;
import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.channelkart.models.enums.RoleName;
import com.salescode.channelkart.repository.UserRepository;
import com.salescode.channelkart.services.enums.OperationType;
import com.salescode.channelkart.utils.CdmDiffUtil;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.channelkart.utils.NullUtils;
import com.salescode.jooq.CkSupplierMetadata;
import com.salescode.jooq.generated.tables.pojos.*;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.salescode.jooq.generated.Tables.CK_USER_PARENT;
import static com.salescode.jooq.generated.tables.CkHierarchyMetadata.CK_HIERARCHY_METADATA;

@Service
public class UserService extends AbstractCDMService<CkUser> {

    public static final String CACHE_DOMAIN = "users";
    public static final String CHANNEL_CACHE = "users_channel";
    public static final String DEFAULT_LOCATION_FIELD = "locationHierarchy";
    public static final String NORMALIZED_CHARECTORS = "U";
    public static final String NORMALIZED_JOINING_CHARECTORS = "U>U";
    public static final String TEST_USER_STARTS_WITH = "test";
    public static final String DEFAULT_ERROR_MESSAGE = "invalid username or password";
    public static final String RETAILER = "retailer";
    public static final String DEFAULT_PASSWORD = "@1234";
    public static final String DEFAULT_ENCODED_PASSWORD = "$2a$10$GetnNjgilfLkIv.2R3nHMevLZfI9HGHWQ3iXw3nrCfJlrpePirkIi";
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final String LOG_TYPE = "GENERAL";
    private static final String DEFAULT_PASSWORD_DOMAIN_NAME = "password";
    private static final String DEFAULT_PASSWORD_DOMAIN_TYPE = "user";
    private static final String UNAUTHORIZED_USER = "Unauthorized user";
    private static final String MULTIPLE_USER_FOUND = "Got multiple users for single login ";
    private static final String LOGIN_ID = "loginId";
    private final DSLContext dsl;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final UserParentService userparentservice;
    private final MetaDataService metadataservice;

    public UserService(HierarchyMetaDataService hierarchyMetaDataService, RoleService roleService, UserParentService userparentservice, DSLContext dsl, MetaDataService metadataservice, UserRepository userRepository, SupplierMetaDataService supplierMetaDataService) {
        this.roleService = roleService;
        this.userparentservice = userparentservice;
        this.dsl = dsl;
        this.metadataservice = metadataservice;
        this.userRepository = userRepository;
    }

    //
//
    private static boolean staleRecords(Set<String> existingParents, List<CkHierarchyMetadata> immediateParents) {
        Set<String> hmlist = immediateParents.stream().map(CkHierarchyMetadata::getParent).collect(Collectors.toSet());
        return !existingParents.equals(hmlist);
    }

    //
    private static boolean isSameSupplierMetada(CkUser user1, CkUser user2) {

        if ((user1.getSupplierMetaData() == null || user1.getSupplierMetaData().isEmpty()) && (user2.getSupplierMetaData() == null || user2.getSupplierMetaData().isEmpty())) {
            return true;

        } else if (user1.getSupplierMetaData() != null && !user1.getSupplierMetaData().isEmpty()) {

            List<CkSupplierMetadata> spms1 = user1.getSupplierMetaData();

            if (user2.getSupplierMetaData() == null || user2.getSupplierMetaData().isEmpty()) {

                return false;
            } else {

                List<CkSupplierMetadata> spms2 = user2.getSupplierMetaData();


                if (spms2.size() != spms1.size()) {

                    return false;
                } else {
                    try {

                        return spms1.stream().allMatch(s -> s.getUser() != null && spms2.contains(s));

                    } catch (Exception e) {
                        logger.error("stacktrace", e);
                    }
                    return false;
                }

            }

        } else {

            return false;
        }

    }

    public static String getNormalizedHierarchy(String hierarchy) {
        if (StringUtils.isBlank(hierarchy)) {
            return hierarchy;
        }
        String normalizedHierarchy = NORMALIZED_CHARECTORS + Arrays.asList(hierarchy.split(",")).stream().map(h -> Arrays.asList(h.split(" > "))).flatMap(List::stream).collect(Collectors.toSet()).stream().collect(Collectors.joining(NORMALIZED_JOINING_CHARECTORS)) + NORMALIZED_CHARECTORS;
        return removeSpecialCharacters(normalizedHierarchy);
    }

    private static String removeSpecialCharacters(String normalizedHierarchy) {
        String exludedCharactors = getExludedCharactors();
        try {
            String str = normalizedHierarchy.replaceAll(exludedCharactors, "");
            return str;
        } catch (Exception e) {
            logger.error("Exception happend while removing special charactors {} in normalized hierarchy {}", exludedCharactors, normalizedHierarchy);
            return normalizedHierarchy;
        }
    }

    public static String getExludedCharactors() {
        return System.getProperty("excludeCharNormalizedHierarchy", "[^a-zA-Z0-9>]");
    }

    public CkUser findByLoginId(String loginId, boolean cached) {
        return findByLoginId(loginId, true, true);
    }

    public CkUser findByLoginId(String loginId, boolean cached, boolean hierarchy) {
        //String lob = SecurityContextUtils.getLob();
        Function<String, CkUser> function = (String lid) -> {
            UserService service = SpringContext.getBean(UserService.class);
            return service.getLoadedUserObject(lid, hierarchy);
        };


        return function.apply(loginId);
    }

    public CkUser getLoadedUserObject(String lid, boolean hierarchy) {
//		CkUser u = TimerUtils
//				.withTime("Time taken UserService record ", () -> userRepository.findByLoginId(lid));
        CkUser u = userRepository.findByLoginId(lid);
        if (u != null) {
            loadUserAssociationObjects(u);
//			if (hierarchy && outletDetailsService.getImmediateParent(u) == null) {
//				TimerUtils
//						.withTime("Time taken UserService hierarchyMetaDataService load ", () ->
//				u.setImmediateParent(hierarchyMetaDataService.findParentThroughUserLoginId(lid));
//						);
//				u.setImmediateParent(hierarchyMetaDataService.findParentThroughUserLoginId(lid));
        }
        return u;
    }

    @Override
    public CkUser save(CkUser inUser) {
        return this.save(inUser, OperationType.insert);
    }

    public void saveUserHierarchyMetadata(CkUser user) {
        List<CkHierarchyMetadata> hierarchy = user.getImmediateParent();
        var record = dsl.newRecord(CK_HIERARCHY_METADATA, hierarchy.get(0));
        if (hierarchy.get(0).getId() == null) {
            user.getImmediateParent().get(0).setId("hierarchy-parent");
        }
        if (record.get(CK_HIERARCHY_METADATA.ID) == null) record.set(CK_HIERARCHY_METADATA.ID, "hierarchy-parent");
        if (record.get(CK_HIERARCHY_METADATA.VERSION) == null) record.set(CK_HIERARCHY_METADATA.VERSION, 1);
        dsl.insertInto(CK_HIERARCHY_METADATA).set(record).onDuplicateKeyUpdate().set(record).execute();
    }

    public CkUser save(CkUser inUser, OperationType type) {
        //String lob = SecurityContextUtils.getLob();
        //CkUser user= TimerUtils.withTime("Time Taken to execute fillUser()", u-> fillUser(inUser));
        CkUser user = fillUser(inUser);
        //clearCache(lob,user);
        if (user.getRoles().size() == 1 && user.getRoles().stream().allMatch(desig -> desig.getName().equals(RoleName.ROLE_ADMIN.name()))) {
            CkUserParent up = new CkUserParent();
            up.setUserloginid(user.getLoginid());
            up.setParent(null);
            up.setLob(inUser.getLob());
            //UserParent refreshedObj=TimerUtils.withTime("Time taken to refresh UserParent", s-> userparentservice.refresh(up));
            CkUserParent refreshedObj = userparentservice.refresh(up);
            //TimerUtils.withTime("Time taken to save UserParent", ()->
            var record = dsl.newRecord(CK_USER_PARENT, refreshedObj);
            dsl.insertInto(CK_USER_PARENT).set(record).onDuplicateKeyUpdate().set(record).execute();
            userparentservice.save(refreshedObj);
            //);
        }

        if (user.getImmediateParent() != null && !user.getImmediateParent().isEmpty()) {
            //TimerUtils.withTime("Time taken to save UserParent", ()->
            saveUserParent(user, type);
            //);
        }


        //User savedObj= TimerUtils.withTime("Time Taken to save User[["+user.getLoginId()+"]]", u-> super.save(user));
        saveUser(user);
        CkUser savedObj = super.save(user);
        if (inUser.getSupplierMetaData() != null && !inUser.getSupplierMetaData().isEmpty()) {
            List<CkSupplierMetadata> supplierMetaInfo = user.getSupplierMetaData();
            if (!isSameSupplierMetada(savedObj, user)) {
                supplierMetaInfo.forEach(cdmObject -> {
                    cdmObject.setUser(savedObj);
                    cdmObject.setLob(user.getLob());
                });
                //TimerUtils.withTime("Time taken to batchSave SupplierMetadata of Size "+supplierMetaInfo.size(), ()->
                //supplierMetaDataService.batchSave(supplierMetaInfo));
            }

        }

        //AuditLogger.log(LOG_TYPE, "Created new User with loginId '{}'",user.getLoginId());
        //clearCache(lob,user);

        return savedObj;
    }

    private CkUser saveUser(CkUser user) {
        user.setActiveStatus(ActiveStatus.ACTIVE);


        if (user.getVerified() == null) user.setVerified((byte) 1);
        if (user.getVersion() == null) user.setVersion(1);
        if (user.getPassword() == null) user.setPassword(getDefaultEncryptedUserPassword());
        if (user.getId() == null) user.setId(UUID.randomUUID().toString());
        var record = dsl.newRecord(com.salescode.jooq.generated.tables.CkUser.CK_USER, user);
        dsl.insertInto(com.salescode.jooq.generated.tables.CkUser.CK_USER).set(record).onDuplicateKeyUpdate().set(record).execute();
        return user;
    }

    //
    public CkUser fillUser(CkUser user) {
        if (NullUtils.isNotNull(user.getLocationHierarchy())) {
            try {
//				CkLocation loc=user.getLocationHierarchy();
//				//loc = locationService.findLocationOrPersistLocation(loc);
//				user.setLocationHierarchy(loc);
            } catch (Exception ex) {
                throw new IllegalStateException("Error occured while setting location for user: " + user.getLoginid(), ex);
            }
        } else {
            //	throw new IllegalStateException("Missing location data. Data cannot be saved without location information for user : "+user.getLoginid());
        }
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            List<CkAuthRole> roles = roleService.getRoleAsList(RoleName.ROLE_USER.name());
            user.setRoles(roles);
        } else {
            List<CkAuthRole> roles = new ArrayList<>();
            for (CkAuthRole objectRole : user.getRoles()) {
                roleService.getRole(objectRole.getName()).ifPresent(elem -> {
                    if (!roles.contains(elem)) {
                        roles.add(elem);
                    }
                });
            }
            user.setRoles(roles);
        }

        if (user.getSupplierMetaData() != null) {
            user.getSupplierMetaData().forEach(s -> {
                //supplierMetaDataService.fillCommonAttributes(s);
                s.setUser(user);
            });
        }

        if (user.getVerified() == null) {
            //	user.setVerified(false);
        }

        return user;
    }

    public String getDefaultEncryptedUserPassword() {
        try {
            CkMetadata metadata = metadataservice.fetchByValue(DEFAULT_PASSWORD_DOMAIN_NAME, DEFAULT_PASSWORD_DOMAIN_TYPE);
            String rawPassword;
            if (metadata == null) {
                rawPassword = DEFAULT_PASSWORD;
            } else {
                ArrayNode arraynode = (ArrayNode) metadata.getDomainValues();
                if (arraynode == null || arraynode.size() == 0) {
                    throw new Exception("System has found metadata resource for default password but seems misconfigured. Please check configuration.");
                }
                JsonNode node = arraynode.get(0);
                if (!node.has("default")) {
                    throw new Exception("System has found metadata resource for default password but 'default' key not found. Please check configuration.");
                }
                rawPassword = node.get("default").textValue();
            }
            if (StringUtils.equals(DEFAULT_PASSWORD, rawPassword)) {
                return DEFAULT_ENCODED_PASSWORD;
            } else {
                //return TimerUtils.withTime("time taken to encode password", () -> encoder.encode(rawPassword));
                return encoder.encode(rawPassword);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error");
        }

    }

    public void saveUserParent(CkUser user, OperationType type) {
        Set<CkUserParent> userParents = new HashSet<>();
        List<CkUserParent> dbParents = userparentservice.findByUserLoginId(user.getLoginid());
        if (dbParents != null && !dbParents.isEmpty()) {
            Set<String> dataset = dbParents.stream().map(CkUserParent::getParent).collect(Collectors.toSet());
            if (staleRecords(new HashSet<>(dataset), user.getImmediateParent())) {
                userParents.addAll(getNewUserParents(user, dataset, type));
                if (type.equals(OperationType.insert) || user.getDesignation().contains(RETAILER)) {
                    userparentservice.deleteByUserLoginId(user.getLoginid());
                    //hierarchySynchronizer.removeUser(user.getLoginid());
                }
            }
        } else {
            userParents.addAll(getUserParents(user));
        }
        if (!userParents.isEmpty()) {
            CkUserParent par = userParents.stream().findFirst().orElseThrow(() -> new RuntimeException("value not found"));
            if (par.getId() == null) {
                par.setId(UUID.randomUUID().toString());
            }
            if (par.getVersion() == null) {
                par.setVersion(1);
            }
            var record = dsl.newRecord(CK_USER_PARENT, par);
            dsl.insertInto(CK_USER_PARENT).set(record).onDuplicateKeyUpdate().set(record).execute();
        }
    }

    private List<CkUserParent> getNewUserParents(CkUser user, Set<String> dataset, OperationType type) {
        List<CkUserParent> userParentList = new ArrayList<>();
        for (CkHierarchyMetadata hm : user.getImmediateParent()) {
            if (!dataset.contains(hm.getParent()) || type.equals(OperationType.insert)) {
                CkUserParent up = new CkUserParent();
                up.setUserloginid(user.getLoginid());
                up.setParent(hm.getParent());
                if (up.getUserloginid().equalsIgnoreCase(up.getParent())) {
                    //	throw new UnexpectedResultException("User can't be mapped to itself. Found a record for user " + up.getUserLoginId() + " mapped to itself. Please verify the data once.");
                }
                userParentList.add(up);
            }
        }
        return userParentList;
    }

    private List<CkUserParent> getUserParents(CkUser user) {
        List<CkUserParent> userParentList = new ArrayList<>();
        for (CkHierarchyMetadata hm : user.getImmediateParent()) {
            CkUserParent up = new CkUserParent();
            up.setUserloginid(user.getLoginid());
            up.setParent(hm.getParent());
            up.setId(UUID.randomUUID().toString());
            if (up.getUserloginid().equalsIgnoreCase(up.getParent())) {
                //	throw new UnexpectedResultException("User can't be mapped to itself. Found a record for user " + up.getUserLoginId() + " mapped to itself. Please verify the data once.");
            }
            userParentList.add(up);
        }
        return userParentList;
    }

    private String getUserParentList(Set<CkUserParent> userParents) {
        StringBuilder userParentList = new StringBuilder();
        for (CkUserParent parent : userParents) {
            userParentList.append(parent.getParent() + ", ");
        }
        return userParentList.toString();
    }


    @Override
    public CkUser refresh(CkUser cdmObject) {
        var dbRecord = CdmDiffUtil.withOldModel(() -> (CkUser) EntityUtils.getInstance().findRecords(cdmObject.getClass(), cdmObject));
        if (dbRecord != null) {
            cdmObject.setOldModel(dbRecord.getOldModel());
            CkUser dbrecordsCopy = synchronizeNewObject(dbRecord, cdmObject);
            setSupplierChanges(dbrecordsCopy, cdmObject);
            setChanges(dbRecord, dbrecordsCopy);
            return dbrecordsCopy;
        }
        return cdmObject;
    }

    private CkUser synchronizeNewObject(CkUser dbRecord, CkUser cdmObject) {
        CkUser dbrecordsCopy = new CkUser();
        CkUser clonedDBRecord = EntityUtils.deepClone(dbRecord);
        EntityUtils.copyProperties(clonedDBRecord, dbrecordsCopy);


        List<CkHierarchyMetadata> tempList = NullUtils.isNull(cdmObject.getImmediateParent()) ? dbrecordsCopy.getImmediateParent() : cdmObject.getImmediateParent();
        //attributeUpdateOverrideManager.mergeProperties(cdmObject,dbrecordsCopy);

        Map<String, CkHierarchyMetadata> hmMap = new HashMap<>();
        dbrecordsCopy.getImmediateParent().forEach(h -> hmMap.put(h.getParent(), h));

        List<CkHierarchyMetadata> changedList = new ArrayList<>();
        dbrecordsCopy.setImmediateParent(tempList.stream().map(h -> {
            if (!hmMap.containsKey(h.getParent())) {
                changedList.add(h);
            }
            return h;
        }).collect(Collectors.toList()));


        EntityUtils.copyProperties(cdmObject, dbrecordsCopy, "supplierMetaData", "version");

        if (dbrecordsCopy.getMobile() != null && !dbrecordsCopy.getMobile().equals(dbRecord.getMobile())) {
            //dbrecordsCopy.setVerified(false);
        }

        if (!changedList.isEmpty()) {
            dbrecordsCopy.setHash(null);
            Set<Change<Serializable>> userChanges = dbrecordsCopy.getChanges();
            userChanges.add(new Change<>("immediateParent", null, null));
            dbrecordsCopy.setChanges(userChanges);
        }
        return dbrecordsCopy;
    }

    private void setSupplierChanges(CkUser dbrecordsCopy, CkUser cdmObject) {
        if (!dbrecordsCopy.getSupplierMetaData().isEmpty()) {
            List<CkSupplierMetadata> cdmSupplierList = cdmObject.getSupplierMetaData();
            for (CkSupplierMetadata supplier : cdmSupplierList) {
                supplier.setUser(dbrecordsCopy);
            }
//            cdmSupplierList = supplierMetaDataService.refresh(cdmSupplierList);
            dbrecordsCopy.getSupplierMetaData().clear();
            dbrecordsCopy.getSupplierMetaData().addAll(cdmSupplierList);
        } else {
            dbrecordsCopy.setSupplierMetaData(cdmObject.getSupplierMetaData());
        }

    }

    private void loadUserAssociationObjects(CkUser u) {

    }
}
