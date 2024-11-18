package com.salescode.dataintegration.etl.cdm.services;

import com.fasterxml.jackson.core.JsonProcessingException;
//import com.salescode.channelkart.utils.TimerUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
//import com.fasterxml.jackson.databind.util.RawValue;
//import com.salescode.channelkart.services.SpringContext;
//import com.salescode.channelkart.utils.EntityUtils;
//import com.salescode.channelkart.utils.JSONUtils;
//import com.salescode.channelkart.utils.NullUtils;
//import com.salescode.channelkart.utils.TimerUtils;
//import com.salescode.channelkart.utils.TimerUtils;
import com.fasterxml.jackson.databind.util.RawValue;
import com.salescode.channelkart.converters.HierarchyMetaDataToStringConverter;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.channelkart.utils.JSONUtils;
import com.salescode.channelkart.utils.NullUtils;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.jooq.generated.tables.pojos.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jooq.DSLContext;
import org.jooq.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.salescode.jooq.generated.Tables.CK_LOCATION;
import static com.salescode.jooq.generated.tables.CkHierarchyMetadata.CK_HIERARCHY_METADATA;
import static com.salescode.jooq.generated.tables.CkOutletDetails.CK_OUTLET_DETAILS;
import static com.salescode.jooq.generated.tables.CkOutletDetailsHierarchymetadata.CK_OUTLET_DETAILS_HIERARCHYMETADATA;
import static com.salescode.jooq.generated.tables.CkUser.CK_USER;


@Service
public class OutletDetailsService extends AbstractCDMService<CkOutletDetails> {

    private final DSLContext dsl;
    @Autowired
    private SupplierInfoService supplierInfoService;
    //    private final UserDetailsService userDetailsService;
//    @Autowired
    private HierarchyMetaDataService hierarchyMetaDataService;
    //
    private LocationService locationService;
    //     private CkUser retailerInfo;
    //     private UserService userService;

   private final HierarchyMetaDataToStringConverter hierarchyMetaDataToStringConverter;
    @Autowired
    public OutletDetailsService(DSLContext dsl,HierarchyMetaDataToStringConverter hierarchyMetaDataToStringConverter) {
        this.dsl = dsl;
        this.hierarchyMetaDataToStringConverter = hierarchyMetaDataToStringConverter;
    }

    private CustomerAccountsService getCustomerAccountsService() {
        return SpringContext.getBean(CustomerAccountsService.class);
    }
//
//    private void createRetailUser(CkOutletDetails outlet) {
//        CkUser user = new CkUser();
//        user.setActiveStatus(outlet.getActiveStatus());
//        user.setLoginid(outlet.getOutletcode());
//        user.setUseraccountid(outlet.getOutletcode());
////        user.setLocationHierarchy(outlet.getLocationHierarchy(outlet));
//        user.setMobile(outlet.getContactno());
//        user.setName(StringUtils.isEmpty(outlet.getOutletName()) ? outlet.getOutletcode() : outlet.getOutletName());
//        user.setImmediateParent(replicateRetailerOutletParent(outlet.getImmediateParent()));
//        user.setDesignation(Set.of(RETAILER));
//        List<CkAuthRole> roles = roleService.getRoleAsList(RoleName.ROLE_USER.name());
//        user.setRoles(roles);
//        outlet.setUserName(user);
//        outlet.setImmediateParent(new ArrayList<>(1));
//        createAssociateDataWithLock(outlet);
//    }

//    public void createAssociatedData(CkOutletDetails outlet) {
//        if (outlet.getUserName() != null) {
//            addAssociatedData(outlet);
//        } else if (propertyRegistry.getValue(PropertyDefinition.APPLICATION_CATETORY)
//                .equals(ApplicationCategory.RETAIL.name())) {
//            //domain-name -> client
//            //domain-type -> properties
//            createRetailUser(outlet);
//        }
//    }

    //    private void addAssociatedData(CkOutletDetails outlet) {
//        if (propertyRegistry.getValue(PropertyDefinition.APPLICATION_CATETORY).equals(ApplicationCategory.RETAIL.name()) && (outlet.getUserName().getDesignation().contains(RETAILER) || outlet.getUserName().getDesignation().contains(WHOLESALER))){
//            outlet.getChanges().forEach(changed ->
//
//                    setUserAssociateData(outlet,changed)
//            );
//        }
//        GlobalLock.withLock(outlet.getUserName().getLoginId(), s ->
//                TimerUtils.withTime("Time taken to execute updateUser([[" + outlet.getUserName().getLoginId() + "]]) for outlet[[" + outlet.getOutletCode() + "]]", () -> createAssociateDataWithLock(outlet)));
//    }

