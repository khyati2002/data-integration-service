package com.salescode.channelkart.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.RawValue;
import com.salescode.channelkart.converters.HierarchyMetaDataToStringConverter;
import com.salescode.channelkart.converters.LocationToStringConverter;
import com.salescode.channelkart.converters.StringToLocationConverter;
import com.salescode.channelkart.models.diff.Change;
import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.channelkart.models.enums.ApplicationCategory;
import com.salescode.channelkart.models.enums.RoleName;
import com.salescode.channelkart.repository.OutletDetailsRepository;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.channelkart.utils.JSONUtils;
import com.salescode.channelkart.utils.NullUtils;
import com.salescode.dataintegration.etl.metadata.registry.MetadataRegistry;
import com.salescode.jooq.generated.tables.pojos.CkAuthRole;
import com.salescode.jooq.generated.tables.pojos.CkHierarchyMetadata;
import com.salescode.jooq.generated.tables.pojos.CkLocation;
import com.salescode.jooq.generated.tables.pojos.CkMetadata;
import com.salescode.jooq.impl.CkOutletDetails;
import com.salescode.jooq.impl.CkUser;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.salescode.jooq.generated.Tables.CK_CUSTOMER_ACCOUNT;
import static com.salescode.jooq.generated.tables.CkHierarchyMetadata.CK_HIERARCHY_METADATA;
import static com.salescode.jooq.generated.tables.CkOutletDetails.CK_OUTLET_DETAILS;
import static com.salescode.jooq.generated.tables.CkOutletDetailsHierarchymetadata.CK_OUTLET_DETAILS_HIERARCHYMETADATA;
import static com.salescode.jooq.generated.tables.CkUserRoles.CK_USER_ROLES;
import static com.salescode.jooq.generated.tables.CkUserdesignation.CK_USERDESIGNATION;


@Service
public class OutletDetailsService extends AbstractCDMService<CkOutletDetails> {

    public static final String RETAILER = "retailer";
    private static final String WHOLESALER = "wholesaler";

    private final DSLContext dsl;
    private final SupplierInfoService supplierInfoService;
    private final RoleService roleService;
    private final MetadataRegistry metadataRegistry;
    private final UserService userService;
    private final HierarchyMetaDataService hierarchyMetaDataService;
    private final HierarchyMetaDataToStringConverter hierarchyMetaDataToStringConverter;
    private LocationService locationService;
    private OutletDetailsRepository outletDetailsRepository;
    @Autowired private LocationToStringConverter locationToStringConverter;
    @Autowired private StringToLocationConverter stringToLocationConverter;
    @Autowired
    public OutletDetailsService(DSLContext dsl, HierarchyMetaDataToStringConverter hierarchyMetaDataToStringConverter, UserService userService, SupplierInfoService supplierInfoService, RoleService roleService, MetadataRegistry metadataRegistry, HierarchyMetaDataService hierarchyMetaDataService, LocationService locationService, OutletDetailsRepository outletDetailsRepository) {

        this.dsl = dsl;
        this.hierarchyMetaDataToStringConverter = hierarchyMetaDataToStringConverter;
        this.userService = userService;
        this.supplierInfoService = supplierInfoService;
        this.roleService = roleService;
        this.metadataRegistry = metadataRegistry;
        this.hierarchyMetaDataService = hierarchyMetaDataService;
        this.locationService = locationService;
        this.outletDetailsRepository = outletDetailsRepository;
    }

    private CustomerAccountsService getCustomerAccountsService() {
        return SpringContext.getBean(CustomerAccountsService.class);
    }


    public void createAssociatedData(CkOutletDetails outlet) {
        if (outlet.getUserName() != null) {
            addAssociatedData(outlet);
        } else if (getClientProperty("application.category").equals(ApplicationCategory.RETAIL.name())) {
            createRetailUser(outlet);
        }
    }

