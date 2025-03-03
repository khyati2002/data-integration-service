package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.utils.JSONUtils;

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

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_OUTLET_DETAILS;
import static com.salescode.dim.jooq.generated.Tables.CK_USER;

public class OutletDetailsService extends AbstractCDMService<OutletDetails> {

    private static OutletDetailsService instance;
    private static UserService userService;
    public static final String RETAILER = "retailer";
    private static final String WHOLESALER = "wholesaler";
    private static  CustomerAccountsService customerAccountsService;
    private static  LocationService locationService;
    private static  HierarchyMetadataService HierarchyMetadataService;
    private static  SupplierInfoService supplierInfoService;
    private final DSLContext dsl;

    public OutletDetailsService (DSLContext dsl) {
        super(dsl);
        this.dsl = dsl;
        userService = new UserService(dsl);
        customerAccountsService = new CustomerAccountsService(dsl);
        locationService = new LocationService(dsl);
        HierarchyMetadataService = new HierarchyMetadataService(dsl);
        supplierInfoService = new SupplierInfoService();
    }


    private void populateUserOutletHierarchy(User user, OutletDetails outlet){
        if (ObjectUtils.isNotEmpty(user.getImmediateParent())) {
            List<HierarchyMetadata> hms = user.getImmediateParent().stream().map(parent -> {
                HierarchyMetadata hm = new HierarchyMetadata();
                hm.setHierarchy(user.getLoginid() + " > "
                        + (StringUtils.isEmpty(parent.getHierarchy())
                        ? parent.getParent() + " > " + customerAccountsService.getAdminLoginId()
                        : parent.getHierarchy()));
                Location location = user.getLocation();
                hm.setLocationHierarchy((location == null) ? null : location.getLocationHierarchy());
                hm.setImmediateParent(user.getLoginid());
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

    private void populateLocation(OutletDetails outletDetails){
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
                List<HierarchyMetadata> savedData = HierarchyMetadataService.batchSave(newMetadata);
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
                    List<HierarchyMetadata> lastParent = (List<HierarchyMetadata>) HierarchyMetadataService
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
               
                    List<HierarchyMetadata> lastParent = (List<HierarchyMetadata>) HierarchyMetadataService
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

            HierarchyMetadata hm = HierarchyMetadataService.findByHierarchy(joinedHierarchy);
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

    private void populateAssociatedData(OutletDetails outlet){
        User user = userService.getUser(outlet.getUserName());
        outlet.setUserName(user);
        if(user.getDesignation().contains(RETAILER) || user.getDesignation().contains(WHOLESALER)) {
            outlet.setActiveStatus(outlet.getUserName().getActiveStatus());
        }
        populateUserOutletHierarchy(user,outlet);
        populateLocation(outlet);
        setOutletHierarchy(outlet);
        setOutletSupplier(outlet);
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

    public void beforeSave(OutletDetails outlet){
      populateAssociatedData(outlet);
    }

    @Override
    public OutletDetails save(OutletDetails outlet){
        beforeSave(outlet);
        super.addHash(outlet);
        com.salescode.dim.jooq.generated.tables.pojos.OutletDetails savedObj = dsl.select(CK_OUTLET_DETAILS.asterisk().except(CK_OUTLET_DETAILS.COORDINATE))
                .from(CK_OUTLET_DETAILS)
                .where(CK_OUTLET_DETAILS.OUTLETCODE.eq(outlet.getOutletcode()))
                .fetchOneInto(com.salescode.dim.jooq.generated.tables.pojos.OutletDetails.class);

        if(savedObj != null) {
            outlet.setId(savedObj.getId());
            savedObj.setVersion(savedObj.getVersion() + 1);
        }
        else{
            outlet.setId(UUID.randomUUID().toString());
            outlet.setVersion(0);
        }
        if(savedObj.getHash() == outlet.getHash()){
            return outlet;
        }

        CkOutletDetailsRecord record = dsl.newRecord(CK_OUTLET_DETAILS,outlet);
        record.setMapped(true);
        dsl.insertInto(CK_OUTLET_DETAILS)
                .set(record)
                .onDuplicateKeyUpdate()
                .set(record)
                .execute();

        return outlet;
    }

    private void populateBatchAssociatedData(List<OutletDetails> outletDetailsList){

        List<User> userList= outletDetailsList.stream()
                .map(OutletDetails::getUserName)
                .collect(Collectors.toList());

        List<Location> locationList= outletDetailsList.stream()
                .map(OutletDetails::getLocation)
                .collect(Collectors.toList());

        List<User> savedUserList = userService.getUser(userList);
        List<Location> savedLocList = locationService.findLocationOrPersistLocation(locationList);
        for(int i=0;i<outletDetailsList.size();i++){
            outletDetailsList.get(i).setUserName(savedUserList.get(i));
            populateUserOutletHierarchy(savedUserList.get(i),outletDetailsList.get(i));
            outletDetailsList.get(i).setLocation(savedLocList.get(i));
            outletDetailsList.get(i).setLocationHierarchy(savedLocList.get(i).getLocationHierarchy());
            setOutletHierarchy(outletDetailsList.get(i));
            setOutletSupplier(outletDetailsList.get(i));
        }
    }

    public List<OutletDetails> batchSave(List<OutletDetails> outletDetailsList){
        populateBatchAssociatedData(outletDetailsList);
        List<String> outletCodes = outletDetailsList.stream()
                .map(OutletDetails::getOutletcode)
                .collect(Collectors.toList());

        Map<String, com.salescode.dim.jooq.generated.tables.pojos.OutletDetails> savedList = dsl.select(CK_OUTLET_DETAILS.asterisk().except(CK_OUTLET_DETAILS.COORDINATE))
                .from(CK_OUTLET_DETAILS)
                .where(CK_OUTLET_DETAILS.OUTLETCODE.in(outletCodes))
                .fetch()
                .intoMap(CK_OUTLET_DETAILS.OUTLETCODE, record -> record.into(com.salescode.dim.jooq.generated.tables.pojos.OutletDetails.class));

        List<OutletDetails> itemsToInsert = new ArrayList<>();
        List<OutletDetails> itemsToUpdate = new ArrayList<>();
        for (int i = 0; i < outletDetailsList.size(); i++) {
            super.addHash(outletDetailsList.get(i));
            if (savedList.get(outletDetailsList.get(i).getOutletcode()) == null) {
                outletDetailsList.get(i).setVersion(0);
                outletDetailsList.get(i).setId(UUID.randomUUID().toString());
                outletDetailsList.get(i).setMapped(true);
                itemsToInsert.add(outletDetailsList.get(i));
            } else {
                if (outletDetailsList.get(i).getHash() != savedList.get(outletDetailsList.get(i).getOutletcode()).getHash()) {
                    outletDetailsList.get(i).setId(savedList.get(outletDetailsList.get(i).getOutletcode()).getId());
                    outletDetailsList.get(i).setVersion(savedList.get(outletDetailsList.get(i).getOutletcode()).getVersion());
                    outletDetailsList.get(i).setMapped(true);
                    itemsToUpdate.add(outletDetailsList.get(i));
                }
            }
        }
        if (!itemsToInsert.isEmpty()) {
            dsl.batchInsert(
                    itemsToInsert.stream()
                            .map(outlet -> dsl.newRecord(CK_OUTLET_DETAILS, outlet)) // Convert to jOOQ Records
                            .collect(Collectors.toList())
            ).execute();
        }

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
        return outletDetailsList;
    }
}