    public void createAssociatedData(CkOutletDetails outlet) {
        if (outlet.getUserName() != null) {
            addAssociatedData(outlet);
        } else if (getClientProperty("application.category")
                .equals(ApplicationCategory.RETAIL.name())) {
            createRetailUser(outlet);
        }
    }

public CkLocation getLocation(CkOutletDetails outlet) {

       return dsl.select(CK_OUTLET_DETAILS.fields())
               .from(CK_OUTLET_DETAILS)
               .join(CK_LOCATION)
               .on(CK_OUTLET_DETAILS.LOCATION_HIERARCHY.eq(CK_LOCATION.LOCATION_HIERARCHY))
               .where(CK_OUTLET_DETAILS.ID.eq(outlet.getId()))
               .fetchOneInto(CkLocation.class);
    private void addAssociatedData(CkOutletDetails outlet) {
        if (propertyRegistry.getValue(PropertyDefinition.APPLICATION_CATETORY).equals(ApplicationCategory.RETAIL.name()) && (outlet.getUserName().getDesignation().contains(RETAILER) || outlet.getUserName().getDesignation().contains(WHOLESALER))){
            outlet.getChanges().forEach(changed ->

    }

    public CkLocation getLocationHierarchy(CkOutletDetails outlet){
        return getLocation(outlet);
    }
//
    public void refreshLocation(CkOutletDetails outletDetails) {
//        /* location */
        if (NullUtils.isNotNull(outletDetails.getLocationHierarchy())) {
//            CkLocation location = outletDetails.getLocation(dsl);
//            location = locationService.findLocationOrPersistLocation(location);
//            List<Integer> arr = new ArrayList<>();
//            arr.add(2);
//            outletDetails.setLocation(location);
//            outletDetails.setLocationHierarchy(location);
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

//    public void fillRetailer(CkOutletDetails outlet, boolean hierarchy) {
//        if (outlet.getUserName(dsl) != null) {
//            if (StringUtils.isBlank(getUserName(outlet).getId())) {
//                CkUser tempuser = TimerUtils.withTime("Time taken UserService findByLoginId from outlet ",
//                        () -> userService.findByLoginId(getLoginId(getUserName(outlet)), true, hierarchy));
//                outlet.setRetailerInfo(tempuser);
//                retailerInfo = tempuser;
//            } else {
//                retailerInfo = getUserName(outlet);
////                outlet.setRetailerInfo(getUserName(outlet));
//            }
//        }
//    }

    public CkOutletDetails prepareOutletDetails(CkOutletDetails outletDetails) {
//        /* location */
//         refreshLocation(outletDetails);
//
//        /* Retailer Info/Username */
//        TimerUtils.withTime("prepareOutletDetails Time taken to fillRetailer ",
//                () -> fillRetailer(outletDetails, false));
//     fillRetailer(outletDetails, false);
//
        return outletDetails;
    }

//    private void setHierarchy(CkOutletDetails tempoutlet) {
//        if (tempoutlet.getImmediateParent(tempoutlet.getId()) != null && !tempoutlet.getImmediateParent(tempoutlet.getId()).isEmpty()) {
//            String hierarchy = String.join(",",
//                    tempoutlet.getImmediateParent(tempoutlet.getId()).stream().map(s -> s.hierarchy).collect(Collectors.toList()));
//            if (StringUtils.isNotEmpty(hierarchy)) {
//                tempoutlet.setHierarchy(hierarchy);
//            } else {
//                //logger.warn("Hierarchy logs: Null hierarchy found for outlet {}. Skipping setHierarchy() operation",tempoutlet.getOutletCode());
//            }
//        }
//    }

//    public void setOutletSupplier(CkOutletDetails outletDetails) throws JsonProcessingException {
//        if (NullUtils.isNotNull(outletDetails.getImmediateParent(outletDetails.getId())) && !outletDetails.getImmediateParent(outletDetails.getId()).isEmpty() && outletDetails.getImmediateParent(outletDetails.getId()).stream().noneMatch(hierarchyMetaData -> NullUtils.isNull(hierarchyMetaData.getHierarchy()))) {
//            List<String> supplierList = supplierInfoService.findSuppliers(outletDetails);
//            ObjectNode extendedAttributes = (ObjectNode) outletDetails.getExtendedAttributes();
//            if (extendedAttributes == null) {
//                extendedAttributes = JSONUtils.getObjectMapper().createObjectNode();
//            }
//            extendedAttributes.putRawValue("supplier", new RawValue(JSONUtils.getObjectMapper().writeValueAsString(supplierList)));
//            outletDetails.setExtendedAttributes(extendedAttributes);
//        }
//    }

   public void setImmediateParent(CkOutletDetails tempoutlet,List<CkHierarchyMetadata> existingMetadata ) {
//        dsl.update(CK_OUTLET_DETAILS_HIERARCHYMETADATA)
//                .set(CK_OUTLET_DETAILS_HIERARCHYMETADATA.HIERARCHY_METADATA_ID,existingMetadata.get(0).getId())
//                .where(CK_OUTLET_DETAILS_HIERARCHYMETADATA.OUTLET_ID.eq(tempoutlet.getId()))
//                .execute();
        for(CkHierarchyMetadata hierarchyMetadata : existingMetadata){
            dsl.insertInto(CK_OUTLET_DETAILS_HIERARCHYMETADATA)
                    .values(tempoutlet.getId(),hierarchyMetadata.getId());
        }
   }
    @Override
    public CkOutletDetails save(CkOutletDetails cdmObject) {

//        TimerUtils.withTime("Time taken to createAssociatedData record ",
//               () -> createAssociatedData(cdmObject));
//      printLogsForNullHierarchy(cdmObject,"Location null before prepare outlet details");
//        CkOutletDetails tempoutlet = TimerUtils.withTime("Time taken to prepareOutletDetails record ",
//                k -> prepareOutletDetails(cdmObject));

        CkOutletDetails tempoutlet = prepareOutletDetails(cdmObject);
        List<CkHierarchyMetadata> immediateParents = getImmediateParent(tempoutlet);
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
                setImmediateParent(tempoutlet, existingMetadata);
        }
////        printLogsForNullHierarchy(tempoutlet,"Location null before setting hierarchy");
        setHierarchy(tempoutlet);
////
      tempoutlet.setNormalizedHierarchy(UserService.getNormalizedHierarchy(tempoutlet.getHierarchy()));
////
//        TimerUtils.withTime("Time taken to set supplier ",
//                () -> {
                   try {
                       setOutletSupplier(tempoutlet);
                    } catch (JsonProcessingException e) {
////                        logger.error("Error while setting supplier in outlet extended attribute");
                   }
//                });
////      printLogsForNullHierarchy(tempoutlet,"Location null before saving outlet");
        var record = dsl.newRecord(CK_OUTLET_DETAILS, tempoutlet);
        if(record.get(CK_OUTLET_DETAILS.ID) == null) record.set(CK_OUTLET_DETAILS.ID, "1");
        if(record.get(CK_OUTLET_DETAILS.VERSION) == null) record.set(CK_OUTLET_DETAILS.VERSION, 1);
        if(record.get(CK_OUTLET_DETAILS.MAPPED) == null) record.set(CK_OUTLET_DETAILS.MAPPED, true);
        if(record.get(CK_OUTLET_DETAILS.DTYPE) == null) record.set(CK_OUTLET_DETAILS.DTYPE, "1");
        dsl.insertInto(CK_OUTLET_DETAILS)
                .set(record)
                .onDuplicateKeyUpdate()
                .set(record)
                .execute();
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
            return cdmObject;
        }


    //SAVE IN USER entity


    //hierarchy 'outletcode > admin@applicate.in'


//
////    public void createAssociatedData(CkOutletDetails outlet) {
////        if (outlet.getUserName() != null) {
////            addAssociatedData(outlet);
////        } else if (propertyRegistry.getValue(PropertyDefinition.APPLICATION_CATETORY)
////                .equals(ApplicationCategory.RETAIL.name())) {
////            createRetailUser(outlet);
////        }
////    }
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
////    private void createRetailUser(CkOutletDetails outlet) {
////        CkUser user = new CkUser();
////        user.setActiveStatus(outlet.getActiveStatus());
////        user.setLoginId(outlet.getOutletCode());
////        user.setUserAccountId(outlet.getOutletCode());
////        user.setLocationHierarchy(outlet.getLocationHierarchy());
////        user.setMobile(outlet.getContactno());
////        user.setName(StringUtils.isEmpty(outlet.getOutletName()) ? outlet.getOutletCode() : outlet.getOutletName());
////        user.setImmediateParent(replicateRetailerOutletParent(outlet.getImmediateParent()));
////        user.setDesignation(Set.of(RETAILER));
////        List<Role> roles = roleService.getRoleAsList(RoleName.ROLE_USER.name());
////        user.setRoles(roles);
////        outlet.setUserName(user);
////        outlet.setImmediateParent(new ArrayList<>(1));
////        createAssociateDataWithLock(outlet);
////    }
////
////    private void createAssociateDataWithLock(CkOutletDetails outlet) {
////        User user = TimerUtils.withTime("Time taken to execute getUser([[" + outlet.getUserName().getLoginId()
////                + "]]) for outlet[[" + outlet.getOutletcode() + "]]", k -> getUser(outlet.getUserName()));
////        outlet.setUserName(user);
////        if(user.getDesignation().contains(RETAILER) || user.getDesignation().contains(WHOLESALER)) {
////            outlet.setActiveStatus(outlet.getUserName().getActiveStatus());
////        }
////        if (ObjectUtils.isNotEmpty(user.getImmediateParent())) {
////            List<HierarchyMetaData> hms = user.getImmediateParent().stream().map(parent -> {
////                HierarchyMetaData hm = new HierarchyMetaData();
////                hm.setHierarchy(user.getLoginId() + " > "
////                        + (StringUtils.isEmpty(parent.getHierarchy())
////                        ? parent.getImmediateParent() + " > " + getCustomerAccountsService().getAdminLoginId()
////                        : parent.getHierarchy()));
////                Location location = user.getLocationHierarchy();
////                hm.setLocationHierarchy((location == null) ? null : location.getLocationHierarchy());
////                hm.setImmediateParent(user.getLoginId());
////                return hm;
////            }).collect(Collectors.toList());
////            // If outlet defines its own immediate parent then it should not override by
////            // user.
////            // The whole hierarchy should be take care by populate hierarchy in next steps.
////            if (ObjectUtils.isEmpty(outlet.getImmediateParent())) {
////                outlet.setImmediateParent(hms);
////            }
////        }
////    }
////
////    private void addAssociatedData(CkOutletDetails outlet) {
////
////        if (propertyRegistry.getValue(PropertyDefinition.APPLICATION_CATETORY).equals(ApplicationCategory.RETAIL.name()) && (outlet.getUserName().getDesignation().contains(RETAILER) || outlet.getUserName().getDesignation().contains(WHOLESALER))) {
////            outlet.getChanges().forEach(changed ->
////
////                    setUserAssociateData(outlet,changed)
////            );
////        }
////        GlobalLock.withLock(outlet.getUserName().getLoginId(), s ->
////                TimerUtils.withTime("Time taken to execute updateUser([[" + outlet.getUserName().getLoginId() + "]]) for outlet[[" + outlet.getOutletCode() + "]]", () -> createAssociateDataWithLock(outlet)));
////



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
                   List<String> hierarchyusers = Arrays.asList(tempHierarchy.split(" > ")).stream().filter(parent -> !parent.equals(getCustomerAccountsService().getAdminLoginId())).collect(Collectors.toList());
                    String loginId = hierarchyusers.get(hierarchyusers.size() - 1);
                    List<CkHierarchyMetadata> lastParent = (List<CkHierarchyMetadata>) hierarchyMetaDataService.findByImmediateParent(loginId);
                    if (lastParent.isEmpty()) {
                        CkHierarchyMetadata hmd = new CkHierarchyMetadata();
                        hmd.setImmediateParent(loginId);
                        hmd.setHierarchy(loginId + " > " + getCustomerAccountsService().getAdminLoginId());
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
public void saveHierarchy(CkOutletDetails tempoutlet,String hierarchy){
         dsl.update(CK_OUTLET_DETAILS)
                .set(CK_OUTLET_DETAILS.HIERARCHY,hierarchy)
                .where(CK_OUTLET_DETAILS.ID.eq(tempoutlet.getId()))
                .execute();
}
private void setHierarchy(CkOutletDetails tempoutlet) {
    List<CkHierarchyMetadata> list = getImmediateParent(tempoutlet);
    String immediateParent = hierarchyMetaDataToStringConverter.convert(list);
    if (immediateParent != null && !immediateParent.isEmpty()) {
        String hierarchy = String.join(",",
                list.stream().map(s -> s.hierarchy).collect(Collectors.toList()));
        if(StringUtils.isNotEmpty(hierarchy)) {
            tempoutlet.setHierarchy(hierarchy);
            saveHierarchy(tempoutlet,hierarchy);
        }else{
           // logger.warn("Hierarchy logs: Null hierarchy found for outlet {}. Skipping setHierarchy() operation",tempoutlet.getOutletCode());
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
}
//
    public void setOutletSupplier(CkOutletDetails outletDetails) throws JsonProcessingException {
        if (NullUtils.isNotNull(getImmediateParent(outletDetails)) && !getImmediateParent(outletDetails).isEmpty() && getImmediateParent(outletDetails).stream().noneMatch(hierarchyMetaData -> NullUtils.isNull(hierarchyMetaData.getHierarchy()))) {
            List<String> supplierList = supplierInfoService.findSuppliers(outletDetails);
            ObjectNode extendedAttributes = (ObjectNode) outletDetails.getExtendedAttributes();
            if (extendedAttributes == null) {
                extendedAttributes = JSONUtils.getObjectMapper().createObjectNode();
            }
            extendedAttributes.putRawValue("supplier", new RawValue(JSONUtils.getObjectMapper().writeValueAsString(supplierList)));
            outletDetails.setExtendedAttributes(extendedAttributes);
        }
    }
}
