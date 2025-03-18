package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.utils.BatchInsertUtil;
import com.applicate.services.channelkart.utils.CdmDiffUtil;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.esotericsoftware.minlog.Log;
import com.salescode.dim.DataStreamJob;
import com.salescode.dim.PreProcessOperationResult;
import com.salescode.dim.PreProcessPipelineService;
import com.salescode.dim.cache.Cacheable;
import com.salescode.dim.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dim.etl.enrichment.service.EnrichmentInfoRegistry;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.etl.validation.service.DataValidationService;
import com.salescode.dim.etl.validation.service.ValidationExcludeGroupRegistry;
import com.salescode.dim.etl.validation.service.ValidationInfoRegistry;
import com.salescode.dim.jooq.generated.tables.pojos.CustomerAccount;
import com.salescode.dim.jooq.generated.tables.pojos.OutletDetailsHierarchymetadata;
import com.salescode.dim.jooq.generated.tables.records.CkOutletDetailsRecord;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.Location;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.User;
import com.salescode.dim.scanner.ExternalRegistryScanner;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.util.RawValue;
import org.checkerframework.checker.units.qual.C;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.*;
import static com.salescode.dim.jooq.generated.Tables.CK_OUTLET_DETAILS_HIERARCHYMETADATA;

public class OutletDetailsService extends AbstractCDMService<OutletDetails> {
    private static final Logger LOG = LoggerFactory.getLogger(OutletDetailsService.class);
    private final UserService userService;
    private final LocationService locationService;
    private final HierarchyMetadataService hierarchyMetadataService;
    private final CustomerAccountsService customerAccountsService;
    private final SupplierInfoService supplierInfoService;
    private final DataValidationService dataValidationService;
    private final DataEnrichmentService dataEnrichmentService;
    private final ExternalRegistryScanner externalRegistryScanner = ExternalRegistryScanner.getInstance();
    private final PreProcessPipelineService preProcessPipelineService;
    private final ValidationInfoRegistry validationInfoRegistry;
    private final ValidationExcludeGroupRegistry validationExcludeGroupRegistry;
    private final EnrichmentInfoRegistry enrichmentInfoRegistry;
    private final ETLRegistry etlRegistry = ETLRegistry.getInstance(externalRegistryScanner);

    public OutletDetailsService(){
        userService = new UserService();
        locationService = new LocationService();
        validationInfoRegistry = new ValidationInfoRegistry(getDslContext());
        validationExcludeGroupRegistry = new ValidationExcludeGroupRegistry(getDslContext());
        enrichmentInfoRegistry = new EnrichmentInfoRegistry(getDslContext());
        dataValidationService = new DataValidationService(validationInfoRegistry,validationExcludeGroupRegistry,etlRegistry);
        dataEnrichmentService = new DataEnrichmentService(enrichmentInfoRegistry,etlRegistry);
        preProcessPipelineService = new PreProcessPipelineService(dataValidationService, dataEnrichmentService);
        hierarchyMetadataService = new HierarchyMetadataService();
        customerAccountsService = new CustomerAccountsService();
        supplierInfoService = new SupplierInfoService(getDslContext());
    }

    @Cacheable
    public OutletDetails findByOutletCode(String outletcode) {
        com.salescode.dim.jooq.generated.tables.pojos.OutletDetails outletDetails = getDslContext().select(CK_OUTLET_DETAILS.asterisk()
                        .except(CK_OUTLET_DETAILS.COORDINATE))
                .from(CK_OUTLET_DETAILS).where(CK_OUTLET_DETAILS.OUTLETCODE.eq(outletcode))
                .fetchOneInto(com.salescode.dim.jooq.generated.tables.pojos.OutletDetails.class);
       return OutletDetails.of(outletDetails);
    }

    private List<User> preProcessUser(List<User> userList){
        userList.forEach(user -> {
           preProcessPipelineService.preProcessPipeline(user, null);
        });
        return userList;
    }

    private List<User> populateUser(List<OutletDetails> outletDetailsList){
        List<User> userList = outletDetailsList.stream()
                .map(OutletDetails::getUserName)
                .collect(Collectors.toList());

        List<User> preProcessedUserList = preProcessUser(userList);
        List<User> savedUserList = userService.batchSave(userList);
        return savedUserList;
    }

    private List<Location> populateLocation(List<OutletDetails> outletDetailsList){
        List<Location> locationList = outletDetailsList.stream()
                .map(OutletDetails::getLocation)
                .collect(Collectors.toList());
        List<Location> savedLocList = locationService.findLocationOrPersistLocation(locationList);
        return savedLocList;
    }

