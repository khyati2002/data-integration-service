package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.enrichments.EnrichmentPhase;
import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.models.enums.RoleName;
import com.applicate.services.channelkart.utils.BatchInsertUtil;
import com.applicate.services.channelkart.utils.CdmDiffUtil;
import com.salescode.dim.JooqDatabaseBatchSink;
import com.salescode.dim.cache.CacheManager;
import com.salescode.dim.cache.Cacheable;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dim.etl.enrichment.service.EnrichmentInfoRegistry;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.event.EventPublisher;
import com.salescode.dim.jooq.generated.tables.pojos.AuthRole;
import com.salescode.dim.jooq.generated.tables.pojos.CustomerAccount;
import com.salescode.dim.jooq.generated.tables.pojos.UserRoles;
import com.salescode.dim.jooq.generated.tables.pojos.Userdesignation;
import com.salescode.dim.jooq.generated.tables.records.CkUserRecord;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.Location;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.User;
import com.salescode.dim.scanner.ExternalRegistryScanner;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.zookeeper3.org.apache.zookeeper.Op;
import org.jooq.impl.DSL;
import scala.tools.ant.sabbus.Use;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.*;
import static com.salescode.dim.jooq.generated.Tables.CK_USERDESIGNATION;

public class UserService extends AbstractCDMService<User> {
    public static final String DEFAULT_ENCODED_PASSWORD = "$2a$10$GetnNjgilfLkIv.2R3nHMevLZfI9HGHWQ3iXw3nrCfJlrpePirkIi";
    public static final String NORMALIZED_CHARECTORS = "U";
    public static final String NORMALIZED_JOINING_CHARECTORS = "U>U";
    private final LocationService locationService;
    private final UserParentService userParentService;
    private final HierarchyMetadataService hierarchyMetadataService;
    private final CustomerAccountsService customerAccountsService;
    private final RoleService roleService;
    private final DataEnrichmentService dataEnrichmentService;
    private final EnrichmentInfoRegistry enrichmentInfoRegistry;
    private final ETLRegistry etlRegistry;
    public UserService(){
        ExternalRegistryScanner externalRegistryScanner = ExternalRegistryScanner.getInstance();
        etlRegistry = ETLRegistry.getInstance(externalRegistryScanner);
        locationService = new LocationService();
        userParentService = new UserParentService();
        hierarchyMetadataService = new HierarchyMetadataService();
        customerAccountsService = new CustomerAccountsService();
        roleService = new RoleService();
        enrichmentInfoRegistry = new EnrichmentInfoRegistry(getDslContext());
        dataEnrichmentService = new DataEnrichmentService(enrichmentInfoRegistry,etlRegistry);
    }
    @Cacheable(cacheName = "dataintegration-user")
    public User findByLoginId(String loginid) {
        com.salescode.dim.jooq.generated.tables.pojos.User user = getDslContext().selectFrom(CK_USER)
                .where(CK_USER.LOGINID.eq(loginid))
                .fetchOneInto(com.salescode.dim.jooq.generated.tables.pojos.User.class);
        if(user == null){
            return null;
        }
        return User.of(user);
    }



    @Cacheable(cacheName = "dataintegration-user")
    public User findByLoginIdParent(String loginid) {
        com.salescode.dim.jooq.generated.tables.pojos.User user = getDslContext().selectFrom(CK_USER)
                .where(CK_USER.LOGINID.eq(loginid))
                .fetchOneInto(com.salescode.dim.jooq.generated.tables.pojos.User.class);
        return User.of(user);
        //  return new User();
    }

    private void populateBatchLocation(List<User> userList) {
        List<Location> locationList = userList.stream()
                .map(User::getLocation)  // Assuming there's a getLocation() method// Filter out null locations
                .collect(Collectors.toList());

        List<Location> savedList = locationService.findLocationOrPersistLocation(locationList);

        for (int i = 0; i < userList.size(); i++) {
            userList.get(i).setLocationHierarchy(savedList.get(i).getLocationHierarchy());
        }
    }

