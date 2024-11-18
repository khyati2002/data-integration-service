package com.salescode.dataintegration.etl.cdm.services;

import com.fasterxml.jackson.core.JsonProcessingException;
//import com.salescode.channelkart.utils.TimerUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.RawValue;
import com.salescode.channelkart.services.SpringContext;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.channelkart.utils.JSONUtils;
import com.salescode.channelkart.utils.NullUtils;
import com.salescode.channelkart.utils.TimerUtils;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.dataintegration.etl.cdm.enums.ApplicationCategory;
import com.salescode.dataintegration.etl.metadata.registry.MetadataRegistry;
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

import static com.salescode.jooq.generated.tables.CkHierarchyMetadata.CK_HIERARCHY_METADATA;
import static com.salescode.jooq.generated.tables.CkOutletDetails.CK_OUTLET_DETAILS;
import static com.salescode.jooq.generated.tables.CkOutletDetailsHierarchymetadata.CK_OUTLET_DETAILS_HIERARCHYMETADATA;


@Service
public class OutletDetailsService extends AbstractCDMService<CkOutletDetails> {

    private final DSLContext dsl;
    private final UserDetailsService userDetailsService;
    private final MetadataRegistry metadataRegistry;


    @Autowired
    public OutletDetailsService(DSLContext dsl, UserDetailsService userDetailsService,MetadataRegistry metadataRegistry) {
        this.dsl = dsl;
        this.userDetailsService = userDetailsService;
        this.metadataRegistry = metadataRegistry;
    }

    private CustomerAccountsService getCustomerAccountsService() {
        return SpringContext.getBean(CustomerAccountsService.class);
    }

    @Override
    public CkOutletDetails save(CkOutletDetails cdmObject) {
        TimerUtils.withTime("Time taken to createAssociatedData record ",
                () -> createAssociatedData(cdmObject));
        CkOutletDetails tempoutlet = TimerUtils.withTime("Time taken to prepareOutletDetails record ",
                k -> prepareOutletDetails(cdmObject));
        List<CkHierarchyMetadata> immediateParents = tempoutlet.getImmediateParent(cdmObject.getId());
        if (immediateParents != null && !immediateParents.isEmpty()) {
            List<CkHierarchyMetadata> existingMetadata = new ArrayList<>();
            List<CkHierarchyMetadata> newMetadata = new ArrayList<>();
            for (CkHierarchyMetadata hierarchyMetadata : immediateParents) {
                populateHierarchy(hierarchyMetadata, existingMetadata, newMetadata, tempoutlet);
            }
            if (!newMetadata.isEmpty()) {
                List<CkHierarchyMetadata> savedData = HierarchyMetaDataService.batchSave(newMetadata);
                existingMetadata.addAll(savedData);
            }
            if (!existingMetadata.isEmpty())
                tempoutlet.setImmediateParent(existingMetadata, cdmObject);
        }
        setHierarchy(tempoutlet);

        tempoutlet.setNormalizedHierarchy(UserService.getNormalizedHierarchy(tempoutlet.getHierarchy()));

        TimerUtils.withTime("Time taken to set supplier ",
                () -> {
                    try {
                        setOutletSupplier(tempoutlet);
                    } catch (JsonProcessingException e) {
                    }
                });
//        var record = dsl.newRecord(CK_OUTLET_DETAILS, tempoutlet);
//        dsl.insertInto(CK_OUTLET_DETAILS)
//                .set(record)
//                .onDuplicateKeyUpdate()
//                .set(record)
//                .execute();
        CkOutletDetails saved = super.save(tempoutlet);
        return saved;
    }

    public void createAssociatedData(CkOutletDetails outlet) {
        if (outlet.getUserName() != null) {
            addAssociatedData(outlet);
        } else if (getClientProperty("application.category")
                .equals(ApplicationCategory.RETAIL.name())) {
            createRetailUser(outlet);
        }
    }

    public String getClientProperty(String property){
        final String[] result = {null};
        CkMetadata metaConfig = metadataRegistry.getMetadataByDomainNameAndType("client", "properties").orElse(null);
        if (metaConfig != null) {
            metaConfig.getDomainValues().forEach(pair -> {
                if(property.equalsIgnoreCase(pair.get("name").toString())) result[0] = pair.get("value").asText();
            });
        }
        return result[0];
    }

    private void createRetailUser(CkOutletDetails outlet) {
        CkUser user = new CkUser();
        user.setActiveStatus(outlet.getActiveStatus());
        user.setLoginid(outlet.getOutletcode());
        user.setUseraccountid(outlet.getOutletcode());
//        user.setLocationHierarchy(outlet.getLocationHierarchy(outlet));
        user.setMobile(outlet.getContactno());
        user.setName(StringUtils.isEmpty(outlet.getOutletName()) ? outlet.getOutletcode() : outlet.getOutletName());
        user.setImmediateParent(replicateRetailerOutletParent(outlet.getImmediateParent(outlet.getOutletcode())));
        user.setDesignation(Set.of("retailer"));
        List<CkAuthRole> roles = roleService.getRoleAsList(RoleName.ROLE_USER.name());
        user.setRoles(roles);
        outlet.setUserName(user);
        outlet.setImmediateParent(new ArrayList<>(1));
        createAssociateDataWithLock(outlet);
    }

