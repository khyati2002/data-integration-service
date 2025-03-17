package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.RoleName;
import com.salescode.dim.PreProcessOperationResult;
import com.salescode.dim.PreProcessPipelineService;
import com.salescode.dim.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dim.etl.enrichment.service.EnrichmentInfoRegistry;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.etl.validation.service.DataValidationService;
import com.salescode.dim.etl.validation.service.ValidationExcludeGroupRegistry;
import com.salescode.dim.etl.validation.service.ValidationInfoRegistry;
import com.salescode.dim.jooq.generated.tables.pojos.AuthRole;
import com.salescode.dim.jooq.generated.tables.records.CkUserRecord;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.Location;
import com.salescode.dim.jooq.impl.User;
import com.salescode.dim.scanner.ExternalRegistryScanner;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.jooq.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_USER;
import static com.salescode.dim.jooq.generated.Tables.CK_USERDESIGNATION;

public class UserService extends AbstractCDMService<User> {
    private static UserService instance;
    private static CustomerAccountsService customerAccountsService;
    private static HierarchyMetadataService hierarchyMetadataService;
    private static LocationService locationService;
    private static RoleService roleService;
    private static UserParentService userParentService;
    private final DSLContext dsl;
    private static DataValidationService dataValidationService;
    private static DataEnrichmentService dataEnrichmentService;
    private final ExternalRegistryScanner externalRegistryScanner = ExternalRegistryScanner.getInstance();
    private static PreProcessPipelineService preProcessPipelineService;
    private final ValidationInfoRegistry validationInfoRegistry;
    private final ValidationExcludeGroupRegistry validationExcludeGroupRegistry;
    private final EnrichmentInfoRegistry enrichmentInfoRegistry;
    private final ETLRegistry etlRegistry = ETLRegistry.getInstance(externalRegistryScanner);

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    public static final String DEFAULT_ENCODED_PASSWORD = "$2a$10$GetnNjgilfLkIv.2R3nHMevLZfI9HGHWQ3iXw3nrCfJlrpePirkIi";

    public static final String NORMALIZED_CHARECTORS = "U";

    public static final String NORMALIZED_JOINING_CHARECTORS = "U>U";