    private Set<String> populateUserParentHierarchy(User user) {
        Set<String> hierarchyStr = new HashSet<>();
        if (ObjectUtils.isNotEmpty(user.getImmediateParent())) {
            Set<String> uniqueParents = user.getImmediateParent().stream().map(HierarchyMetadata::getParent).collect(Collectors.toSet());
            uniqueParents.forEach(parent -> {
                List<HierarchyMetadata> hierarchyMetaDataList = hierarchyMetadataService.findByImmediateParent(parent);


                if (hierarchyMetaDataList.isEmpty()) {
                    hierarchyStr.add(user.getLoginid() + " > " + parent + " > " + customerAccountsService.getAdminLoginId());
                } else {
                    hierarchyMetaDataList.forEach(hmList -> hierarchyStr.add(user.getLoginid() + " > "
                            + (StringUtils.isEmpty(hmList.getHierarchy())
                            ? hmList.getParent() + " > " + customerAccountsService.getAdminLoginId()
                            : hmList.getHierarchy())));
                }
            });
        }
        return hierarchyStr;
    }

    private void setHierarchy(User user, Set<String> hierarchyStr) {
        if (!hierarchyStr.isEmpty()) {
            String hierarchy = StringUtils.join(hierarchyStr, ",");
            if (StringUtils.isNotEmpty(hierarchy)) {
                user.setHierarchy(hierarchy);
                user.setNormalizedHierarchy(getNormalizedHierarchy(user.getHierarchy()));
                // Find existing hierarchies in the database
                List<HierarchyMetadata> existingHierarchies = hierarchyMetadataService.findByHierarchyIn(hierarchyStr);
                List<HierarchyMetadata> hierarchiesToUpdate = new ArrayList<>();
                for (HierarchyMetadata existingHierarchy : existingHierarchies) {
                    if (Objects.equals(existingHierarchy.getLocationHierarchy(), user.getLocationHierarchy()) && existingHierarchy.getActiveStatus() == user.getActiveStatus()) {

                    }
                    else {
                        if (!Objects.equals(existingHierarchy.getLocationHierarchy(), user.getLocationHierarchy())) {
                            existingHierarchy.setLocationHierarchy(user.getLocationHierarchy());
                        }
                        if (existingHierarchy.getActiveStatus() != user.getActiveStatus()) {
                            existingHierarchy.setActiveStatus(user.getActiveStatus());
                            existingHierarchy.setActiveStatusReason(user.getActiveStatusReason());
                        }
                        hierarchiesToUpdate.add(existingHierarchy);
                    }
                }
                // Determine which hierarchies need to be created
                Set<String> existingHierarchyStrings = existingHierarchies.stream()
                        .map(HierarchyMetadata::getHierarchy)
                        .collect(Collectors.toSet());

                // Prepare new hierarchies to create
                List<HierarchyMetadata> newHierarchies = hierarchyStr.stream()
                        .filter(hStr -> !existingHierarchyStrings.contains(hStr))
                        .map(hStr -> {
                            HierarchyMetadata hm = new HierarchyMetadata();
                            hm.setId(UUID.randomUUID().toString());
                            hm.setHierarchy(hStr);
                            hm.setActiveStatus(user.getActiveStatus());
                            // Set immediate parent as the comma-separated list of ALL hierarchies
                            String immediateParent = hierarchyStr.stream()
                                    .collect(Collectors.joining(","));
                            hm.setImmediateParent(user.getLoginid());

                            hm.setLocationHierarchy(
                                    user.getLocationHierarchy() != null ?
                                            user.getLocationHierarchy() :
                                            null
                            );
                            hm.setChanged(Boolean.TRUE);
                            return hm;
                        })
                        .collect(Collectors.toList());

                if (!hierarchiesToUpdate.isEmpty()) {
                    getDslContext().batchUpdate(
                            hierarchiesToUpdate.stream()
                                    .map(hierarchyMetadata -> getDslContext().newRecord(CK_HIERARCHY_METADATA, hierarchyMetadata)) // Convert to jOOQ Records
                                    .collect(Collectors.toList())
                    ).execute();
                }
                // Combine existing and new hierarchies
                List<HierarchyMetadata> allHierarchies = new ArrayList<>(existingHierarchies);
                allHierarchies.addAll(newHierarchies);
                user.setImmediateParent(allHierarchies);

                hierarchyMetadataService.batchSave(newHierarchies);
            } else {
                //             logger.warn("Hierarchy logs: Null hierarchy found for user {}. Skipping setHierarchy() operation", user.getLoginId());
            }
        }


    }