    public String getClientProperty(String property) {
        final String[] result = {null};
        CkMetadata metaConfig = metadataRegistry.getMetadataByDomainNameAndType("client", "properties").orElse(null);
        if (metaConfig != null) {
            metaConfig.getDomainValues().forEach(pair -> {
                String str = pair.get("name").toString();
                String name = str.substring(1, str.length() - 1);
                String val = pair.get("value").asText();
                if (property.equalsIgnoreCase(name)) {
                    result[0] = pair.get("value").asText();
                }
            });
        }
        return result[0];
    }

    public CkOutletDetails prepareOutletDetails(CkOutletDetails outletDetails) {
//        /* location */
//        refreshLocation(outletDetails);
//
//        /* Retailer Info/Username */
//        TimerUtils.withTime("prepareOutletDetails Time taken to fillRetailer ",
//                () -> fillRetailer(outletDetails, false));
//     fillRetailer(outletDetails, false);
       return outletDetails;
    }


    public void saveOutletHierarchyMetadata(CkOutletDetails cdmOutletDetails) {
        List<CkHierarchyMetadata> hierarchy = cdmOutletDetails.getImmediateParent();
        hierarchy.forEach(elem-> {
            Collection<CkHierarchyMetadata> list = hierarchyMetaDataService.findByImmediateParent(elem.getParent());
            list.forEach(item -> {
                if(item.getHierarchy().equals(elem.getHierarchy())) {
                    boolean exists = dsl.fetchExists(dsl.selectFrom(CK_OUTLET_DETAILS_HIERARCHYMETADATA).where(CK_OUTLET_DETAILS_HIERARCHYMETADATA.OUTLET_ID.eq(cdmOutletDetails.getId())).and(CK_OUTLET_DETAILS_HIERARCHYMETADATA.HIERARCHY_METADATA_ID.eq(item.getId())));
                    if(exists == false) {
                        var record = dsl.newRecord(CK_OUTLET_DETAILS_HIERARCHYMETADATA);
                        record.set(CK_OUTLET_DETAILS_HIERARCHYMETADATA.OUTLET_ID, cdmOutletDetails.getId());
                        record.set(CK_OUTLET_DETAILS_HIERARCHYMETADATA.HIERARCHY_METADATA_ID, item.getId());
                        dsl.insertInto(CK_OUTLET_DETAILS_HIERARCHYMETADATA).set(record).execute();
                    }
                }
            });
            });
    }

