package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.utils.BatchInsertUtil;
import com.applicate.services.channelkart.utils.JSONUtils;

import com.salescode.dim.PropertyLoader;
import com.salescode.dim.jooq.generated.tables.CkHierarchyMetadata;
import com.salescode.dim.jooq.generated.tables.pojos.OutletDetailsHierarchymetadata;
import com.salescode.dim.jooq.generated.tables.pojos.UserRoles;
import com.salescode.dim.jooq.generated.tables.pojos.Userdesignation;
import com.salescode.dim.jooq.generated.tables.records.CkOutletDetailsRecord;
import com.salescode.dim.jooq.generated.tables.records.CkUserRecord;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.Location;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.User;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.util.RawValue;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.*;

public class OutletDetailsService extends AbstractCDMService<OutletDetails> {

    private static OutletDetailsService instance;
    private static UserService userService;
    public static final String RETAILER = "retailer";
    private static final String WHOLESALER = "wholesaler";
    private static  CustomerAccountsService customerAccountsService;
    private static  LocationService locationService;
    private static  HierarchyMetadataService hierarchyMetadataService;
    private static  SupplierInfoService supplierInfoService;

    private final DSLContext dsl;
    private static final Logger LOG = LoggerFactory.getLogger(OutletDetailsService.class);
    public OutletDetailsService (DSLContext dsl) {
        super(dsl);
        this.dsl = dsl;
        userService = new UserService(dsl);
        customerAccountsService = new CustomerAccountsService(dsl);
        locationService = new LocationService(dsl);
        hierarchyMetadataService = new HierarchyMetadataService(dsl);
        supplierInfoService = new SupplierInfoService(dsl);
    }


    private void populateUserOutletHierarchy(User user, OutletDetails outlet) {
            if (ObjectUtils.isEmpty(outlet.getImmediateParent())) {
                outlet.setImmediateParent(user.getImmediateParent());
            }
        }



    private void populateLocation(OutletDetails outletDetails){
            boolean locExists = findEntity(Location.class,outletDetails.getLocationHierarchy());
            if(locExists == true){
                return;
            }
            Location location = outletDetails.getLocation();
            location = locationService.findLocationOrPersistLocation(location);
            outletDetails.setLocation(location);
            outletDetails.setLocationHierarchy(location.getLocationHierarchy());

    }
    
    private void setOutletHierarchy(OutletDetails outlet){
        List<HierarchyMetadata> immediateParents = outlet.getImmediateParent();
        if (immediateParents != null && !immediateParents.isEmpty()) {
            List<HierarchyMetadata> existingMetadata = new ArrayList<>();
            List<HierarchyMetadata> newMetadata = new ArrayList<>();
            for (HierarchyMetadata HierarchyMetadata : immediateParents) {
                populateHierarchy(HierarchyMetadata, existingMetadata, newMetadata, outlet);
            }
            if (!newMetadata.isEmpty()) {
                List<HierarchyMetadata> savedData = hierarchyMetadataService.batchSave(newMetadata);
                existingMetadata.addAll(savedData);
            }
            if (!existingMetadata.isEmpty())
                outlet.setImmediateParent(existingMetadata);
        }
        setHierarchy(outlet);

        outlet.setNormalizedHierarchy(com.applicate.services.channelkart.services.UserService.getNormalizedHierarchy(outlet.getHierarchy()));
    }

    private void setHierarchy(OutletDetails tempoutlet) {
        if (tempoutlet.getImmediateParent() != null && !tempoutlet.getImmediateParent().isEmpty()) {
            String hierarchy = String.join(",",
                    tempoutlet.getImmediateParent().stream().map(s -> s.getHierarchy()).collect(Collectors.toList()));
            if(StringUtils.isNotEmpty(hierarchy)) {
                tempoutlet.setHierarchy(hierarchy);
            }else{
  //              logger.warn("Hierarchy logs: Null hierarchy found for outlet {}. Skipping setHierarchy() operation",tempoutlet.getOutletCode());
            }
        }
    }

