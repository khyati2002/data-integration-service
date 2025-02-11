package com.salescode.channelkart.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.salescode.channelkart.cache.DistributedCache;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.utils.TimerUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.RawValue;
import com.salescode.channelkart.client.properties.PropertyDefinition;
import com.salescode.channelkart.client.properties.PropertyRegistry;
import com.salescode.channelkart.converters.HierarchyMetaDataToStringConverter;
import com.salescode.channelkart.models.enums.RoleName;
import com.salescode.channelkart.utils.*;
import com.salescode.dataintegration.etl.OperationResponse;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.channelkart.models.enums.ApplicationCategory;
import com.salescode.dataintegration.etl.metadata.registry.MetadataRegistry;
import com.salescode.jooq.generated.tables.pojos.CkLocation;
import com.salescode.jooq.generated.tables.pojos.*;


import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.jooq.generated.Tables.CK_CUSTOMER_ACCOUNT;
import static com.salescode.jooq.generated.tables.CkLocation.CK_LOCATION;
import static com.salescode.jooq.generated.tables.CkUserParent.CK_USER_PARENT;
import static com.salescode.jooq.generated.tables.CkUserdesignation.CK_USERDESIGNATION;
import static com.salescode.jooq.generated.tables.CkHierarchyMetadata.CK_HIERARCHY_METADATA;
import static com.salescode.jooq.generated.tables.CkOutletDetails.CK_OUTLET_DETAILS;
import static com.salescode.jooq.generated.tables.CkOutletDetailsHierarchymetadata.CK_OUTLET_DETAILS_HIERARCHYMETADATA;
import static com.salescode.jooq.generated.tables.CkUser.CK_USER;
import static com.salescode.jooq.generated.tables.CkUserRoles.CK_USER_ROLES;
import static org.jooq.impl.DSL.table;


@Service
public class OutletDetailsService extends AbstractCDMService<CkOutletDetails> {

    public static final String RETAILER = "retailer";
    private static final String WHOLESALER = "wholesaler";

    public static final String OUTLETS_CACHE_DOMAIN = "outlets";

    private final DSLContext dsl;
    @Autowired
    private SupplierInfoService supplierInfoService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private MetadataRegistry metadataRegistry;

    private final UserService userService;
    @Autowired
    private HierarchyMetaDataService hierarchyMetaDataService;

    @Autowired
    private DistributedCache distributedCache;


    @Autowired
    private PropertyRegistry propertyRegistry;

    private final LocationService locationService;


    @Autowired
    private PreProcessPipelineService preProcessPipelineService;

   private final HierarchyMetaDataToStringConverter hierarchyMetaDataToStringConverter;
    @Autowired
    public OutletDetailsService(DSLContext dsl, HierarchyMetaDataToStringConverter hierarchyMetaDataToStringConverter,
                                UserService userService, LocationService locationService) {

        this.dsl = dsl;
        this.hierarchyMetaDataToStringConverter = hierarchyMetaDataToStringConverter;
        this.userService = userService;
        this.locationService = locationService;
    }

    private CustomerAccountsService getCustomerAccountsService() {
        return SpringContext.getBean(CustomerAccountsService.class);
    }


    public void createAssociatedData(CkOutletDetails outlet) {
        if (outlet.getUserName() != null) {
            addAssociatedData(outlet);
        } else if (propertyRegistry.getValue(PropertyDefinition.APPLICATION_CATETORY)
                .equals(ApplicationCategory.RETAIL.name())) {
            createRetailUser(outlet);
        }
    }