    private void addAssociatedData(CkOutletDetails outlet) {
        if (propertyRegistry.getValue(PropertyDefinition.APPLICATION_CATETORY).equals(ApplicationCategory.RETAIL.name()) && (outlet.getUserName().getDesignation().contains(RETAILER) || outlet.getUserName().getDesignation().contains(WHOLESALER))){
            outlet.getChanges().forEach(changed ->

                    setUserAssociateData(outlet,changed)
            );
        }
        GlobalLock.withLock(outlet.getUserName().getLoginId(), s ->
                TimerUtils.withTime("Time taken to execute updateUser([[" + outlet.getUserName().getLoginId() + "]]) for outlet[[" + outlet.getOutletCode() + "]]", () -> createAssociateDataWithLock(outlet)));
    }

    public void refreshLocation(CkOutletDetails outletDetails) {
        /* location */
        if (NullUtils.isNotNull(outletDetails.getLocationHierarchy(outletDetails))) {
            CkLocation location = outletDetails.getLocation(outletDetails);
            location = locationService.findLocationOrPersistLocation(location);
            outletDetails.setLocation(location);
            outletDetails.setLocationHierarchy(location);
        }
    }

    public void fillRetailer(CkOutletDetails outlet, boolean hierarchy) {
        if (outlet.getUserName() != null) {
            if (StringUtils.isBlank(outlet.getUserName().getId())) {
                CkUser tempuser = TimerUtils.withTime("Time taken UserService findByLoginId from outlet ",
                        () -> userService.findByLoginId(outlet.getUserName().getLoginId(), true, hierarchy));
                outlet.setRetailerInfo(tempuser);
            } else {
                outlet.setRetailerInfo(outlet.getUserName());
            }
        }
    }

    public CkOutletDetails prepareOutletDetails(CkOutletDetails outletDetails) {
        /* location */
        refreshLocation(outletDetails);

        /* Retailer Info/Username */
        TimerUtils.withTime("prepareOutletDetails Time taken to fillRetailer ",
                () -> fillRetailer(outletDetails, false));

        return outletDetails;
    }

    private void setHierarchy(CkOutletDetails tempoutlet) {
        if (tempoutlet.getImmediateParent(tempoutlet.getId()) != null && !tempoutlet.getImmediateParent(tempoutlet.getId()).isEmpty()) {
            String hierarchy = String.join(",",
                    tempoutlet.getImmediateParent(tempoutlet.getId()).stream().map(s -> s.hierarchy).collect(Collectors.toList()));
            if (StringUtils.isNotEmpty(hierarchy)) {
                tempoutlet.setHierarchy(hierarchy);
            } else {
                //logger.warn("Hierarchy logs: Null hierarchy found for outlet {}. Skipping setHierarchy() operation",tempoutlet.getOutletCode());
            }
        }
    }

    public void setOutletSupplier(CkOutletDetails outletDetails) throws JsonProcessingException {
        if (NullUtils.isNotNull(outletDetails.getImmediateParent(outletDetails.getId())) && !outletDetails.getImmediateParent(outletDetails.getId()).isEmpty() && outletDetails.getImmediateParent(outletDetails.getId()).stream().noneMatch(hierarchyMetaData -> NullUtils.isNull(hierarchyMetaData.getHierarchy()))) {
            List<String> supplierList = supplierInfoService.findSuppliers(outletDetails);
            ObjectNode extendedAttributes = (ObjectNode) outletDetails.getExtendedAttributes();
            if (extendedAttributes == null) {
                extendedAttributes = JSONUtils.getObjectMapper().createObjectNode();
            }
            extendedAttributes.putRawValue("supplier", new RawValue(JSONUtils.getObjectMapper().writeValueAsString(supplierList)));
            outletDetails.setExtendedAttributes(extendedAttributes);
        }
    }

    private void populateHierarchy(CkHierarchyMetadata hierarchyMetadata, List<CkHierarchyMetadata> existingMetadata,
                                   List<CkHierarchyMetadata> newMetadata, CkOutletDetails tempoutlet) {
        if (hierarchyMetadata.getId() == null) {
            String parentHierarchy = hierarchyMetadata.getHierarchy();
            // Dangerous code, this has to be fixed. Very bad workaround
            if (parentHierarchy != null) {
                Arrays.asList(parentHierarchy.split(",")).stream().forEach(tempHierarchy -> {
                    List<String> hierarchyusers = Arrays.asList(tempHierarchy.split(" > ")).stream().filter(parent -> !parent.equals(getCustomerAccountsService().getAdminLoginId())).collect(Collectors.toList());
                    String loginId = hierarchyusers.get(hierarchyusers.size() - 1);
                    List<CkHierarchyMetadata> lastParent = (List<CkHierarchyMetadata>) hierarchyMetaDataService
                            .findByImmediateParent(loginId);
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
                TimerUtils.withTime("Time taken to read and populate hierarchyMetadata record ", () -> {
                    List<CkHierarchyMetadata> lastParent = (List<CkHierarchyMetadata>) hierarchyMetaDataService
                            .findByImmediateParent(hierarchyMetadata.getImmediateParent());
                    if (lastParent != null) {
                        existingMetadata.addAll(lastParent);
                    }
                });
                // Handle the case where it is a new Hierarchy
            }
        } else {
            existingMetadata.add(hierarchyMetadata);
        }
    }

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
                CkLocation location = tempoutlet.getLocationHierarchy(tempoutlet);
                tempHierarchyMetaData.setLocationHierarchy((location == null) ? null : location.getLocationHierarchy());
                tempHierarchyMetaData.setLob(tempoutlet.getLob());
                newMetadata.add(tempHierarchyMetaData);
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
}