    private void populateHierarchy(HierarchyMetadata HierarchyMetadata, List<HierarchyMetadata> existingMetadata,
                                   List<HierarchyMetadata> newMetadata, OutletDetails tempoutlet) {
        if (HierarchyMetadata.getId() == null) {
            String parentHierarchy = HierarchyMetadata.getHierarchy();
            // Dangerous code, this has to be fixed. Very bad workaround
            if (parentHierarchy != null) {
                Arrays.asList(parentHierarchy.split(",")).stream().forEach(tempHierarchy -> {
                    List<String> hierarchyusers = Arrays.asList(tempHierarchy.split(" > ")).stream().filter(parent->!parent.equals(customerAccountsService.getAdminLoginId())).collect(Collectors.toList());
                    String loginId = hierarchyusers.get(hierarchyusers.size() - 1);
                    List<HierarchyMetadata> lastParent = (List<HierarchyMetadata>) hierarchyMetadataService
                            .findByImmediateParent(loginId);
                    if(lastParent.isEmpty()){
                        HierarchyMetadata hmd=new HierarchyMetadata();
                        hmd.setImmediateParent(loginId);
                        hmd.setHierarchy(loginId + " > " + customerAccountsService.getAdminLoginId());
                        setHierarchyElement(hmd,hierarchyusers,HierarchyMetadata,existingMetadata,newMetadata,tempoutlet);
                    }else {
                        lastParent.stream().forEach(element -> setHierarchyElement(element, hierarchyusers,
                                HierarchyMetadata, existingMetadata, newMetadata, tempoutlet));
                    }
                });
            } else {
                // Handle the case at which Hierarchy is null
               
                    List<HierarchyMetadata> lastParent = (List<HierarchyMetadata>) hierarchyMetadataService
                            .findByImmediateParent(HierarchyMetadata.getParent());
                    if (lastParent != null) {
                        existingMetadata.addAll(lastParent);
                    }
             
                // Handle the case where it is a new Hierarchy
            }
        } else {
            existingMetadata.add(HierarchyMetadata);
        }
    }

    private void setHierarchyElement(HierarchyMetadata element, List<String> hierarchyusers,
                                     HierarchyMetadata HierarchyMetadata, List<HierarchyMetadata> existingMetadata,
                                     List<HierarchyMetadata> newMetadata, OutletDetails tempoutlet) {
        String hierarchy = element.getHierarchy();
        if (hierarchy != null) {
            List<String> tempList = new ArrayList<>(hierarchyusers);
            tempList.remove(tempList.size() - 1);
            tempList.add(hierarchy);
            String joinedHierarchy = StringUtils.join(tempList, " > ");

            HierarchyMetadata hm = hierarchyMetadataService.findByHierarchy(joinedHierarchy);
            if (hm != null && existingMetadata.stream().noneMatch(np->np.getHierarchy().equals(joinedHierarchy))) {
                existingMetadata.add(hm);
            } else if(hm != null && newMetadata.stream().noneMatch(np->np.getHierarchy().equals(joinedHierarchy))) {
                HierarchyMetadata tempHierarchyMetadata = new HierarchyMetadata();
                tempHierarchyMetadata.setHierarchy(joinedHierarchy);
                Location location = tempoutlet.getLocation();
                tempHierarchyMetadata.setLocationHierarchy((location == null) ? null : location.getLocationHierarchy());
                tempHierarchyMetadata.setLob(tempoutlet.getLob());
                newMetadata.add(tempHierarchyMetadata);
            }
        }
    }

    private User populateAssociatedData(OutletDetails outlet){
        User user = userService.getUser(outlet.getUserName());
        outlet.setUserName(user);
        outlet.setLocationHierarchy(user.getLocation().getLocationHierarchy());
        if(user.getDesignation().contains(RETAILER) || user.getDesignation().contains(WHOLESALER)) {
            outlet.setActiveStatus(outlet.getUserName().getActiveStatus());
        }
        populateUserOutletHierarchy(user,outlet);
        populateLocation(outlet);
        setOutletHierarchy(outlet);
        setOutletSupplier(outlet);
        return user;
    }