    public String getClientProperty(String property){
        final String[] result = {null};
        CkMetadata metaConfig = metadataRegistry.getMetadataByDomainNameAndType("client", "properties").orElse(null);
        if (metaConfig != null) {
            metaConfig.getDomainValues().forEach(pair -> {
                String str = pair.get("name").toString();
                String name = str.substring(1,str.length()-1);
                String val = pair.get("value").asText();
                if(property.equalsIgnoreCase(name)){
                    result[0] = pair.get("value").asText();
                }
            });
        }
        return result[0];
    }

public CkLocation getLocation(CkOutletDetails outlet) {

    return dsl.select(CK_OUTLET_DETAILS.fields())
            .from(CK_OUTLET_DETAILS)
            .join(CK_LOCATION)
            .on(CK_OUTLET_DETAILS.LOCATION_HIERARCHY.eq(CK_LOCATION.LOCATION_HIERARCHY))
            .where(CK_OUTLET_DETAILS.ID.eq(outlet.getId()))
            .fetchOneInto(CkLocation.class);
}



    public CkLocation getLocationHierarchy(CkOutletDetails outlet){
        return getLocation(outlet);
    }
//
    public void refreshLocation(CkOutletDetails outletDetails) {
//        /* location */
        if (NullUtils.isNotNull(outletDetails.getLocation())) {
            CkLocation location = locationService.findLocationOrPersistLocation(outletDetails.getLocation());
            outletDetails.setLocation(location);
            outletDetails.setLocationHierarchy(location.getLocationHierarchy());
        }
    }

    public CkUser getUserName(CkOutletDetails outlet) {
        return dsl.select(CK_OUTLET_DETAILS.fields())
                .from(CK_OUTLET_DETAILS)
                .join(CK_USER)
                .on(CK_OUTLET_DETAILS.LOGINID.eq(CK_USER.LOGINID))
                .where(CK_OUTLET_DETAILS.ID.eq(outlet.getId()))
                .fetchOneInto(CkUser.class);
    }

    public String getLoginId(CkUser user) {
        return dsl.select(CK_USER.ID)
                .from(CK_USER)
                .where(CK_USER.ID.eq(user.getId()))
                .fetchOneInto(String.class);

    }

    public List<CkHierarchyMetadata> getImmediateParent(CkOutletDetails outlet) {
        return dsl.select(CK_HIERARCHY_METADATA.fields())  // Select fields from the HierarchyMetaData table
                .from(CK_OUTLET_DETAILS)
                .join(CK_OUTLET_DETAILS_HIERARCHYMETADATA)
                .on(CK_OUTLET_DETAILS.ID.eq(CK_OUTLET_DETAILS_HIERARCHYMETADATA.OUTLET_ID))  // Join on outlet_id
                .join(CK_HIERARCHY_METADATA)
                .on(CK_OUTLET_DETAILS_HIERARCHYMETADATA.HIERARCHY_METADATA_ID.eq(CK_HIERARCHY_METADATA.ID))  // Join on hierarchy_metadata_id
                .where(CK_OUTLET_DETAILS.ID.eq(outlet.getId()))  // Filter by outlet_id
                .fetchInto(CkHierarchyMetadata.class);
    }


    public CkOutletDetails prepareOutletDetails(CkOutletDetails outletDetails) {
         refreshLocation(outletDetails);
        return outletDetails;
    }



   public void saveOutletHierarchyMetadata(CkOutletDetails cdmOutletDetails) {
           List<CkHierarchyMetadata> hierarchy = cdmOutletDetails.getImmediateParent();
           var record = dsl.newRecord(CK_OUTLET_DETAILS_HIERARCHYMETADATA);
           record.set(CK_OUTLET_DETAILS_HIERARCHYMETADATA.OUTLET_ID,cdmOutletDetails.getOutletcode());
           Collection<CkHierarchyMetadata> list = hierarchyMetaDataService.findByImmediateParent(hierarchy.get(0).getParent());
           record.set(CK_OUTLET_DETAILS_HIERARCHYMETADATA.HIERARCHY_METADATA_ID,list.stream().findFirst().get().getId());
           dsl.insertInto(CK_OUTLET_DETAILS_HIERARCHYMETADATA)
                   .set(record)
                   .onDuplicateKeyUpdate()
                   .set(record)
                   .execute();
        }

    public void clearCache(String lob, String outletCode) {
        if (StringUtils.isNotBlank(outletCode)) {
            distributedCache.clearCache(lob, OUTLETS_CACHE_DOMAIN, outletCode);
            // distributedCache.clearCache(lob, MicroOutletDetailsService.CACHE_DOMAIN, outletCode);
            userService.clearCache(lob, outletCode);
        }
    }

