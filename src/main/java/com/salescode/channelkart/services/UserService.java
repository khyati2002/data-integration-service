/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.services;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.channelkart.cache.AppCacheManager;
import com.salescode.channelkart.cache.DistributedCache;
import com.salescode.channelkart.exceptions.UnexpectedResultException;
import com.salescode.channelkart.models.*;
import com.salescode.channelkart.models.diff.Change;
import com.salescode.channelkart.models.enums.RoleName;
import com.salescode.channelkart.models.SupplierMetaData;
import com.salescode.channelkart.permission.services.AttributeUpdateOverrideManager;
import com.salescode.channelkart.repository.UserRepository;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.services.enums.OperationType;
import com.salescode.channelkart.utils.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;


import javax.persistence.Query;
import javax.transaction.Transactional;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class UserService extends AbstractCDMService<User> {

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

   // private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final UserParentService userparentservice;
    private HierarchyMetaDataService hierarchyMetaDataService;


    @Autowired private SupplierMetaDataService supplierMetaDataService;

    @Autowired private LocationService locationService;

    private DistributedCache distributedCache;
    @Autowired
    private AttributeUpdateOverrideManager attributeUpdateOverrideManager;

    public UserService(HierarchyMetaDataService hierarchyMetaDataService, RoleService roleService, UserParentService userparentservice,UserRepository userRepository, DistributedCache distributedCache) {
        super(userRepository);
        this.roleService = roleService;
        this.userparentservice = userparentservice;
        this.userRepository = userRepository;
        this.hierarchyMetaDataService = hierarchyMetaDataService;
        this.distributedCache = distributedCache;
    }


    private static boolean staleRecords(Set<String> existingParents,List<HierarchyMetaData> immediateParents) {
        Set<String> hmlist = immediateParents.stream().map(HierarchyMetaData::getImmediateParent).collect(Collectors.toSet());
        return !existingParents.equals(hmlist);
    }
    //

    private static boolean isSameSupplierMetada(User user1,User user2) {

        if( (user1.getSupplierMetaData()==null|| user1.getSupplierMetaData().isEmpty()) && (user2.getSupplierMetaData()==null || user2.getSupplierMetaData().isEmpty())) {
            return true;

        } else if(user1.getSupplierMetaData()!=null && !user1.getSupplierMetaData().isEmpty()) {

            List<SupplierMetaData> spms1 = user1.getSupplierMetaData();

            if(user2.getSupplierMetaData()==null || user2.getSupplierMetaData().isEmpty()) {

                return false;
            }else {

                List<SupplierMetaData> spms2 = user2.getSupplierMetaData();


                if(spms2.size()!=spms1.size()) {

                    return false;
                }else {
                    try {

                        return spms1.stream().allMatch(s->s.getUser()!=null && spms2.contains(s));

                    }catch (Exception e) {
                        logger.error("stacktrace", e);
                    }
                    return false;
                }

            }

        }else {

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

    public User findByLoginId(String loginId) {
        return findByLoginId(loginId,true,true);
    }

    public User findByLoginId(String loginId, boolean cached) {
        return findByLoginId(loginId, true, true);
    }


    public User findByLoginId(String loginId,boolean cached,boolean hierarchy) {
        String lob = SecurityContextUtils.getLob();
        Function<String,User> function = (String lid)->{
            UserService service= SpringContext.getBean(UserService.class);
            return service.getLoadedUserObject(lid,hierarchy);
        };

        if(cached) {
            User user = distributedCache.withCache(lob,CACHE_DOMAIN, loginId,function);
            if(user != null && org.apache.commons.lang.StringUtils.isBlank(user.getHierarchy())) {
                return reloadCache(loginId);
            }
            return user;
        }

        return function.apply(loginId);
    }

    public User reloadCache(String loginId) {
        String lob = SecurityContextUtils.getLob();
        Function<String,User> function = (String lid)->{
            UserService service= SpringContext.getBean(UserService.class);
            return service.getLoadedUserObject(lid,true);
        };

        return  distributedCache.withCache(lob,CACHE_DOMAIN, loginId,function);
    }

    public Optional<List<User>> findByMobileSafely(String mobile) {
        List<User> users= userRepository.findByMobile(mobile);
        return CollectionUtils.isEmpty(users)? Optional.empty() : Optional.of(users);
    }

    public User getLoadedUserObject(String lid,boolean hierarchy) {
        User u = TimerUtils
                .withTime("Time taken UserService record ", () -> userRepository.findByLoginId(lid));

        if (u != null) {
            loadUserAssociationObjects(u);
            if (hierarchy && u.getImmediateParent() == null) {
                TimerUtils
                        .withTime("Time taken UserService hierarchyMetaDataService load ", () ->
                                u.setImmediateParent(hierarchyMetaDataService.findParentThroughUserLoginId(lid))
                        );
            }
        }
        return u;
    }



    @Override
    public User save(User inUser)  {
        return this.save(inUser, OperationType.insert);
    }

    @Override
    public User save(User inUser, OperationType type) {
         String lob = SecurityContextUtils.getLob();
       // User user= TimerUtils.withTime("Time Taken to execute fillUser()", u-> fillUser(inUser));
        User user = fillUser(inUser);
        clearCache(lob,user);
        if(user.getRoles().size()==1 && user.getRoles().stream().allMatch(desig->desig.getName().equals(RoleName.ROLE_ADMIN.name()))) {
            UserParent up= new UserParent();
            up.setUserLoginId(user.getLoginId());
            up.setParent(null);
            up.setLob(inUser.getLob());
           // UserParent refreshedObj=TimerUtils.withTime("Time taken to refresh UserParent", s-> userparentservice.refresh(up));
            UserParent refreshedObj = userparentservice.refresh(up);
            //TimerUtils.withTime("Time taken to save UserParent", ()->
            userparentservice.save(refreshedObj);
            //);
        }

        if(user.getImmediateParent() != null && !user.getImmediateParent().isEmpty()) {
//            TimerUtils.withTime("Time taken to save UserParent", ()->
                    saveUserParent(user,type);
//        );
        }

       // User savedObj= TimerUtils.withTime("Time Taken to save User[["+user.getLoginId()+"]]", u-> super.save(user));
        User savedObj = super.save(user);
        if(inUser.getSupplierMetaData()!=null && !inUser.getSupplierMetaData().isEmpty()) {
            List<SupplierMetaData> supplierMetaInfo= user.getSupplierMetaData();
            if(!isSameSupplierMetada(savedObj, user)) {
                supplierMetaInfo.forEach(cdmObject->{
                    cdmObject.setUser(savedObj);
                    cdmObject.setLob(user.getLob());
                });
                try {
                    supplierMetaDataService.batchSave(supplierMetaInfo);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                //  TimerUtils.withTime("Time taken to batchSave SupplierMetadata of Size "+supplierMetaInfo.size(), ()->
                  //  supplierMetaDataService.batchSave(supplierMetaInfo);
               // );
            }
        }

//        AuditLogger.log(LOG_TYPE, "Created new User with loginId '{}'",user.getLoginId());
        clearCache(lob,user);

        return savedObj;
    }

    public void clearCache(String lob, User user) {
        if(user != null) {
            clearCache(lob, user.getLoginId());
            locationService.clearCache(lob,user.getLocationHierarchy().getLocationHierarchy());
        }
    }

    public User fillUser(User user) {
        if(NullUtils.isNotNull(user.getLocationHierarchy())) {
            try {
                Location loc=user.getLocationHierarchy();
                loc = locationService.findLocationOrPersistLocation(loc);
                user.setLocationHierarchy(loc);
            }
            catch(Exception ex) {
               // throw new IllegalStateException("Error occured while setting location for user: "+user.getLoginId(),ex);
            }
        }else {
        //    throw new IllegalStateException("Missing location data. Data cannot be saved without location information for user : "+user.getLoginId());
        }
        if(user.getRoles()== null || user.getRoles().isEmpty()) {
            List<Role> roles = roleService.getRoleAsList(RoleName.ROLE_USER.name());
            user.setRoles(roles);
        } else{
            List<Role> roles = new ArrayList<>();
            for (Role objectRole : user.getRoles()) {
                roleService.getRole(objectRole.getName()).ifPresent(elem->{
                    if(!roles.contains(elem)) {
                        roles.add(elem);
                    }
                });
            }
            user.setRoles(roles);
        }

        if(user.getSupplierMetaData()!=null) {
            user.getSupplierMetaData().forEach(s->
            {
               supplierMetaDataService.fillCommonAttributes(s);
                s.setUser(user);
            });
        }

        if(user.getVerified()==null) {
            user.setVerified(false);
        }
        if(user.getPassword()==null){
            user.setPassword(DEFAULT_ENCODED_PASSWORD);
        }
        if(user.getBlocked()!=true) {
            user.setBlocked(false);
        }


        return user;
    }


    private void evaluateUserHierarchy(User user,Set<UserParent> userParents) {
        StringBuilder hierarchyStr = new StringBuilder();
        userParents.forEach(parent -> {
            List<HierarchyMetaData> hmList = (List<HierarchyMetaData>) hierarchyMetaDataService.findByImmediateParent(parent.getParent());
            if (hmList.isEmpty()) {
                hierarchyStr.append(user.getLoginId() + " > " + parent.getParent() + " > " + getCustomerAccountsService().getAdminLoginId());
                hierarchyStr.append(",");
            } else {
                hmList.forEach(hierarchyMetaData -> {
                    hierarchyStr.append(user.getLoginId() + " > " + hierarchyMetaData.getHierarchy());
                    hierarchyStr.append(",");
                });
            }
        });
        user.setHierarchy(hierarchyStr.substring(0, hierarchyStr.length() - 1));
        user.setNormalizedHierarchy(UserService.getNormalizedHierarchy(user.getHierarchy()));
    }
    private CustomerAccountsService getCustomerAccountsService() {
        return SpringContext.getBean(CustomerAccountsService.class);
    }


    public void saveUserParent(User user, OperationType type){
        Set<UserParent> userParents= new HashSet<>();
        List<UserParent> dbParents= userparentservice.findByUserLoginId(user.getLoginId());
        if(dbParents != null && !dbParents.isEmpty()) {
            Set<String> dataset= dbParents.stream().map(UserParent::getParent).collect(Collectors.toSet());
            if(staleRecords(new HashSet<>(dataset), user.getImmediateParent())) {
                userParents.addAll(getNewUserParents(user, dataset, type));
                if(type.equals(OperationType.insert) || user.getDesignation().contains(RETAILER)) {
                    userparentservice.deleteByUserLoginId(user.getLoginId());
                   // hierarchySynchronizer.removeUser(user.getLoginId());
                }
            }
        }else {
            userParents.addAll( getUserParents(user) );
        }
        if(!userParents.isEmpty()) {
             try {
                 userparentservice.batchSave(userParents);
                 evaluateUserHierarchy(user,userParents);
             } catch (Exception e) {
                 throw new RuntimeException(e);
             }

//            AuditLogger.log(LOG_TYPE, "Updated parents of User with loginId '{}'. LoginId of parents: '[{}]'", user.getLoginId(), getUserParentList(userParents));
        }
    }

    private List<UserParent> getNewUserParents(User user, Set<String> dataset, OperationType type){
        List<UserParent> userParentList = new ArrayList<>();
        for(HierarchyMetaData hm: user.getImmediateParent()) {
            if(!dataset.contains(hm.getImmediateParent()) || type.equals(OperationType.insert)) {
                UserParent up= new UserParent();
                up.setUserLoginId(user.getLoginId());
                up.setParent(hm.getImmediateParent());
                if (up.getUserLoginId().equalsIgnoreCase(up.getParent())) {
                   throw new UnexpectedResultException("User can't be mapped to itself. Found a record for user " + up.getUserLoginId() + " mapped to itself. Please verify the data once.");
                }
                userParentList.add(up);
            }
        }
        return userParentList;
    }

    private List<UserParent> getUserParents(User user){
        List<UserParent> userParentList = new ArrayList<>();
        for(HierarchyMetaData hm: user.getImmediateParent()) {
            UserParent up= new UserParent();
            up.setUserLoginId(user.getLoginId());
            up.setParent(hm.getImmediateParent());
            if (up.getUserLoginId().equalsIgnoreCase(up.getParent())) {
           //     throw new UnexpectedResultException("User can't be mapped to itself. Found a record for user " + up.getUserLoginId() + " mapped to itself. Please verify the data once.");
            }
            userParentList.add(up);
        }
        return userParentList;
    }

    private String getUserParentList(Set<UserParent> userParents){
        StringBuilder userParentList = new StringBuilder();
        for (UserParent parent: userParents) {
            userParentList.append(parent.getParent() + ", ");
        }
        return userParentList.toString();
    }


    @Override
    public User refresh(User cdmObject) {
        var dbRecord = CdmDiffUtil.withOldModel(() -> (User) EntityUtils.get().findRecords(cdmObject.getClass(), cdmObject));
        if (dbRecord != null) {
            cdmObject.setOldModel(dbRecord.getOldModel());
            User dbrecordsCopy = synchronizeNewObject(dbRecord, cdmObject);
            setSupplierChanges(dbrecordsCopy, cdmObject);
            setChanges(dbRecord, dbrecordsCopy);
            return dbrecordsCopy;
        }
        return cdmObject;
    }


    private User synchronizeNewObject(User dbRecord,User cdmObject){
        User dbrecordsCopy = new User();
        User clonedDBRecord=EntityUtils.deepClone(dbRecord);
        EntityUtils.copyProperties(clonedDBRecord, dbrecordsCopy);


        List<HierarchyMetaData> tempList = NullUtils.isNull(cdmObject.getImmediateParent())?dbrecordsCopy.getImmediateParent():cdmObject.getImmediateParent();
        attributeUpdateOverrideManager.mergeProperties(cdmObject,dbrecordsCopy);

        Map<String,HierarchyMetaData> hmMap = new HashMap<>();
        if(dbrecordsCopy.getImmediateParent() != null) dbrecordsCopy.getImmediateParent().forEach(h->hmMap.put(h.getImmediateParent(),h));
        List<HierarchyMetaData> changedList = new ArrayList<>();
        if(tempList != null) {
            dbrecordsCopy.setImmediateParent(tempList.stream().map(h -> {
                if (!hmMap.containsKey(h.getImmediateParent())) {
                    changedList.add(h);
                }
                return h;
            }).collect(Collectors.toList()));
        }



        EntityUtils.copyProperties(cdmObject,dbrecordsCopy,"supplierMetaData","version");

        if(dbrecordsCopy.getMobile() != null && !dbrecordsCopy.getMobile().equals(dbRecord.getMobile())) {
            dbrecordsCopy.setVerified(false);
        }

        if(!changedList.isEmpty()) {
            dbrecordsCopy.setHash(null);
            Set<Change<Serializable>> userChanges = dbrecordsCopy.getChanges();
            userChanges.add(new Change<>("immediateParent", null, null));
            dbrecordsCopy.setChanges(userChanges);
        }
        return dbrecordsCopy;
    }

    private void setSupplierChanges(User dbrecordsCopy,User cdmObject){
        if(!dbrecordsCopy.getSupplierMetaData().isEmpty()) {
            List<SupplierMetaData> cdmSupplierList=cdmObject.getSupplierMetaData();
            for(SupplierMetaData supplier:cdmSupplierList){
                supplier.setUser(dbrecordsCopy);
            }
            cdmSupplierList=supplierMetaDataService.refresh(cdmSupplierList);
            dbrecordsCopy.getSupplierMetaData().clear();
            dbrecordsCopy.getSupplierMetaData().addAll(cdmSupplierList);
        }else{
            dbrecordsCopy.setSupplierMetaData(cdmObject.getSupplierMetaData());
        }
    }

    private void loadUserAssociationObjects(User u) {
        if (u.getRoles() != null) u.getRoles().size();
        if (u.getSupplierMetaData() != null) u.getSupplierMetaData().size();
        if (u.getDesignation() != null) u.getDesignation().size();
        // if (u.getMessengerInfo() != null) u.getMessengerInfo().size();
    }

    public void clearCache(String lob, String loginId) {
        if(org.apache.commons.lang.StringUtils.isNotBlank(loginId)) {
            distributedCache.clearCache(lob,CACHE_DOMAIN,loginId);
            SupplierInfoService supplierInfoService= SpringContext.getBean(SupplierInfoService.class);
            supplierInfoService.clearCache(lob,"u:"+loginId);
            supplierInfoService.clearCache(lob,"o:"+loginId);
            hierarchyMetaDataService.clearCache(lob, loginId);
            clearChannelCache(loginId);
        }
    }
    private void clearChannelCache(String loginId) {
        AppCacheManager.getInstance().removeByDomain(SecurityContextUtils.getLob(), CHANNEL_CACHE + loginId);
        //AppCacheManager.getInstance().removeByDomain(SecurityContextUtils.getLob(), CHANNEL_CACHE + loginId + NotificationTypeRegistry.FIREBASE.name());

    }
    public String getDefaultEncryptedUserPassword(){
        return DEFAULT_ENCODED_PASSWORD;
    }
}