    private void populateUserOutletHierarchy(User user, OutletDetails outlet) {
        if (ObjectUtils.isEmpty(outlet.getImmediateParent())) {
            outlet.setImmediateParent(user.getImmediateParent());
        }
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

    public void setOutletSupplier(OutletDetails outletDetails)  {
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


    private List<User> populateBatchAssociatedData(List<OutletDetails> outletDetailsList) {
        List<User> savedUserList = populateUser(outletDetailsList);
        List<Location> savedLocList = populateLocation(outletDetailsList);

        for (int i = 0; i < outletDetailsList.size(); i++) {
            outletDetailsList.get(i).setLoginid(outletDetailsList.get(i).getOutletcode());
            outletDetailsList.get(i).setUserName(savedUserList.get(i));
            populateUserOutletHierarchy(savedUserList.get(i), outletDetailsList.get(i));
            outletDetailsList.get(i).setLocation(savedLocList.get(i));
            outletDetailsList.get(i).setLocationHierarchy(savedLocList.get(i).getLocationHierarchy());
            setOutletHierarchy(outletDetailsList.get(i));
            setOutletSupplier(outletDetailsList.get(i));
        }
        return savedUserList;
    }

    private List<User> preBatchSave(List<OutletDetails> outletDetailsList){
        List<User> savedUserList = populateBatchAssociatedData(outletDetailsList);
        return savedUserList;
    }

    public void saveOutletDetailHierarchyMetadata(List<OutletDetailsHierarchymetadata> outletDetailsHierarchymetadata) {
        BatchInsertUtil.saveBatchWithDuplicateCheck(
                getDslContext(),
                outletDetailsHierarchymetadata,
                CK_OUTLET_DETAILS_HIERARCHYMETADATA,
                item -> DSL.row(item.getOutletId(), item.getHierarchyMetadataId()),
                CK_OUTLET_DETAILS_HIERARCHYMETADATA.OUTLET_ID,
                CK_OUTLET_DETAILS_HIERARCHYMETADATA.HIERARCHY_METADATA_ID,
                item -> getDslContext().insertInto(CK_OUTLET_DETAILS_HIERARCHYMETADATA)
                        .set(CK_OUTLET_DETAILS_HIERARCHYMETADATA.OUTLET_ID, item.getOutletId())
                        .set(CK_OUTLET_DETAILS_HIERARCHYMETADATA.HIERARCHY_METADATA_ID, item.getHierarchyMetadataId())
        );
    }

    public List<List<OutletDetails>> getItemsToSaveList(List<OutletDetails> outletDetailsList){
        List<List<OutletDetails>> result = new ArrayList<>();
        List<String> outletCodes = outletDetailsList.stream()
                .map(OutletDetails::getOutletcode)
                .collect(Collectors.toList());

        Map<String, com.salescode.dim.jooq.generated.tables.pojos.OutletDetails> savedList = getDslContext()
                .select(CK_OUTLET_DETAILS.asterisk().except(CK_OUTLET_DETAILS.COORDINATE))
                .from(CK_OUTLET_DETAILS)
                .where(CK_OUTLET_DETAILS.OUTLETCODE.in(outletCodes))
                .fetch()
                .intoMap(CK_OUTLET_DETAILS.OUTLETCODE, record -> record.into(com.salescode.dim.jooq.generated.tables.pojos.OutletDetails.class));
        List<OutletDetails> itemsToInsert = new ArrayList<>();
        List<OutletDetails> itemsToUpdate = new ArrayList<>();
        for (OutletDetails outlet : outletDetailsList) {
            fillAttributes(outlet,OutletDetails.of(savedList.get(outlet.getOutletcode())));
            super.addHash(outlet);
            if (savedList.get(outlet.getOutletcode()) == null) {
                outlet.setVersion(0);
                outlet.setId(UUID.randomUUID().toString());
                outlet.setMapped(true);
                itemsToInsert.add(outlet);
                outlet.setOperationPerformed(ActionType.INSERT);
            } else {
                OutletDetails existingOutlet = OutletDetails.of(savedList.get(outlet.getOutletcode()));
                outlet.setId(existingOutlet.getId());
                outlet.setVersion(existingOutlet.getVersion() + 1);
                outlet.setMapped(true);

                String outlethash = outlet.getHash();
                String existingHash = existingOutlet.getHash();

                if (!Objects.equals(outlet.getHash(), existingOutlet.getHash())) {
                    outlet.setChanges(CdmDiffUtil.getChanges(outlet,existingOutlet));
                    outlet.setOperationPerformed(ActionType.UPDATE);
                    itemsToUpdate.add(outlet);
                }
            }
        }
        result.add(itemsToInsert);
        result.add(itemsToUpdate);
        return result;
    }

    @Override
    public Collection<OutletDetails> batchSave(Collection<OutletDetails> outletDetailsList){
        List<OutletDetails> outletDetails = new ArrayList<>(outletDetailsList);
        LOG.info("Pre Batch Save Called with size " + outletDetails.size());
        List<User> savedUserList = preBatchSave(outletDetails);
        List<List<OutletDetails>> saveItemsList = getItemsToSaveList(outletDetails);
        if (!saveItemsList.get(0).isEmpty()) {
            getDslContext().batchInsert(
                    saveItemsList.get(0).stream()
                            .map(outlet -> getDslContext().newRecord(CK_OUTLET_DETAILS, outlet))
                            .collect(Collectors.toList())
            ).execute();
        }
        if (!saveItemsList.get(1).isEmpty()) {
            getDslContext().batchUpdate(
                    saveItemsList.get(1).stream()
                            .map(outlet -> {
                                CkOutletDetailsRecord record = getDslContext().newRecord(CK_OUTLET_DETAILS, outlet);
                                record.changed(CK_USER.ID, false); // Avoid updating primary key
                                return record;
                            })
                            .collect(Collectors.toList())
            ).execute();
        }
        postBatchSave(outletDetails);
        return outletDetails;
    }

    public void postBatchSave(List<OutletDetails> outletDetailsList){
        List<OutletDetailsHierarchymetadata> outletDetailsHierarchymetadata = setOutletHierarchyMetadata(outletDetailsList);
        saveOutletDetailHierarchyMetadata(outletDetailsHierarchymetadata);
    }
}