    public void clearCache(String lob, CkOutletDetails outlet) {
        if (outlet != null) {
            clearCache(lob, outlet.getOutletcode());
        }
    }

    @Override
    public CkOutletDetails save(CkOutletDetails cdmObject){

        return saveInternal(cdmObject);
    }
    public CkOutletDetails saveInternal(CkOutletDetails outletDetails) {
        clearCache(SecurityContextUtils.getLob(), outletDetails);
        refreshLocation(outletDetails);
        TimerUtils.withTime("Time taken to createAssociatedData record ",
                () -> createAssociatedData(outletDetails));
        printLogsForNullHierarchy(outletDetails,"Location null before prepare outlet details");
      CkOutletDetails tempoutlet = TimerUtils.withTime("Time taken to prepareOutletDetails record ",
					k -> prepareOutletDetails(outletDetails));
        List<CkHierarchyMetadata> immediateParents = tempoutlet.getImmediateParent();
        if (immediateParents != null && !immediateParents.isEmpty()) {
            List<CkHierarchyMetadata> existingMetadata = new ArrayList<>();
            List<CkHierarchyMetadata> newMetadata = new ArrayList<>();
            for (CkHierarchyMetadata hierarchyMetadata : immediateParents) {
                populateHierarchy(hierarchyMetadata, existingMetadata, newMetadata, tempoutlet);
            }
            if (!newMetadata.isEmpty()) {
                List<CkHierarchyMetadata> savedData = hierarchyMetaDataService.batchSave(newMetadata);
                existingMetadata.addAll(savedData);
            }
            if (!existingMetadata.isEmpty())
                tempoutlet.setImmediateParent(existingMetadata);
        }

        printLogsForNullHierarchy(tempoutlet,"Location null before setting hierarchy");
        setHierarchy(tempoutlet);

        tempoutlet.setNormalizedHierarchy(UserService.getNormalizedHierarchy(tempoutlet.getHierarchy()));
        TimerUtils.withTime("Time taken to set supplier ",
                () -> {
                    try {
                        setOutletSupplier(tempoutlet);
                    } catch (JsonProcessingException e) {
                       // logger.error("Error while setting supplier in outlet extended attribute");
                    }
                });
     printLogsForNullHierarchy(tempoutlet,"Location null before saving outlet");
     if(tempoutlet.getId() == null) tempoutlet.setId(tempoutlet.getOutletcode());
     if(tempoutlet.getVersion() == null) tempoutlet.setVersion(1);
     if(tempoutlet.getMapped() == null) tempoutlet.setMapped(true);

        CkOutletDetails saved = super.save(tempoutlet);
        String logMessage = String.format(
                "OutletDetails is updated for id '%s', outletcode '%s', last updated on '%s', modified by '%s'",
                saved.getId(),
                saved.getOutletcode(),
                saved.getLastModifiedTime(),
                saved.getModifiedBy()
        );
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
            return saved;
        }


    //SAVE IN USER entity


    //hierarchy 'outletcode > admin@applicate.in'
////
////    void printLogsForNullHierarchy(CkOutletDetails outletDetails, String message){
////        message += " for outletCode {}";
////        if(propertyRegistry.getAsBoolean(PropertyDefinition.LOGS_FOR_NULL_LOCATION) &&
////                (outletDetails.getLocationHierarchy()==null || outletDetails.getLocationHierarchy().getLocationHierarchy()==null)){
////            logger.error(message, outletDetails.getOutletcode());
////        }
////    }
////
////    public CkOutletDetails prepareOutletDetails(CkOutletDetails outletDetails) {
////        /* location */
////        refreshLocation(outletDetails);
////
////        /* Retailer Info/Username */
////        TimerUtils.withTime("prepareOutletDetails Time taken to fillRetailer ",
////                () -> fillRetailer(outletDetails, false));
////
////        return outletDetails;
////    }
////