    public void saveLocation(CkOutletDetails cdmObject){
        CkLocation location = locationService.findLocationOrPersistLocation(cdmObject.getLocation());
        cdmObject.setLocation(location);
        cdmObject.setLocationHierarchy(locationToStringConverter.convert(location));
    }
    @Override
    public CkOutletDetails save(CkOutletDetails cdmObject) {

//        TimerUtils.withTime("Time taken to createAssociatedData new_record ",
//               () ->
        saveLocation(cdmObject);
        if (cdmObject.getId() == null) {
             cdmObject.setId(UUID.randomUUID().toString());
        }
        createAssociatedData(cdmObject);
//      printLogsForNullHierarchy(cdmObject,"Location null before prepare outlet details");
//        CkOutletDetails tempoutlet = TimerUtils.withTime("Time taken to prepareOutletDetails new_record ",
//                k -> prepareOutletDetails(cdmObject));
        String outlet = cdmObject.toString();
        CkOutletDetails tempoutlet = prepareOutletDetails(cdmObject);
        List<CkHierarchyMetadata> immediateParents = tempoutlet.getImmediateParent();
        if (immediateParents != null && !immediateParents.isEmpty()) {
            List<CkHierarchyMetadata> existingMetadata = new ArrayList<>();
            List<CkHierarchyMetadata> newMetadata = new ArrayList<>();
            for (CkHierarchyMetadata hierarchyMetadata : immediateParents) {
                populateHierarchy(hierarchyMetadata, existingMetadata, newMetadata, tempoutlet);
            }
            if (!newMetadata.isEmpty()) {
//               List<CkHierarchyMetadata> savedData = hierarchyMetaDataService.batchSave(newMetadata);
//                existingMetadata.addAll(savedData);
            }
            if (!existingMetadata.isEmpty()) tempoutlet.setImmediateParent(existingMetadata);
        }
////      printLogsForNullHierarchy(tempoutlet,"Location null before setting hierarchy");
          setHierarchy(tempoutlet);
////
          tempoutlet.setNormalizedHierarchy(UserService.getNormalizedHierarchy(tempoutlet.getHierarchy()));
////
//        TimerUtils.withTime("Time taken to set supplier ",
//                () -> {
        try {
            setOutletSupplier(tempoutlet);
        } catch (JsonProcessingException e) {
////       logger.error("Error while setting supplier in outlet extended attribute");
        }
//                });
////      printLogsForNullHierarchy(tempoutlet,"Location null before saving outlet");

//        setImmediateParent(cdmObjectDetails);

        var record = dsl.newRecord(CK_OUTLET_DETAILS, tempoutlet);
        if (record.get(CK_OUTLET_DETAILS.ID) == null) record.set(CK_OUTLET_DETAILS.ID, tempoutlet.getOutletcode());
        if (record.get(CK_OUTLET_DETAILS.VERSION) == null) record.set(CK_OUTLET_DETAILS.VERSION, 1);
        if (record.get(CK_OUTLET_DETAILS.MAPPED) == null) record.set(CK_OUTLET_DETAILS.MAPPED, true);
        if(record.get(CK_OUTLET_DETAILS.CREATION_TIME) == null ) record.set(CK_OUTLET_DETAILS.CREATION_TIME,  Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));

        dsl.insertInto(CK_OUTLET_DETAILS).set(record).onDuplicateKeyUpdate().set(record).execute();

