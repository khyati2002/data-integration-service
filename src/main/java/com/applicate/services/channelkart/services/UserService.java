package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.RoleName;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.salescode.dim.OperationType;
import com.salescode.dim.jooq.generated.tables.CkUserParent;
import com.salescode.dim.jooq.generated.tables.pojos.AuthRole;
import com.salescode.dim.jooq.generated.tables.pojos.Location;
import com.salescode.dim.jooq.generated.tables.pojos.UserParent;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.User;
import com.salescode.dim.registry.ETLRegistry;
import com.salescode.dim.transformers.registry.TransformerInfoRegistry;
import com.salescode.dim.transformers.service.DataTransformationService;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;

import java.util.*;
import java.util.stream.Collectors;

import static com.applicate.services.channelkart.services.OutletDetailsService.RETAILER;

public class UserService extends AbstractCDMService<User>{

    private static UserService instance;

    public static final String NORMALIZED_CHARECTORS = "U";

    public static final String NORMALIZED_JOINING_CHARECTORS = "U>U";

    private static DSLContext dsl;

    private static UserParentService userParentService;

    public static UserService getInstance(DSLContext dsl) {
        if (instance == null) {
            instance = new UserService(dsl);
            userParentService = new UserParentService(dsl);
        }
        return instance;
    }


    public UserService(DSLContext dsl){
        super(dsl);
        this.dsl = dsl;
    }

    @Override
    public List<User> batchSave(List<User> cdmObject) {
        UserService.getInstance(dsl);
        List<User> userList = fillUser(cdmObject);
        List<List<UserParent>> userParentList = new ArrayList<>();
        userList.forEach(user -> {
            if (user.getRoles()!=null && user.getRoles().size() == 1 && user.getRoles().stream().allMatch(desig -> desig.getName().equals(RoleName.ROLE_ADMIN.name()))) {
                List<UserParent> list = new ArrayList<>();
                UserParent up = new UserParent();
                up.setUserloginid(user.getLoginid());
                up.setParent(null);
                up.setLob(user.getLob());
                list.add(up);
                userParentList.add(list);
            }

            if (user.getImmediateParent() != null && !user.getImmediateParent().isEmpty()) {
                saveUserParent(user,OperationType.insert,userParentList);
            }


        });
        List<UserParent> flattenedList = userParentList.stream()
                .flatMap(List::stream) // Flatten the nested lists
                .collect(Collectors.toList());
        userParentService.batchSave(flattenedList);
        return super.batchSave(userList);
    }

    public void saveUserParent(User user, OperationType type,List<List<UserParent>> userParentList){
        Set<UserParent> userParents= new HashSet<>();
        List<UserParent> dbParents= userParentService.findByUserLoginId(user.getLoginid());
        if(dbParents != null && !dbParents.isEmpty()) {
            Set<String> dataset= dbParents.stream().map(UserParent::getParent).collect(Collectors.toSet());
            if(staleRecords(new HashSet<>(dataset), user.getImmediateParent())) {
                userParents.addAll(getNewUserParents(user, dataset, type));
            }
        }else {
            userParents.addAll(getUserParents(user) );
        }
//        if(!userParents.isEmpty()) {
//            userparentservice.batchSave(userParents);
//            evaluateUserHierarchy(user,userParents);
//            AuditLogger.log(LOG_TYPE, "Updated parents of User with loginId '{}'. LoginId of parents: '[{}]'", user.getLoginId(), getUserParentList(userParents));
//        }
    }

    private List<UserParent> getNewUserParents (User user, Set < String > dataset, OperationType type){
        List<UserParent> userParentList = new ArrayList<>();
        for (HierarchyMetadata hm : user.getImmediateParent()) {
            if (!dataset.contains(hm.getParent()) || type.equals(OperationType.insert)) {
                UserParent up = new UserParent();
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

    private static boolean staleRecords(Set < String > existingParents, List <HierarchyMetadata> immediateParents){
        Set<String> hmlist = immediateParents.stream().map(HierarchyMetadata::getParent).collect(Collectors.toSet());
        return !existingParents.equals(hmlist);
    }

    private List<UserParent> getUserParents (User user){
        List<UserParent> userParentList = new ArrayList<>();
        for (HierarchyMetadata hm : user.getImmediateParent()) {
            UserParent up = new UserParent();
            up.setUserloginid(user.getLoginid());
            up.setParent(hm.getParent());
            up.setId(UUID.randomUUID().toString());
            up.setVersion(1);
            if (up.getUserloginid().equalsIgnoreCase(up.getParent())) {
                //	throw new UnexpectedResultException("User can't be mapped to itself. Found a record for user " + up.getUserLoginId() + " mapped to itself. Please verify the data once.");
            }
            userParentList.add(up);
        }
        return userParentList;
    }

    public List<User> fillUser(List<User> userList) {
//
//        List<String> locList =  userList.stream()
//                .map(User::getLocationHierarchy)// Avoids null locations
//                .collect(Collectors.toList());

//        List<Location> persistedLocation = locationService.findLocationOrPersistLocation(locList);
//        for(int i=0;i<userList.size();i++){
//            userList.get(i).setLocationHierarchy(persistedLocation.get(i));
//        }
//
        userList.forEach(user -> {
//            if (user.getRoles() == null || user.getRoles().isEmpty()) {
//                List<AuthRole> roles = roleService.getRoleAsList(RoleName.ROLE_USER.name());
//                user.setRoles(roles);
//            } else {
//                List<AuthRole> roles = new ArrayList<>();
//                for (AuthRole objectRole : user.getRoles()) {
//                    roleService.getRole(objectRole.getName()).ifPresent(elem -> {
//                        if (!roles.contains(elem)) {
//                            roles.add(elem);
//                        }
//                    });
//                }
//                user.setRoles(roles);
//            }
//
//            if (user.getSupplierMetaData() != null) {
//                user.getSupplierMetaData().forEach(s ->
//                {
//                    supplierMetaDataService.fillCommonAttributes(s);
//                    s.setUser(user);
//                });
//            }
//
            if (user.getVerified() == null) {
                user.setVerified(false);
            }

          //  if (!user.getBlocked()) {
                user.setBlocked(false);
//            }
            user.setRowid(UUID.randomUUID().hashCode());
            user.setLocationHierarchy("abcd");
        });
        return userList;
    }

    public static String getNormalizedHierarchy (String hierarchy){
        if (StringUtils.isBlank(hierarchy)) {
            return hierarchy;
        }
        String normalizedHierarchy = NORMALIZED_CHARECTORS + Arrays.asList(hierarchy.split(","))
                .stream().map(h -> Arrays.asList(h.split(" > "))).flatMap(List::stream)
                .collect(Collectors.toSet()).stream()
                .collect(Collectors.joining(NORMALIZED_JOINING_CHARECTORS)) + NORMALIZED_CHARECTORS;
        return removeSpecialCharacters(normalizedHierarchy);
    }

    private static String removeSpecialCharacters (String normalizedHierarchy){
        String exludedCharactors = getExludedCharactors();
        try {
            String str = normalizedHierarchy.replaceAll(exludedCharactors, "");
            return str;
        } catch (Exception e) {
            //  logger.error("Exception happend while removing special charactors {} in normalized hierarchy {}", exludedCharactors, normalizedHierarchy);
            return normalizedHierarchy;
        }
    }

    public static String getExludedCharactors () {
        return System.getProperty("excludeCharNormalizedHierarchy", "[^a-zA-Z0-9>]");
    }


}