    public UserService(DSLContext dsl) {
        super(dsl);
        System.out.println("UserService constructor executed!");
        this.dsl = dsl;
        customerAccountsService = new CustomerAccountsService(dsl);
        userParentService = new UserParentService(dsl);
        roleService = new RoleService(dsl);
        hierarchyMetadataService = new HierarchyMetadataService(dsl);
        locationService = new LocationService(dsl);
        validationInfoRegistry = new ValidationInfoRegistry(dsl);
        validationExcludeGroupRegistry = new ValidationExcludeGroupRegistry(dsl);
        enrichmentInfoRegistry = new EnrichmentInfoRegistry(dsl);
        dataValidationService = new DataValidationService(validationInfoRegistry,validationExcludeGroupRegistry,etlRegistry);
        dataEnrichmentService = new DataEnrichmentService(enrichmentInfoRegistry,etlRegistry);
        preProcessPipelineService = new PreProcessPipelineService(dataValidationService, dataEnrichmentService);
        userParentService = new UserParentService(dsl);
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
                user.setNormalizedHierarchy(com.applicate.services.channelkart.services.UserService.getNormalizedHierarchy(user.getHierarchy()));
            } else {
                //             logger.warn("Hierarchy logs: Null hierarchy found for user {}. Skipping setHierarchy() operation", user.getLoginId());
            }
        }

        List<HierarchyMetadata> hierarchyMetadataList = hierarchyStr.stream().map(hStr -> {
            HierarchyMetadata hm = new HierarchyMetadata();
            hm.setHierarchy(hStr);
            hm.setImmediateParent(user.getLoginid());
            hm.setLocationHierarchy(user.getLocation() != null ? user.getLocation().getLocationHierarchy() : null);
            return hm;
        }).collect(Collectors.toList());

        hierarchyMetadataService.batchSave(hierarchyMetadataList);
    }

    public User getUser(User user) {
//        PreProcessOperationResult operationResult = preProcessPipelineService.preProcessPipeline(user,"" );
//        if (operationResult.getStatus() == PreProcessOperationResult.Status.FAILURE) {
//            LOG.info(user.getLoginid());
//            throw new RuntimeException("Pre Process Pipeline Of User Failed");
//        }
        Set<String> hierarchyStr = populateUserParentHierarchy(user);
        setHierarchy(user, hierarchyStr);
        return save(user);
    }

    public List<User> getUser(List<User> userList) {
        long startTime = System.currentTimeMillis();

        if (preProcessPipelineService != null) {
            LOG.info("Pre process value is not null");
            long preProcessStartTime = System.currentTimeMillis();

            userList.forEach(user -> {
                LOG.info(user.getLoginid());
                PreProcessOperationResult operationResult = preProcessPipelineService.preProcessPipeline(user, null);
//            if (operationResult.getStatus() == PreProcessOperationResult.Status.FAILURE) {
//                throw new RuntimeException("Pre Process Pipeline Of User Failed");
//            }
            });

            long preProcessEndTime = System.currentTimeMillis();
            LOG.info("Pre-process time: " + (preProcessEndTime - preProcessStartTime) + " ms");
        } else {
            LOG.info("Pre process called with null value");
        }

        populateBatchLocation(userList);

        userList.forEach(user -> {
            Set<String> hierarchyStr = populateUserParentHierarchy(user);
            setHierarchy(user, hierarchyStr);
        });

        long batchSaveStartTime = System.currentTimeMillis();
        List<User> userListSaved = batchSave(userList);
        long batchSaveEndTime = System.currentTimeMillis();
        LOG.info("Batch save time: " + (batchSaveEndTime - batchSaveStartTime) + " ms");

        long endTime = System.currentTimeMillis();
        LOG.info("Total getUser execution time: " + (endTime - startTime) + " ms");

        return userListSaved;
    }


    private void populateUserLocation(User user) {
        if (user.getLocation() != null) {
            try {
                Location loc = user.getLocation();
                loc = locationService.findLocationOrPersistLocation(loc);
                user.setLocationHierarchy(loc);
            } catch (Exception ex) {
                throw new IllegalStateException("Error occured while setting location for user: " + user.getLoginid(), ex);
            }
        } else {
            throw new IllegalStateException("Missing location data. Data cannot be saved without location information for user : " + user.getLoginid());
        }
    }

    private void populateRoles(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            List<AuthRole> roles = roleService.getRoleAsList(RoleName.ROLE_USER.name());
            user.setRoles(roles);
        } else {
            List<AuthRole> roles = new ArrayList<>();
            for (AuthRole objectRole : user.getRoles()) {
                roleService.getRole(objectRole.getName()).ifPresent(elem -> {
                    if (!roles.contains(elem)) {
                        roles.add(elem);
                    }
                });
            }
            user.setRoles(roles);
        }
    }

    private void populateUserDetails(User user) {
        if (user.getVerified() == null) {
            user.setVerified(Byte.parseByte("0"));
        }

        if (user.getPassword() == null) {
            user.setPassword(DEFAULT_ENCODED_PASSWORD);
        }
        user.setBlocked(false);

    }

    private User fillUser(User user) {
        populateUserLocation(user);
        populateRoles(user);
        populateUserDetails(user);
        return user;
    }

    private void beforeSave(User user) {
        User u = fillUser(user);
        userParentService.populateAndSaveUserParent(user);
    }

    public User save(User user) {
        beforeSave(user);
        super.addHash(user);
        com.salescode.dim.jooq.generated.tables.pojos.User savedObj = dsl.selectFrom(CK_USER)
                .where(CK_USER.LOGINID.eq(user.getLoginid()))
                .fetchOneInto(com.salescode.dim.jooq.generated.tables.pojos.User.class);

        if (savedObj != null) {
            user.setId(savedObj.getId());
            savedObj.setVersion(savedObj.getVersion() + 1);
        } else {
            user.setId(UUID.randomUUID().toString());
            user.setVersion(0);
        }
        if (savedObj!=null && Objects.equals(savedObj.getHash(), user.getHash())) {
            return user;
        }

        CkUserRecord record = dsl.newRecord(CK_USER, user);
        dsl.insertInto(CK_USER)
                .set(record)
                .onDuplicateKeyUpdate()
                .set(record)
                .execute();

        return user;
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
            //    logger.error("Exception happend while removing special charactors {} in normalized hierarchy {}",exludedCharactors,normalizedHierarchy);
            return normalizedHierarchy;
        }
    }

    public static String getExludedCharactors() {
        return System.getProperty("excludeCharNormalizedHierarchy", "[^a-zA-Z0-9>]");
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

        users.forEach(user ->
        {
            populateUserDetails(user);
        });
    }

    @Override
    public List<User> batchSave(List<User> userList) {
        long startTime = System.nanoTime();
        LOG.info("User list size: " + userList.size());

        long startLocationTime = System.nanoTime();
        long endLocationTime = System.nanoTime();
        LOG.info("Time taken for populateBatchLocation: " + (endLocationTime - startLocationTime) / 1_000_000 + " ms");

        long startRolesTime = System.nanoTime();
        populateBatchRoles(userList);
        long endRolesTime = System.nanoTime();
        LOG.info("Time taken for populateBatchRoles: " + (endRolesTime - startRolesTime) / 1_000_000 + " ms");

        long startParentTime = System.nanoTime();
        userList.forEach(user -> userParentService.populateAndSaveUserParent(user));
        long endParentTime = System.nanoTime();
        LOG.info("Time taken for populateAndSaveUserParent: " + (endParentTime - startParentTime) / 1_000_000 + " ms");

        long startFetchTime = System.nanoTime();
        List<String> loginIds = userList.stream()
                .map(User::getLoginid)
                .collect(Collectors.toList());

        Map<String, com.salescode.dim.jooq.generated.tables.pojos.User> savedList = dsl.selectFrom(CK_USER)
                .where(CK_USER.LOGINID.in(loginIds))
                .fetch()
                .intoMap(CK_USER.LOGINID, record -> record.into(com.salescode.dim.jooq.generated.tables.pojos.User.class));
        long endFetchTime = System.nanoTime();
        LOG.info("Time taken for fetching existing users: " + (endFetchTime - startFetchTime) / 1_000_000 + " ms");

        LOG.info("Already saved List: " + savedList);

        List<User> itemsToInsert = new ArrayList<>();
        List<User> itemsToUpdate = new ArrayList<>();

        long startProcessingTime = System.nanoTime();
        for (User user : userList) {
            super.addHash(user);
            if (savedList.get(user.getLoginid()) == null) {
                user.setVersion(0);
                user.setId(UUID.randomUUID().toString());
                itemsToInsert.add(user);
            } else {
                if (!Objects.equals(user.getHash(), savedList.get(user.getLoginid()).getHash())) {
                    user.setId(savedList.get(user.getLoginid()).getId());
                    user.setVersion(savedList.get(user.getLoginid()).getVersion());
                    itemsToUpdate.add(user);
                } else {
                    user.setId(savedList.get(user.getLoginid()).getId());
                    user.setVersion(savedList.get(user.getLoginid()).getVersion());
                }
            }
        }
        long endProcessingTime = System.nanoTime();
        LOG.info("Time taken for processing users: " + (endProcessingTime - startProcessingTime) / 1_000_000 + " ms");

        LOG.info("Items to insert: " + itemsToInsert.size());
        LOG.info("Items to update: " + itemsToUpdate.size());

        long startInsertTime = System.nanoTime();
        if (!itemsToInsert.isEmpty()) {
            dsl.batchInsert(
                    itemsToInsert.stream()
                            .map(user -> dsl.newRecord(CK_USER, user)) // Convert to jOOQ Records
                            .collect(Collectors.toList())
            ).execute();
        }
        long endInsertTime = System.nanoTime();
        LOG.info("Time taken for batch insert: " + (endInsertTime - startInsertTime) / 1_000_000 + " ms");

        long startUpdateTime = System.nanoTime();
        if (!itemsToUpdate.isEmpty()) {
            dsl.batchUpdate(
                    itemsToUpdate.stream()
                            .map(user -> {
                                CkUserRecord record = dsl.newRecord(CK_USER, user);
                                record.changed(CK_USER.ID, false); // Avoid updating primary key
                                return record;
                            })
                            .collect(Collectors.toList())
            ).execute();
        }
        long endUpdateTime = System.nanoTime();
        LOG.info("Time taken for batch update: " + (endUpdateTime - startUpdateTime) / 1_000_000 + " ms");

        long endTime = System.nanoTime();
        LOG.info("Total time taken for batchSave: " + (endTime - startTime) / 1_000_000 + " ms");

        return userList;
    }


    public com.salescode.dim.jooq.generated.tables.pojos.User findByLoginIdUser(String loginid){
       return dsl.selectFrom(CK_USER)
                .where(CK_USER.LOGINID.eq(loginid))
                .fetchOneInto(com.salescode.dim.jooq.generated.tables.pojos.User.class);

    }

    public User findByLoginId(String loginid){
        return dsl.selectFrom(CK_USER)
                .where(CK_USER.LOGINID.eq(loginid))
                .fetchOneInto(User.class);


    }

    public Set<String> getDesignation(String loginid) {
        return dsl.select(CK_USERDESIGNATION.DESIGNATION)
                .from(CK_USERDESIGNATION)
                .where(CK_USERDESIGNATION.LOGIN_ID.eq(loginid))
                .fetchSet(CK_USERDESIGNATION.DESIGNATION);
    }


    public Optional<List<User>> findByMobileSafely(String mobile) {
        List<User> users= dsl.selectFrom(CK_USER)
                .where(CK_USER.MOBILE.eq(mobile))
                .fetchInto(User.class);
        return CollectionUtils.isEmpty(users)? Optional.empty() : Optional.of(users);
    }
}