        if (tempoutlet != null && tempoutlet.getImmediateParent() != null && tempoutlet.getImmediateParent().size() > 0) {
             saveOutletHierarchyMetadata(tempoutlet);
        }
        CkOutletDetails saved = super.save(tempoutlet);
        String logMessage = String.format("OutletDetails is updated for id '%s', outletcode '%s', last updated on '%s', modified by '%s'", saved.getId(), saved.getOutletcode(), saved.getLastModifiedTime(), saved.getModifiedBy());
//       AuditLogger.log("OutletDetails Updated",logMessage, AuditLogger.Status.SUCCESS, "OutletDetails", AuditLogger.Operations.UPDATE.toString(),null);
//        if (ObjectUtils.isEmpty(outletDetails.getChanges())) {
//           AuditLogger.log(LOG_TYPE, "Created new Outlet with outletCode '{}'", outletDetails.getOutletCode());
//       } else {
//            AuditLogger.log(LOG_TYPE, "Outlet with outletCode '{}' updated with data '{}'",
//                    outletDetails.getOutletCode(), EntityUtils.getDataChanges(outletDetails.getChanges()));
//        }
//       clearCache(SecurityContextUtils.getLob(), outletDetails);
//
//              .execute();
//        userDetailsService.save(cdmObject);
        return cdmObject;
    }


    //SAVE IN USER entity


    //hierarchy 'outletcode > admin@applicate.in'

    /// /
    /// /    void printLogsForNullHierarchy(CkOutletDetails outletDetails, String message){
    /// /        message += " for outletCode {}";
    /// /        if(propertyRegistry.getAsBoolean(PropertyDefinition.LOGS_FOR_NULL_LOCATION) &&
    /// /                (outletDetails.getLocationHierarchy()==null || outletDetails.getLocationHierarchy().getLocationHierarchy()==null)){
    /// /            logger.error(message, outletDetails.getOutletcode());
    /// /        }
    /// /    }
    /// /
    /// /    public CkOutletDetails prepareOutletDetails(CkOutletDetails outletDetails) {
    /// /        /* location */
    /// /        refreshLocation(outletDetails);
    /// /
    /// /        /* Retailer Info/Username */
    /// /        TimerUtils.withTime("prepareOutletDetails Time taken to fillRetailer ",
    /// /                () -> fillRetailer(outletDetails, false));
    /// /
    /// /        return outletDetails;
    /// /    }
    /// /


    private void setDesignation(Set<String> designation, CkUser user) {
        String Designation = (designation != null) ? designation.stream().map(String::toLowerCase).collect(Collectors.joining(",")) : null;
        boolean exists = dsl.fetchExists(dsl.selectFrom(CK_USERDESIGNATION).where(CK_USERDESIGNATION.LOGIN_ID.eq(user.getLoginid())));
      if(exists == false) {
            var new_record = dsl.newRecord(CK_USERDESIGNATION);
            new_record.set(CK_USERDESIGNATION.LOGIN_ID, user.getLoginid());
            new_record.set(CK_USERDESIGNATION.DESIGNATION, Designation);
            dsl.insertInto(CK_USERDESIGNATION)
                  .set(new_record)
                  .onDuplicateKeyUpdate()
                  .set(new_record)
                  .execute();
        }
      else{
          dsl.update(CK_USERDESIGNATION)
                  .set(CK_USERDESIGNATION.DESIGNATION,Designation)
                  .where(CK_USERDESIGNATION.LOGIN_ID.eq(user.getLoginid()))
                  .execute();
      }
    }


    private void setRoles(List<CkAuthRole> roles, CkUser user) {
        for (CkAuthRole role : roles) {
            boolean exists = dsl.fetchExists(dsl.selectFrom(CK_USER_ROLES).where(CK_USER_ROLES.USER_ID.eq(user.getLoginid())));
         if(exists == false) {
                dsl.insertInto(CK_USER_ROLES).values(user.getId(), role.getId()).execute();
            }
        }
    }

    private void createRetailUser(CkOutletDetails outlet) {
        CkUser user = new CkUser();
        user.setActiveStatus(outlet.getActiveStatus());
        user.setId(outlet.getOutletcode());
        user.setLoginid(outlet.getOutletcode());
        user.setVersion(outlet.getVersion());
        user.setUseraccountid(outlet.getOutletcode());
        user.setLocationHierarchy(outlet.getLocationHierarchy());
        user.setMobile(outlet.getContactno());
        user.setName(StringUtils.isEmpty(outlet.getOutletName()) ? outlet.getOutletcode() : outlet.getOutletName());
        user.setImmediateParent(replicateRetailerOutletParent(outlet.getImmediateParent()));
        user.setDesignation(Set.of(RETAILER));
        setDesignation(user.getDesignation(), user);
        List<CkAuthRole> roles = roleService.getRoleAsList(RoleName.ROLE_USER.name());
        user.setRoles(roles);
        outlet.setUserName(user);
        outlet.setImmediateParent(new ArrayList<>(1));
        createAssociateDataWithLock(outlet);
        setRoles(user.getRoles(), user);
    }


    public void saveUserHierarchyMetadata(CkHierarchyMetadata hierarchy){

        CkHierarchyMetadata refreshedHier = hierarchyMetaDataService.refresh(hierarchy);
        if(refreshedHier.getId() == null) {
            refreshedHier.setId(UUID.randomUUID().toString());
        }

        var new_record = dsl.newRecord(CK_HIERARCHY_METADATA,refreshedHier);
        if(new_record.get(CK_OUTLET_DETAILS.CREATION_TIME) == null ) new_record.set(CK_OUTLET_DETAILS.CREATION_TIME,  Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
            dsl.insertInto(CK_HIERARCHY_METADATA)
                    .set(new_record)
                    .onDuplicateKeyUpdate()
                    .set(new_record)
                    .execute();
        }



    private void createAssociateDataWithLock(CkOutletDetails outlet) {
//        User user = TimerUtils.withTime("Time taken to execute getUser([[" + getUserName(outlet).getLoginId()
//                + "]]) for outlet[[" + outlet.getOutletcode() + "]]", k -> getUser(outlet.getUserName()));
        CkUser user = getUser(outlet.getUserName());
        System.out.println(user);
        outlet.setUserName(user);
        if(user.getDesignation() != null && (user.getDesignation().contains(RETAILER) || user.getDesignation().contains(WHOLESALER))) {
            outlet.setActiveStatus(outlet.getUserName().getActiveStatus());
            outlet.setActiveStatus(outlet.getUserName().getActiveStatus());
        }
        if (ObjectUtils.isNotEmpty(user.getImmediateParent())) {
            List<CkHierarchyMetadata> hms = user.getImmediateParent().stream().map(parent -> {
                CkHierarchyMetadata hm = new CkHierarchyMetadata();
                if(user.getHierarchy() != null){
                    hm.setHierarchy(user.getHierarchy());
                }
                else {
                    hm.setHierarchy(user.getLoginid() + " > " + (StringUtils.isEmpty(parent.getHierarchy()) ? parent.getParent() + " > " + getAdminLoginId() : parent.getHierarchy()));
                }
                hm.setLocationHierarchy(user.getLocationHierarchy());
                hm.setParent(user.getLoginid());
                hm.setActiveStatus(user.getActiveStatus());
                hm.setVersion(1);
                saveUserHierarchyMetadata(hm);
                return hm;
            }).collect(Collectors.toList());
            // If outlet defines its own immediate parent then it should not override by
            // user.
            // The whole hierarchy should be take care by populate hierarchy in next steps.
            if (ObjectUtils.isEmpty(outlet.getImmediateParent())) {
                outlet.setImmediateParent(hms);
            }
        }
    }


    private CkUser getUser(CkUser user) {
        CkUser out;
//        User od = TimerUtils.withTime("Time taken to execute findUserByLogindId:[[ " + user.getLoginId() + "]]",
//                k -> userService.refresh(user));
        CkUser od = userService.refresh(user);
        if (od.getId() == null) {
            od = validateAndGetUser(user);
//            User u = TimerUtils.withTime(
//                    "Time taken to execute findUserByLogindId:[[" + user.getLoginId() + "]] with disabled cache",
//                    k -> userService.findByLoginId(user.getLoginId(), false));
            CkUser u = userService.findByLoginId(user.getLoginid(), false);
            if (u != null) {
                out = u;
                out.setImmediateParent(od.getImmediateParent());
            } else {
                out = od;
            }
        } else {
            out = updateUser(od);
        }
        return out;
    }

    private CkUser updateUser(CkUser user) {
        CkUser out;
        CkUser outUser;
        // Non-retailers which are already present in system must not be uploaded as
        // retailer.
//      logger.debug("Existing user hash for update user : {}, user can hash : {}, user hash : {}", existingHash,
//                    user.canHash(), user.canHash() ? user.hash() : "cannot hash");
            outUser = validateAndGetUser(user);

//            User u = TimerUtils.withTime(
//                    "Time taken to execute findUserByLogindId:[[" + user.getLoginId() + "]] with disabled cache",
//                    k -> ufalse);serService.findByLoginId(user.getLoginId(), false));
            CkUser u = userService.findByLoginId(user.getLoginid(),false);
            u.setLocation(outUser.getLocation());
            u.setImmediateParent(outUser.getImmediateParent());

            if (u != null) {
                out = u;
            } else {
                //out = outUser;
                out = outUser;
            }
            return out;
        }


    private CkUser validateAndGetUser(CkUser user) {
//        OperationResponse<CkUser> operationResponse = TimerUtils.withTime(
//                "Time Taken to exceute preprocess pipeline for class:[[" + user.getClass() + "]]",
//                k -> pipelineService.process(user));

//        if (operationResponse.getStatus().compareTo(OperationStatus.Success) == 0) {
        Set<String> hierarchyStr = populateUserParentHierarchy(user);
        if (!hierarchyStr.isEmpty()) {
            String hierarchy = StringUtils.join(hierarchyStr, ",");
            if (StringUtils.isNotEmpty(hierarchy)) {
                user.setHierarchy(hierarchy);
                user.setNormalizedHierarchy(UserService.getNormalizedHierarchy(user.getHierarchy()));
            } else {
                //  logger.warn("Hierarchy logs: Null hierarchy found for user {}. Skipping setHierarchy() operation", user.getLoginId());
            }
        }
//            return TimerUtils.withTime("Time taken to execute UserService.save[[" + user.getLoginid() + "]]",
//                    k -> userService.save(user));

        return userService.save(user);
//        } else {
//            throw new PreprocessFailedException(pipelineService.getError(operationResponse), operationResponse);
//        }
    }

    private String getAdminLoginId() {
        return dsl.select(CK_CUSTOMER_ACCOUNT.USERNAME).from(CK_CUSTOMER_ACCOUNT).fetchOneInto(String.class);
    }


    private Set<String> populateUserParentHierarchy(CkUser user) {
        Set<String> hierarchyStr = new HashSet<>();
        List<CkHierarchyMetadata> hierarchy = user.getImmediateParent();
        if (ObjectUtils.isNotEmpty(hierarchy)) {
            Set<String> uniqueParents = user.getImmediateParent().stream().map(CkHierarchyMetadata::getParent).collect(Collectors.toSet());
            uniqueParents.forEach(parent -> {
                List<CkHierarchyMetadata> hierarchyMetaDataList = (List) hierarchyMetaDataService.findByImmediateParent(parent);
                if (hierarchyMetaDataList.isEmpty()) {
                    hierarchyStr.add(user.getLoginid() + " > " + parent + " > " + getAdminLoginId());
                } else {
                    hierarchyMetaDataList.forEach(hmList -> hierarchyStr.add(user.getLoginid() + " > "
                            + (StringUtils.isEmpty(hmList.getHierarchy())
                            ? hmList.getParent() + " > " + getAdminLoginId()
                            :  hmList.getHierarchy())));
                }
            });
        }
        return hierarchyStr;
    }


    //
    private void addAssociatedData(CkOutletDetails outlet) {

        if (getClientProperty("application.category").equals(ApplicationCategory.RETAIL.name()) && (outlet.getUserName().getDesignation().contains(RETAILER) || outlet.getUserName().getDesignation().contains(WHOLESALER))) {
            outlet.getChanges().forEach(changed ->

                    setUserAssociateData(outlet, changed));
        }
//        GlobalLock.withLock(outlet.getUserName().getLoginId(), s ->
//                TimerUtils.withTime("Time taken to execute updateUser([[" + outlet.getUserName().getLoginId() + "]]) for outlet[[" + outlet.getOutletCode() + "]]", () -> createAssociateDataWithLock(outlet,cdmObjectDetails)));
        createAssociateDataWithLock(outlet);
    }

    private void setUserAssociateData(CkOutletDetails outlet, Change<Serializable> changed) {
        if (changed.getName().equalsIgnoreCase("activestatus")) {
            outlet.getUserName().setActiveStatus(outlet.getActiveStatus());
        }
        if (changed.getName().equalsIgnoreCase("outletname")) {
            outlet.getUserName().setName(outlet.getOutletName());
        }
        if (changed.getName().equalsIgnoreCase("contactno")) {
            outlet.getUserName().setMobile(outlet.getContactno());
        }
        if (changed.getName().equalsIgnoreCase("location")) {
            outlet.getUserName().setLocation(outlet.getLocation());
            outlet.getUserName().setLocationHierarchy(outlet.getLocationHierarchy());
        }
        if (changed.getName().equalsIgnoreCase("immediateparent")) {
            List<CkHierarchyMetadata> parentList = outlet.getImmediateParent().stream().filter(h -> !h.getParent().equalsIgnoreCase(outlet.getUserName().getLoginid())).collect(Collectors.toList());
            if (!parentList.isEmpty()) {
                outlet.getUserName().setImmediateParent(replicateRetailerOutletParent(parentList));
                outlet.setImmediateParent(new ArrayList<>(1));
            }
        }
    }


    private void populateHierarchy(CkHierarchyMetadata hierarchyMetadata, List<CkHierarchyMetadata> existingMetadata, List<CkHierarchyMetadata> newMetadata, CkOutletDetails tempoutlet) {
        String id = hierarchyMetadata.getId();
        if (hierarchyMetadata.getId() == null) {
            String parentHierarchy = hierarchyMetadata.getHierarchy();
            // Dangerous code, this has to be fixed. Very bad workaround
            if (parentHierarchy != null) {
                Arrays.asList(parentHierarchy.split(",")).stream().forEach(tempHierarchy -> {
                    List<String> hierarchyusers = Arrays.asList(tempHierarchy.split(" > ")).stream().filter(parent -> !parent.equals(getAdminLoginId())).collect(Collectors.toList());
                    String loginId = hierarchyusers.get(hierarchyusers.size() - 1);
                    List<CkHierarchyMetadata> lastParent = (List<CkHierarchyMetadata>) hierarchyMetaDataService.findByImmediateParent(loginId);
                    if (lastParent.isEmpty()) {
                        CkHierarchyMetadata hmd = new CkHierarchyMetadata();
                        hmd.setParent(loginId);
                         hmd.setHierarchy(loginId + " > " + getAdminLoginId());
                        setHierarchyElement(hmd, hierarchyusers, hierarchyMetadata, existingMetadata, newMetadata, tempoutlet);
                    } else {
                        lastParent.stream().forEach(element -> setHierarchyElement(element, hierarchyusers, hierarchyMetadata, existingMetadata, newMetadata, tempoutlet));
                    }
                });
            } else {
                // Handle the case at which Hierarchy is null
                //               TimerUtils.withTime("Time taken to read and populate hierarchyMetadata record ", () -> {
                List<CkHierarchyMetadata> lastParent = (List<CkHierarchyMetadata>) hierarchyMetaDataService.findByImmediateParent(hierarchyMetadata.getParent());
                if (lastParent != null) {
                    existingMetadata.addAll(lastParent);
                }
//                });
                // Handle the case where it is a new Hierarchy
            }
        } else {
            existingMetadata.add(hierarchyMetadata);
        }
    }


    private void setHierarchyElement(CkHierarchyMetadata element, List<String> hierarchyusers, CkHierarchyMetadata hierarchyMetadata, List<CkHierarchyMetadata> existingMetadata, List<CkHierarchyMetadata> newMetadata, CkOutletDetails tempoutlet) {
        String hierarchy = element.getHierarchy();
        if (hierarchy != null) {
            List<String> tempList = new ArrayList<>(hierarchyusers);
            tempList.remove(tempList.size() - 1);
            tempList.add(hierarchy);
            String joinedHierarchy = StringUtils.join(tempList, " > ");

            CkHierarchyMetadata hm = hierarchyMetaDataService.findByHierarchy(joinedHierarchy);
            if (hm != null && existingMetadata.stream().noneMatch(np -> np.getHierarchy().equals(joinedHierarchy))) {
                existingMetadata.add(hm);
            } else if (NullUtils.isNull(hm) && newMetadata.stream().noneMatch(np -> np.getHierarchy().equals(joinedHierarchy))) {
                CkHierarchyMetadata tempHierarchyMetaData = new CkHierarchyMetadata();
                EntityUtils.copyProperties(hierarchyMetadata, tempHierarchyMetaData);
                tempHierarchyMetaData.setHierarchy(joinedHierarchy);
                CkLocation location = stringToLocationConverter.convert(tempoutlet.getLocationHierarchy());
                tempHierarchyMetaData.setLocationHierarchy((location == null) ? null : location.getLocationHierarchy());
                tempHierarchyMetaData.setLob(tempoutlet.getLob());
                newMetadata.add(tempHierarchyMetaData);
            }
        }
    }

    private void setHierarchy(CkOutletDetails tempoutlet) {
        if (tempoutlet.getImmediateParent() != null && !tempoutlet.getImmediateParent().isEmpty()) {
            String hierarchy = String.join(",",
                    tempoutlet.getImmediateParent().stream().map(s -> s.getHierarchy()).collect(Collectors.toList()));
            if(org.apache.commons.lang.StringUtils.isNotEmpty(hierarchy)) {
                tempoutlet.setHierarchy(hierarchy);
            }else{
              //  logger.warn("Hierarchy logs: Null hierarchy found for outlet {}. Skipping setHierarchy() operation",tempoutlet.getOutletCode());
            }
        }
    }

    private List<CkHierarchyMetadata> replicateRetailerOutletParent(List<CkHierarchyMetadata> hms) {
        if (NullUtils.isNull(hms)) {
            return new ArrayList<>(1);
        }
        List<CkHierarchyMetadata> parentList = new ArrayList<>(hms.size());
        hms.forEach(hm -> parentList.add(EntityUtils.deepClone(hm)));
        return parentList;
    }

    //
    public void setOutletSupplier(CkOutletDetails outletDetails) throws JsonProcessingException {
        if (NullUtils.isNotNull(outletDetails.getImmediateParent()) && outletDetails.getImmediateParent().isEmpty() && outletDetails.getImmediateParent().stream().noneMatch(hierarchyMetaData -> NullUtils.isNull(hierarchyMetaData.getHierarchy()))) {
            List<String> supplierList = supplierInfoService.findSuppliers(outletDetails);
            ObjectNode extendedAttributes = (ObjectNode) outletDetails.getExtendedAttributes();
            if (extendedAttributes == null) {
                extendedAttributes = JSONUtils.getObjectMapper().createObjectNode();
            }
            extendedAttributes.putRawValue("supplier", new RawValue(JSONUtils.getObjectMapper().writeValueAsString(supplierList)));
            outletDetails.setExtendedAttributes(extendedAttributes);
        }
    }


    public CkOutletDetails findByOutletCode(String outletCode, boolean cache) {
        if (outletCode == null) {
            return null;
        }
        Function<String, CkOutletDetails> function = (String oc) -> {
            OutletDetailsService service = SpringContext.getBean(OutletDetailsService.class);
            return service.getLoadedOutletObject(oc);
        };

        return function.apply(outletCode);
    }

    public CkOutletDetails getLoadedOutletObject(String outletCode) {
        CkOutletDetails outlet = outletDetailsRepository.findByOutletCode(outletCode);
        if (outlet != null) {
            if (outlet.getImmediateParent() != null) {
                int immediateParentSize = outlet.getImmediateParent().size();
//                if (logger.isDebugEnabled()) {
//                    logger.debug("Immediate parent size {}", immediateParentSize);
//                }
            }
            if (outlet.getUserName() != null) {
//                CkUser u = TimerUtils.withTime("Time taken UserService Association load ",
//                        () -> userService.getLoadedUserObject(outlet.getUserName().getLoginId(), true));
                CkUser u = userService.getLoadedUserObject(outlet.getUserName().getLoginid(), true);
                outlet.setUserName(u);
            }
        }
        return outlet;
    }

    public CkOutletDetails findByOutletCode(String outletCode) {
        return findByOutletCode(outletCode, true);
    }
}