    public void setOutletSupplier(OutletDetails outletDetails)  {
        if ((outletDetails.getImmediateParent())!=null && !outletDetails.getImmediateParent().isEmpty() && outletDetails.getImmediateParent().stream().noneMatch(hierarchyMetaData -> (hierarchyMetaData.getHierarchy()) != null)) {
            List<String> supplierList = supplierInfoService.findSuppliers(outletDetails);
            ObjectNode extendedAttributes = (ObjectNode) outletDetails.getExtendedAttributes();
            if (extendedAttributes == null) {
                extendedAttributes = JSONUtils.getObjectMapper().createObjectNode();
            }
            try {
                extendedAttributes.putRawValue("supplier", new RawValue(JSONUtils.getObjectMapper().writeValueAsString(supplierList)));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            outletDetails.setExtendedAttributes(extendedAttributes);
        }
    }

    public User beforeSave(OutletDetails outlet){
      return populateAssociatedData(outlet);
    }

    @Override
    public OutletDetails save(OutletDetails outlet){
        User user = beforeSave(outlet);
        super.addHash(outlet);
        LOG.info(outlet.getHash());
        com.salescode.dim.jooq.generated.tables.pojos.OutletDetails savedObj = dsl.select(CK_OUTLET_DETAILS.asterisk().except(CK_OUTLET_DETAILS.COORDINATE))
                .from(CK_OUTLET_DETAILS)
                .where(CK_OUTLET_DETAILS.OUTLETCODE.eq(outlet.getOutletcode()))
                .fetchOneInto(com.salescode.dim.jooq.generated.tables.pojos.OutletDetails.class);
        LOG.info(String.valueOf(savedObj));
        LOG.info("Old record found success");
        if(savedObj != null) {
            outlet.setId(savedObj.getId());
            outlet.setVersion(savedObj.getVersion() + 1);
        }
        else{
            outlet.setId(UUID.randomUUID().toString());
            outlet.setVersion(0);
        }

        if(savedObj!=null && Objects.equals(savedObj.getHash(), outlet.getHash())){
            LOG.info("Found outlet");
            return outlet;
        }

        CkOutletDetailsRecord record = dsl.newRecord(CK_OUTLET_DETAILS,outlet);
        LOG.info("Got Record");
        record.setMapped(true);
        LOG.info("Create Outlet Record");
        try {
            dsl.insertInto(CK_OUTLET_DETAILS)
                    .set(record)
                    .onDuplicateKeyUpdate()
                    .set(record)
                    .execute();
        }
        catch (Exception e){
            LOG.info(String.valueOf(e));
        }
        LOG.info("Save completed successfully");
//        List<UserRoles> roleList = setRoles(List.of(user));
//        saveRoles(roleList);
//        List<Userdesignation> userdesignationsList = setDesignation(List.of(user));
//        saveDesignation(userdesignationsList);
//        List<OutletDetailsHierarchymetadata> outletDetailsHierarchymetadata = setOutletHierarchyMetadata(List.of(outlet));
//        saveOutletDetailHierarchyMetadata(outletDetailsHierarchymetadata);
        return outlet;

    }

    private List<User> populateBatchAssociatedData(List<OutletDetails> outletDetailsList) {
        long startTime = System.currentTimeMillis();

        List<User> userList = outletDetailsList.stream()
                .map(OutletDetails::getUserName)
                .collect(Collectors.toList());

        List<Location> locationList = outletDetailsList.stream()
                .map(OutletDetails::getLocation)
                .collect(Collectors.toList());

        long userStartTime = System.currentTimeMillis();
        List<User> savedUserList = userService.getUser(userList);
        long userEndTime = System.currentTimeMillis();
        LOG.info("UserService save took: " + (userEndTime - userStartTime) + " ms");

        long locationStartTime = System.currentTimeMillis();
        List<Location> savedLocList = locationService.findLocationOrPersistLocation(locationList);
        long locationEndTime = System.currentTimeMillis();
        LOG.info("LocationService save took: " + (locationEndTime - locationStartTime) + " ms");

        for (int i = 0; i < outletDetailsList.size(); i++) {
            outletDetailsList.get(i).setUserName(savedUserList.get(i));
            populateUserOutletHierarchy(savedUserList.get(i), outletDetailsList.get(i));
            outletDetailsList.get(i).setLocation(savedLocList.get(i));
            outletDetailsList.get(i).setLocationHierarchy(savedLocList.get(i).getLocationHierarchy());
            setOutletHierarchy(outletDetailsList.get(i));
            setOutletSupplier(outletDetailsList.get(i));
        }

        long endTime = System.currentTimeMillis();
        LOG.info("Total populateBatchAssociatedData execution time: " + (endTime - startTime) + " ms");

        return savedUserList;
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
                dsl,
                userRoles,
                CK_USER_ROLES,
                role -> DSL.row(role.getUserId(), role.getRolesId()),
                CK_USER_ROLES.USER_ID,
                CK_USER_ROLES.ROLES_ID,
                role -> dsl.insertInto(CK_USER_ROLES)
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
                dsl,
                userDesignation,
                CK_USERDESIGNATION,
                designation -> DSL.row(designation.getLoginId(), designation.getDesignation()),
                CK_USERDESIGNATION.LOGIN_ID,
                CK_USERDESIGNATION.DESIGNATION,
                designation -> dsl.insertInto(CK_USERDESIGNATION)
                        .set(CK_USERDESIGNATION.LOGIN_ID, designation.getLoginId())
                        .set(CK_USERDESIGNATION.DESIGNATION, designation.getDesignation())
        );
    }

