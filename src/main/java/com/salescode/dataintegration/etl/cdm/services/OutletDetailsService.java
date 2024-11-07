package com.salescode.dataintegration.etl.cdm.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.salescode.channelkart.utils.TimerUtils;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.jooq.generated.tables.pojos.CkOutletDetails;
import com.salescode.jooq.generated.tables.pojos.CkUser;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jooq.DSLContext;
import org.jooq.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.salescode.jooq.generated.tables.CkOutletDetails.CK_OUTLET_DETAILS;

@Service
public class OutletDetailsService extends AbstractCDMService<CkOutletDetails> {

    private final DSLContext dsl;

    @Autowired
    public OutletDetailsService(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public CkOutletDetails save(CkOutletDetails cdmObject) {
        TimerUtils.withTime("Time taken to createAssociatedData record ",
                () -> createAssociatedData(cdmObject));
        printLogsForNullHierarchy(cdmObject,"Location null before prepare outlet details");
        CkOutletDetails tempoutlet = TimerUtils.withTime("Time taken to prepareOutletDetails record ",
                k -> prepareOutletDetails(cdmObject));
        List<CkHierarchyMetaData> immediateParents = tempoutlet.getImmediateParent();
        if (immediateParents != null && !immediateParents.isEmpty()) {
            List<CkHierarchyMetaData> existingMetadata = new ArrayList<>();
            List<CkHierarchyMetaData> newMetadata = new ArrayList<>();
            for (CkHierarchyMetaData hierarchyMetadata : immediateParents) {
                populateHierarchy(hierarchyMetadata, existingMetadata, newMetadata, tempoutlet);
            }
            if (!newMetadata.isEmpty()) {
                List<HierarchyMetaData> savedData = hierarchyMetaDataService.batchSave(newMetadata);
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
                        logger.error("Error while setting supplier in outlet extended attribute");
                    }
                });
        printLogsForNullHierarchy(tempoutlet,"Location null before saving outlet");
        OutletDetails saved = super.save(tempoutlet);
        String logMessage = String.format(
                "OutletDetails is updated for id '%s', outletcode '%s', last updated on '%s', modified by '%s'",
                saved.getId(),
                saved.getOutletCode(),
                saved.getLastModifiedTime(),
                saved.getModifiedBy()
        );
        AuditLogger.log("OutletDetails Updated",logMessage, AuditLogger.Status.SUCCESS, "OutletDetails", AuditLogger.Operations.UPDATE.toString(),null);
        if (ObjectUtils.isEmpty(outletDetails.getChanges())) {
            AuditLogger.log(LOG_TYPE, "Created new Outlet with outletCode '{}'", outletDetails.getOutletCode());
        } else {
            AuditLogger.log(LOG_TYPE, "Outlet with outletCode '{}' updated with data '{}'",
                    outletDetails.getOutletCode(), EntityUtils.getDataChanges(outletDetails.getChanges()));
        }
        clearCache(SecurityContextUtils.getLob(), outletDetails);


        var record = dsl.newRecord(CK_OUTLET_DETAILS, cdmObject);

        dsl.insertInto(CK_OUTLET_DETAILS)
                .set(record)
                .onDuplicateKeyUpdate()
                .set(record)
                .execute();

        return cdmObject;
    }

    public void createAssociatedData(CkOutletDetails outlet) {
        if (outlet.getUserName() != null) {
            addAssociatedData(outlet);
        } else if (propertyRegistry.getValue(PropertyDefinition.APPLICATION_CATETORY)
                .equals(ApplicationCategory.RETAIL.name())) {
            createRetailUser(outlet);
        }
    }

    void printLogsForNullHierarchy(CkOutletDetails outletDetails, String message){
        message += " for outletCode {}";
        if(propertyRegistry.getAsBoolean(PropertyDefinition.LOGS_FOR_NULL_LOCATION) &&
                (outletDetails.getLocationHierarchy()==null || outletDetails.getLocationHierarchy().getLocationHierarchy()==null)){
            logger.error(message, outletDetails.getOutletcode());
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

    private void createRetailUser(CkOutletDetails outlet) {
        CkUser user = new CkUser();
        user.setActiveStatus(outlet.getActiveStatus());
        user.setLoginId(outlet.getOutletCode());
        user.setUserAccountId(outlet.getOutletCode());
        user.setLocationHierarchy(outlet.getLocationHierarchy());
        user.setMobile(outlet.getContactno());
        user.setName(StringUtils.isEmpty(outlet.getOutletName()) ? outlet.getOutletCode() : outlet.getOutletName());
        user.setImmediateParent(replicateRetailerOutletParent(outlet.getImmediateParent()));
        user.setDesignation(Set.of(RETAILER));
        List<Role> roles = roleService.getRoleAsList(RoleName.ROLE_USER.name());
        user.setRoles(roles);
        outlet.setUserName(user);
        outlet.setImmediateParent(new ArrayList<>(1));
        createAssociateDataWithLock(outlet);
    }

    private void createAssociateDataWithLock(CkOutletDetails outlet) {
        User user = TimerUtils.withTime("Time taken to execute getUser([[" + outlet.getUserName().getLoginId()
                + "]]) for outlet[[" + outlet.getOutletcode() + "]]", k -> getUser(outlet.getUserName()));
        outlet.setUserName(user);
        if(user.getDesignation().contains(RETAILER) || user.getDesignation().contains(WHOLESALER)) {
            outlet.setActiveStatus(outlet.getUserName().getActiveStatus());
        }
        if (ObjectUtils.isNotEmpty(user.getImmediateParent())) {
            List<HierarchyMetaData> hms = user.getImmediateParent().stream().map(parent -> {
                HierarchyMetaData hm = new HierarchyMetaData();
                hm.setHierarchy(user.getLoginId() + " > "
                        + (StringUtils.isEmpty(parent.getHierarchy())
                        ? parent.getImmediateParent() + " > " + getCustomerAccountsService().getAdminLoginId()
                        : parent.getHierarchy()));
                Location location = user.getLocationHierarchy();
                hm.setLocationHierarchy((location == null) ? null : location.getLocationHierarchy());
                hm.setImmediateParent(user.getLoginId());
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

    private void addAssociatedData(CkOutletDetails outlet) {

        if (propertyRegistry.getValue(PropertyDefinition.APPLICATION_CATETORY).equals(ApplicationCategory.RETAIL.name()) && (outlet.getUserName().getDesignation().contains(RETAILER) || outlet.getUserName().getDesignation().contains(WHOLESALER))) {
            outlet.getChanges().forEach(changed ->

                    setUserAssociateData(outlet,changed)
            );
        }
        GlobalLock.withLock(outlet.getUserName().getLoginId(), s ->
                TimerUtils.withTime("Time taken to execute updateUser([[" + outlet.getUserName().getLoginId() + "]]) for outlet[[" + outlet.getOutletCode() + "]]", () -> createAssociateDataWithLock(outlet)));

    }
}