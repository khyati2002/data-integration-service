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
        LOG.info("validation info registry" + validationInfoRegistry);
        validationExcludeGroupRegistry = new ValidationExcludeGroupRegistry(dsl);
        LOG.info("validation exclude group registry" + validationInfoRegistry);
        enrichmentInfoRegistry = new EnrichmentInfoRegistry(dsl);
        LOG.info("enrichment info registry" + enrichmentInfoRegistry);
        LOG.info("etl registry" + etlRegistry);
        dataValidationService = new DataValidationService(validationInfoRegistry,validationExcludeGroupRegistry,etlRegistry);
        dataEnrichmentService = new DataEnrichmentService(enrichmentInfoRegistry,etlRegistry);
        LOG.info("Initializing PreProcessPipelineService with DataValidationService: {} and DataEnrichmentService: {}",
                dataValidationService, dataEnrichmentService);
        preProcessPipelineService = new PreProcessPipelineService(dataValidationService, dataEnrichmentService);
        LOG.info("PreProcessPipelineService initialized successfully: {}", preProcessPipelineService);

    }

    public static synchronized UserService getInstance(DSLContext dsl) {
        if (instance == null) {
            instance = new UserService(dsl);
        }
        return instance;
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
    }

    public User getUser(User user) {
        Set<String> hierarchyStr = populateUserParentHierarchy(user);
        setHierarchy(user, hierarchyStr);
        return save(user);
    }

    public List<User> getUser(List<User> userList) {
        if(preProcessPipelineService != null) {
            LOG.info("Pre process value is not null");
            userList.forEach(user -> {
                PreProcessOperationResult operationResult = preProcessPipelineService.preProcessPipeline(user,"" );
                if (operationResult.getStatus() == PreProcessOperationResult.Status.FAILURE) {
                    throw new RuntimeException("Pre Process Pipeline Of User Failed");
                }

            });
        }
        else{
            LOG.info("Pre process called with null value");
        }
        userList.forEach(user -> {
            Set<String> hierarchyStr = populateUserParentHierarchy(user);
            setHierarchy(user, hierarchyStr);
        });

       List<User> userListSaved = batchSave(userList);
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
            user.setVerified(false);
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

    private void populateBatchRoles(List<User> userList) {
        userList.forEach(user ->
        {
            populateRoles(user);
            populateUserDetails(user);
        });
    }

    @Override
    public List<User> batchSave(List<User> userList) {
        LOG.info("User list is" + userList.size());
        populateBatchLocation(userList);
        populateBatchRoles(userList);
        userList.forEach(user -> {
            userParentService.populateAndSaveUserParent(user);
        });

        List<String> loginIds = userList.stream()
                .map(User::getLoginid)
                .collect(Collectors.toList());

        Map<String, com.salescode.dim.jooq.generated.tables.pojos.User> savedList = dsl.selectFrom(CK_USER)
                .where(CK_USER.LOGINID.in(loginIds))
                .fetch()
                .intoMap(CK_USER.LOGINID, record -> record.into(com.salescode.dim.jooq.generated.tables.pojos.User.class));

        LOG.info("Already saved List" + savedList);

        List<User> itemsToInsert = new ArrayList<>();
        List<User> itemsToUpdate = new ArrayList<>();
        for (int i = 0; i < userList.size(); i++) {
            super.addHash(userList.get(i));
            if (savedList.get(userList.get(i).getLoginid()) == null) {
                userList.get(i).setVersion(0);
                userList.get(i).setId(UUID.randomUUID().toString());
                itemsToInsert.add(userList.get(i));
            } else {
                if (!Objects.equals(userList.get(i).getHash(), savedList.get(userList.get(i).getLoginid()).getHash())) {
                    userList.get(i).setId(savedList.get(userList.get(i).getLoginid()).getId());
                    userList.get(i).setVersion(savedList.get(userList.get(i).getLoginid()).getVersion());
                    itemsToUpdate.add(userList.get(i));
                }
                else{
                    userList.get(i).setId(savedList.get(userList.get(i).getLoginid()).getId());
                    userList.get(i).setVersion(savedList.get(userList.get(i).getLoginid()).getVersion());
                }
            }
        }
        LOG.info("Items to insert" + itemsToInsert);
        LOG.info("Items to update" + itemsToUpdate);
        if (!itemsToInsert.isEmpty()) {
            dsl.batchInsert(
                    itemsToInsert.stream()
                            .map(user -> dsl.newRecord(CK_USER, user)) // Convert to jOOQ Records
                            .collect(Collectors.toList())
            ).execute();
        }

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