    private List<OutletDetailsHierarchymetadata> setOutletHierarchyMetadata(List<OutletDetails> outletDetailsMap) {
        return outletDetailsMap.stream()
                .flatMap(outlet -> outlet.getImmediateParent().stream()
                        .map(hierarchy -> {
                            OutletDetailsHierarchymetadata metadata = new OutletDetailsHierarchymetadata();
                            // Set the outlet code
                            metadata.setOutletId(outlet.getId());
                            // Set the parent ID
                            metadata.setHierarchyMetadataId(hierarchy.getId());
                            return metadata;
                        })
                )
                .collect(Collectors.toList());
    }

    public void saveOutletDetailHierarchyMetadata(List<OutletDetailsHierarchymetadata> outletDetailsHierarchymetadata) {
        BatchInsertUtil.saveBatchWithDuplicateCheck(
                dsl,
                outletDetailsHierarchymetadata,
                CK_OUTLET_DETAILS_HIERARCHYMETADATA,
                item -> DSL.row(item.getOutletId(), item.getHierarchyMetadataId()),
                CK_OUTLET_DETAILS_HIERARCHYMETADATA.OUTLET_ID,
                CK_OUTLET_DETAILS_HIERARCHYMETADATA.HIERARCHY_METADATA_ID,
                item -> dsl.insertInto(CK_OUTLET_DETAILS_HIERARCHYMETADATA)
                        .set(CK_OUTLET_DETAILS_HIERARCHYMETADATA.OUTLET_ID, item.getOutletId())
                        .set(CK_OUTLET_DETAILS_HIERARCHYMETADATA.HIERARCHY_METADATA_ID, item.getHierarchyMetadataId())
        );
    }