    private void populateBatchRoles(List<User> users) {
        List<String> roleNames = new ArrayList<>();
        Map<Integer, List<AuthRole>> userRolesMap = new HashMap<>();

        // Collect role names for batch fetching, maintaining order
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                roleNames.add(RoleName.ROLE_USER.name()); // Default role
            } else {
                for (AuthRole role : user.getRoles()) {
                    roleNames.add(role.getName());
                }
            }
            userRolesMap.put(i, user.getRoles()); // Store user roles in order
        }

        // Fetch roles in batch using a single query
        Map<String, AuthRole> fetchedRoles = roleService.getRolesByNames(roleNames).stream()
                .collect(Collectors.toMap(AuthRole::getName, role -> role));

        // Assign fetched roles back to users while maintaining order
        for (int i = 0; i < users.size(); i++) {
            List<AuthRole> assignedRoles = new ArrayList<>();
            List<AuthRole> originalRoles = userRolesMap.get(i);

            if (originalRoles == null || originalRoles.isEmpty()) {
                assignedRoles.add(fetchedRoles.get(RoleName.ROLE_USER.name()));
            } else {
                for (AuthRole role : originalRoles) {
                    if (role != null && fetchedRoles.containsKey(role.getName())) {
                        assignedRoles.add(fetchedRoles.get(role.getName()));
                    }
                }
            }
            users.get(i).setRoles(assignedRoles);
        }
    }

    private void fillUserDetails(List<User> userList) {
        for(User user : userList) {
            if (user.getVerified() == null) {
                user.setVerified(false);
            }

            if (user.getPassword() == null) {
                user.setPassword(DEFAULT_ENCODED_PASSWORD);
            }

            if(user.getBlocked()==null || !user.getBlocked()) {
                user.setBlocked(false);
            }
        }
    }


    public List<User> preBatchSave(List<User> userList) {
        populateBatchLocation(userList);

        populateBatchRoles(userList);

        userList.forEach(user -> userParentService.populateAndSaveUserParent(user));

        userList.stream().parallel().forEachOrdered(user -> {
            Set<String> hierarchyStr = populateUserParentHierarchy(user);
            setHierarchy(user, hierarchyStr);
        });
        return userList;
    }

    private void preSaveEnrichment(User user){
        OperationResult or = dataEnrichmentService.enrich(user, EnrichmentPhase.PRE_SAVE);
        if(!or.getStatus().equals(OperationResult.Status.OK)){
            throw new RuntimeException("Pre save enrichment error");
        }
    }


    private List<List<User>> getItemsToSaveList(List<User> userList){

        List<List<User>> result = new ArrayList<>();

        List<String> loginIds = userList.stream()
                .map(User::getLoginid)
                .collect(Collectors.toList());

        Map<String, com.salescode.dim.jooq.generated.tables.pojos.User> savedList = getDslContext().selectFrom(CK_USER)
                .where(CK_USER.LOGINID.in(loginIds))
                .fetch()
                .intoMap(CK_USER.LOGINID, record -> record.into(com.salescode.dim.jooq.generated.tables.pojos.User.class));

        List<User> itemsToInsert = new ArrayList<>();
        List<User> itemsToUpdate = new ArrayList<>();

        for (User user : userList) {
            fillAttributes(user,User.of(savedList.get(user.getLoginid())));
            fillCommonAttributes(user);
            fillUserDetails(userList);
            new AttributeUpdateOverrideManager().overrideAttributes(user,savedList.get(user.getLoginid()));
            if (savedList.get(user.getLoginid()) != null){
                if(savedList.get(user.getLoginid()).getPassword() != null){
                    user.setPassword(savedList.get(user.getLoginid()).getPassword());
                }
            }
            if (savedList.get(user.getLoginid()) != null && User.of(savedList.get(user.getLoginid())).getVerified()) {
                user.setVerified(true);
            }
            super.addHash(user);
            if (savedList.get(user.getLoginid()) == null) {
                preSaveEnrichment(user);
                user.setVersion(0);
                user.setId(UUID.randomUUID().toString());
                user.setOperationPerformed(ActionType.INSERT);
                user.setChanged(Boolean.TRUE);
                itemsToInsert.add(user);

            } else {
                if (!Objects.equals(user.getHash(), savedList.get(user.getLoginid()).getHash())) {
                    preSaveEnrichment(user);
                    User savedUser = User.of(savedList.get(user.getLoginid()));
                    user.setId(savedList.get(user.getLoginid()).getId());
                    user.setVersion(savedList.get(user.getLoginid()).getVersion());
                    user.setChanges(CdmDiffUtil.getChanges(user,savedUser));
                    user.setOperationPerformed(ActionType.UPDATE);
                    user.setChanged(Boolean.TRUE);
                    itemsToUpdate.add(user);
                } else {
                    user.setId(savedList.get(user.getLoginid()).getId());
                    user.setVersion(savedList.get(user.getLoginid()).getVersion());
                    user.setChanged(Boolean.TRUE);
                }
            }
        }
        result.add(itemsToInsert);
        result.add(itemsToUpdate);
        return result;
    }

    @Override
    public Collection<User> batchSave(Collection<User> usersList){
        List<User> userList = new ArrayList<>(usersList);
        preBatchSave(userList);
        List<List<User>> saveItemsList = getItemsToSaveList(userList);
        if (!saveItemsList.get(0).isEmpty()) {
            getDslContext().batchInsert(
                    saveItemsList.get(0).stream()
                            .map(user -> getDslContext().newRecord(CK_USER, user)) // Convert to jOOQ Records
                            .collect(Collectors.toList())
            ).execute();
        }

        if (!saveItemsList.get(1).isEmpty()) {
            getDslContext().batchUpdate(
                    saveItemsList.get(1).stream()
                            .map(user -> {
                                CkUserRecord record = getDslContext().newRecord(CK_USER, user);
                                record.changed(CK_USER.ID, false); // Avoid updating primary key
                                return record;
                            })
                            .collect(Collectors.toList())
            ).execute();
        }
        CacheManager.getInstance().evictAll("dataintegration-user");
        if(!saveItemsList.get(0).isEmpty() || !saveItemsList.get(1).isEmpty()){
            postBatchSave(userList);
        }

        return userList;
    }

    public void postBatchSave(List<User> savedUserList){
        List<UserRoles> roleList = setRoles(savedUserList);
        saveRoles(roleList);
        List<Userdesignation> userdesignationsList = setDesignation(savedUserList);
        saveDesignation(userdesignationsList);
        EventPublisher eventPublisher = JooqDatabaseBatchSink.JooqDatabaseBatchSinkWriter.getEventPublisher();
        savedUserList.stream().parallel().forEach(user -> {
            if(user.getOperationPerformed() != null && !user.getChanges().isEmpty()) {
                eventPublisher.publishEventAsync(
                        user.getReqId(),
                        user.getClass().getSimpleName(),
                        user.getLob(),
                        user.getChanges(),
                        user.getOperationPerformed(),
                        user.getId()
                );
            }
        });
    }

    public List<UserRoles> setRoles(List<User> userList) {
        return userList.stream()
                .map(user -> {
                    UserRoles userRoles = new UserRoles();
                    // Set the user ID
                    userRoles.setUserId(user.getId());
                    // Check if the user has any roles and set the first role's id
                    if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                        userRoles.setRolesId(user.getRoles().get(0).getId());
                    } else {
                        // Optionally set a default value or handle the case where no roles exist
                        userRoles.setRolesId(UUID.randomUUID().toString());
                    }
                    return userRoles;
                })
                .collect(Collectors.toList());
    }

    public void saveRoles(List<UserRoles> userRoles) {
        BatchInsertUtil.saveBatchWithDuplicateCheck(
                getDslContext(),
                userRoles,
                CK_USER_ROLES,
                role -> DSL.row(role.getUserId(), role.getRolesId()),
                CK_USER_ROLES.USER_ID,
                CK_USER_ROLES.ROLES_ID,
                role -> getDslContext().insertInto(CK_USER_ROLES)
                        .set(CK_USER_ROLES.USER_ID, role.getUserId())
                        .set(CK_USER_ROLES.ROLES_ID, role.getRolesId())
        );
    }

    public List<Userdesignation> setDesignation(List<User> userList) {
        return userList.stream()
                .map(user -> {
                    Userdesignation userdesignation = new Userdesignation();

                    // Set the user ID
                    userdesignation.setLoginId(user.getLoginid());

                    // Ensure designation is not null before joining
                    if (user.getDesignation() != null) {
                        userdesignation.setDesignation(user.getDesignation().stream()
                                .collect(Collectors.joining(" ")));
                    } else {
                        userdesignation.setDesignation(null); // Or set a default value if needed
                    }

                    return userdesignation;
                })
                .collect(Collectors.toList());
    }

    public void saveDesignation(List<Userdesignation> userDesignation) {
        BatchInsertUtil.saveBatchWithDuplicateCheck(
                getDslContext(),
                userDesignation,
                CK_USERDESIGNATION,
                designation -> DSL.row(designation.getLoginId(), designation.getDesignation()),
                CK_USERDESIGNATION.LOGIN_ID,
                CK_USERDESIGNATION.DESIGNATION,
                designation -> getDslContext().insertInto(CK_USERDESIGNATION)
                        .set(CK_USERDESIGNATION.LOGIN_ID, designation.getLoginId())
                        .set(CK_USERDESIGNATION.DESIGNATION, designation.getDesignation())
        );
    }

    public static String getNormalizedHierarchy(String hierarchy) {
        if (StringUtils.isBlank(hierarchy)) {
            return hierarchy;
        }
        String normalizedHierarchy = NORMALIZED_CHARECTORS + Arrays.asList(hierarchy.split(","))
                .stream().map(h -> Arrays.asList(h.split(" > "))).flatMap(List::stream)
                .collect(Collectors.toSet()).stream()
                .collect(Collectors.joining(NORMALIZED_JOINING_CHARECTORS)) + NORMALIZED_CHARECTORS;
        return removeSpecialCharacters(normalizedHierarchy);
    }

    private static String removeSpecialCharacters(String normalizedHierarchy) {
        String exludedCharactors = getExludedCharactors();
        try {
            return normalizedHierarchy.replaceAll(exludedCharactors, "");
        } catch (Exception e) {
            //  logger.error("Exception happend while removing special charactors {} in normalized hierarchy {}",exludedCharactors,normalizedHierarchy);
            return normalizedHierarchy;
        }
    }

    public static String getExludedCharactors() {
        return System.getProperty("excludeCharNormalizedHierarchy", "[^a-zA-Z0-9>]");
    }
}