    private void setDesignation(Set<String> designation,CkUser user){
     String Designation = (designation!=null)?designation.stream().map(String::toLowerCase)
                .collect(Collectors.joining(",")):null;
       boolean exists = dsl.fetchExists(
                dsl.selectFrom(CK_USERDESIGNATION)
                        .where(CK_USERDESIGNATION.LOGIN_ID.eq(user.getLoginid()))
        );
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



//    private void saveUser(CkUser user){
//        var record = dsl.newRecord(CK_USER, user);
//        dsl.insertInto(CK_USER)
//                .set(record)
//                .execute();
//    }

//    private void setImmediateParent(List<CkHierarchyMetadata> hierarchyMetadata,CkUser user){
//         String hierarchy = hierarchyMetaDataToStringConverter.convert(hierarchyMetadata);
//         if(hierarchy == "") return;
//         int val = dsl.update(CK_USER_PARENT)
//                .set(CK_USER_PARENT.PARENT,hierarchy)
//                .where(CK_USER_PARENT.USERLOGINID.eq(user.getId()))
//                .execute();
//         if(val == 0){
//             CkUserParent parent = new CkUserParent();
//             parent.setId(user.getId());
//             parent.setUserloginid(user.getId());
//             parent.setParent(hierarchy);
////             var parent_record = dsl.newRecord(CK_USER_PARENT,parent);
////             dsl.insertInto(CK_USER_PARENT)
////                     .set(parent_record)
////                     .onDuplicateKeyIgnore()
////                     .execute();
//         }
//    }

    private Set<String> getDesignation(CkUser user){
//
//       return dsl.selectFrom(CK_USERDESIGNATION).
//               where(CK_USERDESIGNATION.LOGIN_ID.eq(user.getId()))
//               .fetchSet(CK_USERDESIGNATION.DESIGNATION);
        Set<String> hs = Set.of();
        return hs;
    }

    private List<CkHierarchyMetadata> getImmediateParent(CkUser user){
        var result = dsl.select(CK_USER_PARENT.PARENT)
                .from(CK_USER_PARENT)
                .where(CK_USER_PARENT.USERLOGINID.eq(user.getId()))
                .fetch();

        if (result.isEmpty()) {
            return null; // Or null, depending on your design
        }

        return result.into(CkHierarchyMetadata.class);
    }

   private void setRoles(List<CkAuthRole> roles,CkUser user){
     for(CkAuthRole role : roles){
         boolean exists = dsl.fetchExists(
                 dsl.selectFrom(CK_USER_ROLES)
                         .where(CK_USER_ROLES.USER_ID.eq(user.getLoginid()))
         );
         if(exists == false) {
             dsl.insertInto(CK_USER_ROLES)
                     .values(user.getId(), role.getId())
                     .execute();
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
        setDesignation(user.getDesignation(),user);
        List<CkAuthRole> roles = roleService.getRoleAsList(RoleName.ROLE_USER.name());
        user.setRoles(roles);
        outlet.setUserName(user);
        outlet.setImmediateParent(new ArrayList<>(1));
        createAssociateDataWithLock(outlet);
        setRoles(user.getRoles(),user);

    }
////
    private CkLocation getLocationHierarchy(CkUser user){
        return dsl.select(CK_USER.fields())
                .from(CK_USER)
                .join(CK_LOCATION)
                .on(CK_USER.LOCATION_HIERARCHY.eq(CK_LOCATION.LOCATION_HIERARCHY))
                .where(CK_USER.ID.eq(user.getId()))
                .fetchOneInto(CkLocation.class);
    }

    public void saveUserHierarchyMetadata(CkHierarchyMetadata hierarchy){

        boolean exists = dsl.fetchExists(
                dsl.selectFrom(CK_HIERARCHY_METADATA)
                        .where(CK_HIERARCHY_METADATA.HIERARCHY.eq(hierarchy.getHierarchy()))
        );
        if(exists == false) {
            hierarchy.setId(UUID.randomUUID().toString());
            var record = dsl.newRecord(CK_HIERARCHY_METADATA,hierarchy);
            dsl.insertInto(CK_HIERARCHY_METADATA)
                    .set(record)
                    .onDuplicateKeyUpdate()
                    .set(record)
                    .execute();
        }
    }
    private void createAssociateDataWithLock(CkOutletDetails outlet) {
//        CkUser user = TimerUtils.withTime("Time taken to execute getUser([[" + outlet.getUserName().getLoginid()
//                + "]]) for outlet[[" + outlet.getOutletcode() + "]]", k -> getUser(outlet.getUserName()));
        CkUser user = getUser(outlet.getUserName());
        outlet.setUserName(user);
//        if(user.getDesignation().contains(RETAILER) || user.getDesignation().contains(WHOLESALER)) {
//            outlet.setActiveStatus(outlet.getUserName().getActiveStatus());
//        }
        outlet.setActiveStatus(outlet.getUserName().getActiveStatus());
        if (ObjectUtils.isNotEmpty(user.getImmediateParent())) {
            List<CkHierarchyMetadata> hms = user.getImmediateParent().stream().map(parent -> {
                CkHierarchyMetadata hm = new CkHierarchyMetadata();
                hm.setHierarchy(user.getLoginid() + " > "
                        + (StringUtils.isEmpty(parent.getHierarchy())
                        ? parent.getParent() + " > " + getAdminLoginId()
                        : parent.getHierarchy()));
                CkLocation location = getLocationHierarchy(user);
                hm.setId(UUID.randomUUID().toString());
                hm.setLocationHierarchy((location == null) ? null : location.getLocationHierarchy());
                hm.setParent(user.getLoginid());
                hm.setVersion(1);
                return hm;
            }).collect(Collectors.toList());

            List<CkHierarchyMetadata> res = hierarchyMetaDataService.batchSave(hms);

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
        outUser = validateAndGetUser(user);
        CkUser u = userService.findByLoginId(user.getLoginid(), false);
        if (u != null) {
            out = u;
        } else {
            out = outUser;
        }
            return out;
        }

  private CkUser validateAndGetUser(CkUser user) {
        OperationResponse operationResponse = TimerUtils.withTime(
                "Time Taken to exceute preprocess pipeline for class:[[" + user.getClass() + "]]",
                k -> preProcessPipelineService.process(user));

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
                return userService.save(user);
            }
//            return TimerUtils.withTime("Time taken to execute UserService.save[[" + user.getLoginid() + "]]",
//                    k -> userService.save(user));
      throw new RuntimeException("Preprocess pipeline failed");
//        } else {
//            throw new PreprocessFailedException(pipelineService.getError(operationResponse), operationResponse);
//        }
   }

   private String getAdminLoginId(){
        return dsl.select(CK_CUSTOMER_ACCOUNT.USERNAME)
                .from(CK_CUSTOMER_ACCOUNT)
                .fetchOneInto(String.class);
   }



    private Set<String> populateUserParentHierarchy(CkUser user){
        Set<String> hierarchyStr=new HashSet<>();
        List<CkHierarchyMetadata> hierarchy = user.getImmediateParent();
        if (ObjectUtils.isNotEmpty(hierarchy)) {
            Set<String> uniqueParents = user.getImmediateParent().stream().map(CkHierarchyMetadata::getParent).collect(Collectors.toSet());
            uniqueParents.forEach(parent -> {
                List<CkHierarchyMetadata> hierarchyMetaDataList = (List) hierarchyMetaDataService.findByImmediateParent(parent);
                if (hierarchyMetaDataList.isEmpty()) {
                    hierarchyStr.add(user.getLoginid() + " > " + parent + " > "+ getAdminLoginId());
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

    if (propertyRegistry.getValue(PropertyDefinition.APPLICATION_CATETORY).equals(ApplicationCategory.RETAIL.name()) && (outlet.getUserName().getDesignation().contains(RETAILER) || outlet.getUserName().getDesignation().contains(WHOLESALER))) {
                setUserAssociateData(outlet);
    }
    GlobalLock.withLock(outlet.getUserName().getLoginid(), s ->
            TimerUtils.withTime("Time taken to execute updateUser([[" + outlet.getUserName().getLoginid() + "]]) for outlet[[" + outlet.getOutletcode() + "]]", () -> createAssociateDataWithLock(outlet)));

}
    private void setUserAssociateData(CkOutletDetails outlet) {
        outlet.getUserName().setActiveStatus(outlet.getActiveStatus());
        outlet.getUserName().setName(outlet.getOutletName());
        outlet.getUserName().setMobile(outlet.getContactno());
        outlet.getUserName().setLocationHierarchy(outlet.getLocationHierarchy());
        List<CkHierarchyMetadata> parentList = outlet.getImmediateParent().stream()
                    .filter(h -> !h.getParent().equalsIgnoreCase(outlet.getUserName().getLoginid()))
                    .collect(Collectors.toList());
        if (!parentList.isEmpty()) {
                outlet.getUserName().setImmediateParent(replicateRetailerOutletParent(parentList));
                outlet.setImmediateParent(new ArrayList<>(1));
            }

    }





//    private String getImmediateParentHierarchy(CkHierarchyMetadata ckHierarchyMetadata){
//       return ckHierarchyMetadata.getParent();
//    }

    private void populateHierarchy(CkHierarchyMetadata hierarchyMetadata, List<CkHierarchyMetadata> existingMetadata,
                                   List<CkHierarchyMetadata> newMetadata, CkOutletDetails tempoutlet) {
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
                       // hmd.setHierarchy(loginId + " > " + getCustomerAccountsService().getAdminLoginId());
                        setHierarchyElement(hmd, hierarchyusers, hierarchyMetadata, existingMetadata, newMetadata, tempoutlet);
                    } else {
                        lastParent.stream().forEach(element -> setHierarchyElement(element, hierarchyusers,
                                hierarchyMetadata, existingMetadata, newMetadata, tempoutlet));
                    }
                });
            } else {
                // Handle the case at which Hierarchy is null
 //               TimerUtils.withTime("Time taken to read and populate hierarchyMetadata record ", () -> {
                   List<CkHierarchyMetadata> lastParent = (List<CkHierarchyMetadata>) hierarchyMetaDataService
                           .findByImmediateParent(hierarchyMetadata.getParent());
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
//
//    //
    private void setHierarchyElement(CkHierarchyMetadata element, List<String> hierarchyusers,
                                     CkHierarchyMetadata hierarchyMetadata, List<CkHierarchyMetadata> existingMetadata,
                                     List<CkHierarchyMetadata> newMetadata, CkOutletDetails tempoutlet) {
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
                CkLocation location = getLocationHierarchy(tempoutlet);
                tempHierarchyMetaData.setLocationHierarchy((location == null) ? null : location.getLocationHierarchy());
                tempHierarchyMetaData.setLob(tempoutlet.getLob());
                newMetadata.add(tempHierarchyMetaData);
            }
        }
    }

private void setHierarchy(CkOutletDetails tempoutlet) {
    List<CkHierarchyMetadata> list = tempoutlet.getImmediateParent();
    String immediateParent = hierarchyMetaDataToStringConverter.convert(list);
    if (immediateParent != null && !immediateParent.isEmpty()) {
        String hierarchy = String.join(",",
                list.stream().map(s -> s.getHierarchy()).collect(Collectors.toList()));
        if (StringUtils.isNotEmpty(hierarchy)) {
            tempoutlet.setHierarchy(hierarchy);
            tempoutlet.setHierarchy(hierarchy);
        } else {
            // logger.warn("Hierarchy logs: Null hierarchy found for outlet {}. Skipping setHierarchy() operation",tempoutlet.getOutletCode());
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

    void printLogsForNullHierarchy(CkOutletDetails outletDetails, String message){
        message += " for outletCode {}";
        if(propertyRegistry.getAsBoolean(PropertyDefinition.LOGS_FOR_NULL_LOCATION) &&
                (outletDetails.getLocationHierarchy()==null )){
            //logger.error(message, outletDetails.getOutletcode());
        }
    }
}