    @Override
    public List<OutletDetails> batchSave(List<OutletDetails> outletDetailsList) {
        long startTime = System.currentTimeMillis();
        LOG.info("Batch Save started with {} records", outletDetailsList.size());

        LOG.info("Step 1: Populating associated data - Start");
        long step1Start = System.currentTimeMillis();
        List<User> savedUserList = populateBatchAssociatedData(outletDetailsList);
        long step1End = System.currentTimeMillis();
        LOG.info("Step 1: Completed in {} ms", (step1End - step1Start));

        for (User user : savedUserList) {
            LOG.info("User: {}", user);
        }
        LOG.info("User populate success");

        LOG.info("Step 2: Fetching existing outlet records - Start");
        long step2Start = System.currentTimeMillis();
        List<String> outletCodes = outletDetailsList.stream()
                .map(OutletDetails::getOutletcode)
                .collect(Collectors.toList());

        Map<String, com.salescode.dim.jooq.generated.tables.pojos.OutletDetails> savedList = dsl
                .select(CK_OUTLET_DETAILS.asterisk().except(CK_OUTLET_DETAILS.COORDINATE))
                .from(CK_OUTLET_DETAILS)
                .where(CK_OUTLET_DETAILS.OUTLETCODE.in(outletCodes))
                .fetch()
                .intoMap(CK_OUTLET_DETAILS.OUTLETCODE, record -> record.into(com.salescode.dim.jooq.generated.tables.pojos.OutletDetails.class));
        long step2End = System.currentTimeMillis();
        LOG.info("Step 2: Fetching existing outlet records completed in {} ms", (step2End - step2Start));

        LOG.info("Step 3: Preparing insert/update lists - Start");
        long step3Start = System.currentTimeMillis();
        for (OutletDetails outlet : outletDetailsList) {
            outlet.setChanged((byte) 1);
            outlet.setChanged(true);
        }

        List<OutletDetails> itemsToInsert = new ArrayList<>();
        List<OutletDetails> itemsToUpdate = new ArrayList<>();

        for (OutletDetails outlet : outletDetailsList) {
            super.addHash(outlet);
            if (savedList.get(outlet.getOutletcode()) == null) {
                outlet.setVersion(0);
                outlet.setId(UUID.randomUUID().toString());
                outlet.setMapped(true);
                itemsToInsert.add(outlet);
            } else {
                com.salescode.dim.jooq.generated.tables.pojos.OutletDetails existingOutlet = savedList.get(outlet.getOutletcode());
                outlet.setId(existingOutlet.getId());
                outlet.setVersion(existingOutlet.getVersion() + 1);
                outlet.setMapped(true);

                if (!Objects.equals(outlet.getHash(), existingOutlet.getHash())) {
                    itemsToUpdate.add(outlet);
                }
            }
        }
        long step3End = System.currentTimeMillis();
        LOG.info("Step 3: Preparation completed in {} ms", (step3End - step3Start));

        LOG.info("Step 4: Batch insert execution - Start");

        long step4Start = System.currentTimeMillis();
        if (!itemsToInsert.isEmpty()) {
            dsl.batchInsert(
                    itemsToInsert.stream()
                            .map(outlet -> dsl.newRecord(CK_OUTLET_DETAILS, outlet))
                            .collect(Collectors.toList())
            ).execute();
        }

        long step4End = System.currentTimeMillis();
        LOG.info("Step 4: Batch insert completed in {} ms", (step4End - step4Start));

        LOG.info("Step 5: Batch update execution - Start");
        long step5Start = System.currentTimeMillis();
        if (!itemsToUpdate.isEmpty()) {
            dsl.batchUpdate(
                    itemsToUpdate.stream()
                            .map(outlet -> {
                                CkOutletDetailsRecord record = dsl.newRecord(CK_OUTLET_DETAILS, outlet);
                                record.changed(CK_USER.ID, false); // Avoid updating primary key
                                return record;
                            })
                            .collect(Collectors.toList())
            ).execute();
        }
        long step5End = System.currentTimeMillis();
        LOG.info("Step 5: Batch update completed in {} ms", (step5End - step5Start));

        long endTime = System.currentTimeMillis();
        LOG.info("Batch Execution Successful. Total time taken: {} ms", (endTime - startTime));

        List<UserRoles> roleList = setRoles(savedUserList);
        saveRoles(roleList);
        List<Userdesignation> userdesignationsList = setDesignation(savedUserList);
        saveDesignation(userdesignationsList);
        List<OutletDetailsHierarchymetadata> outletDetailsHierarchymetadata = setOutletHierarchyMetadata(outletDetailsList);
        saveOutletDetailHierarchyMetadata(outletDetailsHierarchymetadata);
        return outletDetailsList;
    }


    public OutletDetails findByOutletcode(String outletcode){
        return dsl.select(CK_OUTLET_DETAILS.asterisk().except(CK_OUTLET_DETAILS.COORDINATE))
                .from(CK_OUTLET_DETAILS)
                .where(CK_OUTLET_DETAILS.OUTLETCODE.eq(outletcode))
                .fetchOneInto(OutletDetails.class);
    }
}
