package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.utils.BatchInsertUtil;
import com.applicate.services.channelkart.utils.CdmDiffUtil;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.esotericsoftware.minlog.Log;
import com.fasterxml.jackson.core.type.TypeReference;
import com.salescode.dim.DataStreamJob;
import com.salescode.dim.PreProcessOperationResult;
import com.salescode.dim.PreProcessPipelineService;
import com.salescode.dim.cache.CacheManager;
import com.salescode.dim.cache.Cacheable;
import com.salescode.dim.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dim.etl.enrichment.service.EnrichmentInfoRegistry;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.etl.validation.service.DataValidationService;
import com.salescode.dim.etl.validation.service.ValidationExcludeGroupRegistry;
import com.salescode.dim.etl.validation.service.ValidationInfoRegistry;
import com.salescode.dim.jooq.generated.tables.pojos.CustomerAccount;
import com.salescode.dim.jooq.generated.tables.pojos.Metadata;
import com.salescode.dim.jooq.generated.tables.pojos.OutletDetailsHierarchymetadata;
import com.salescode.dim.jooq.generated.tables.pojos.UserParent;
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
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.util.RawValue;
import org.checkerframework.checker.units.qual.C;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


import static com.salescode.dim.jooq.generated.Tables.*;
import static com.salescode.dim.jooq.generated.Tables.CK_OUTLET_DETAILS_HIERARCHYMETADATA;
import static java.util.Arrays.stream;

public class OutletDetailsService extends AbstractCDMService<OutletDetails> {
    private static final Logger LOG = LoggerFactory.getLogger(OutletDetailsService.class);
    private final UserService userService;
    private final LocationService locationService;
    private final UserParentService userParentService;
    private final HierarchyMetadataService hierarchyMetadataService;
    private final CustomerAccountsService customerAccountsService;
    private final SupplierInfoService supplierInfoService;
    private final DataValidationService dataValidationService;
    private final DataEnrichmentService dataEnrichmentService;
    private final PreProcessPipelineService preProcessPipelineService;
    private final ValidationInfoRegistry validationInfoRegistry;
    private final ValidationExcludeGroupRegistry validationExcludeGroupRegistry;
    private final EnrichmentInfoRegistry enrichmentInfoRegistry;
    private ETLRegistry etlRegistry;


    public OutletDetailsService(){
        ExternalRegistryScanner externalRegistryScanner = ExternalRegistryScanner.getInstance();
        etlRegistry = ETLRegistry.getInstance(externalRegistryScanner);
        userService = new UserService();
        userParentService = new UserParentService();
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

    @Cacheable(cacheName = "dataintegration-outlets")
    public OutletDetails findByOutletCode(String outletcode) {
        com.salescode.dim.jooq.generated.tables.pojos.OutletDetails outletDetails = getDslContext().select(CK_OUTLET_DETAILS.asterisk()
                        .except(CK_OUTLET_DETAILS.COORDINATE))
                .from(CK_OUTLET_DETAILS).where(CK_OUTLET_DETAILS.OUTLETCODE.eq(outletcode))
                .fetchOneInto(com.salescode.dim.jooq.generated.tables.pojos.OutletDetails.class);
        return OutletDetails.of(outletDetails);
    }

    private List<User> preProcessUser(List<User> userList) {
//        userList.parallelStream().forEach(user -> {
//            preProcessPipelineService.preProcessPipeline(user, null);
//        });
        return userList;
    }


    private ConcurrentHashMap<String, User> populateUser(List<OutletDetails> outletDetailsList){
        List<User> userList = outletDetailsList.stream()
                .map(OutletDetails::getUserName)
                .collect(Collectors.toList());

        for(int i=0;i<userList.size();i++){
            userList.get(i).setReqId(outletDetailsList.get(i).getReqId());
            userList.get(i).setLocationHierarchy(outletDetailsList.get(i).getLocation());
        }

        List<User> preProcessedUserList = preProcessUser(userList);
        Collection<User> savedUserList = userService.batchSave(userList);
        ConcurrentHashMap<String, User> userMap = new ConcurrentHashMap<>();

        // Populate ConcurrentHashMap from savedUserList
        savedUserList.parallelStream()
                .forEach(user -> userMap.put(user.getLoginid(), user));

        return userMap;
    }

    private List<Location> populateLocation(List<OutletDetails> outletDetailsList){
        List<Location> locationList = outletDetailsList.stream()
                .map(OutletDetails::getLocation)
                .collect(Collectors.toList());
        List<Location> savedLocList = locationService.findLocationOrPersistLocation(locationList);
        return savedLocList;
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


    private ConcurrentHashMap<String,User> populateBatchAssociatedData(List<OutletDetails> outletDetailsList) {
        ConcurrentHashMap<String,User> savedUserList = populateUser(outletDetailsList);

        for (int i = 0; i < outletDetailsList.size(); i++) {
            String outletCode = outletDetailsList.get(i).getOutletcode();
            outletDetailsList.get(i).setLoginid(outletCode);
            outletDetailsList.get(i).setUserName(savedUserList.get(outletCode));
            outletDetailsList.get(i).setLocation(savedUserList.get(outletCode).getLocation());
            outletDetailsList.get(i).setLocationHierarchy(savedUserList.get(outletCode).getLocationHierarchy());
            outletDetailsList.get(i).setHierarchy(savedUserList.get(outletCode).getHierarchy());
            outletDetailsList.get(i).setNormalizedHierarchy(savedUserList.get(outletCode).getNormalizedHierarchy());
            outletDetailsList.get(i).setImmediateParent(savedUserList.get(outletCode).getImmediateParent());
            setOutletSupplier(outletDetailsList.get(i));
        }
        return savedUserList;
    }

    private ConcurrentHashMap<String,User> preBatchSave(List<OutletDetails> outletDetailsList){
        ConcurrentHashMap<String,User> savedUserList = populateBatchAssociatedData(outletDetailsList);
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
            fillCommonAttributes(outlet);
            new AttributeUpdateOverrideManager().overrideAttributes(outlet,savedList.get(outlet.getOutletcode()));
            if(outlet.getMapped()==null){
                outlet.setMapped(false);
            }
            super.addHash(outlet);
            if (savedList.get(outlet.getOutletcode()) == null) {
                outlet.setVersion(0);
                outlet.setId(UUID.randomUUID().toString());
                outlet.setChanged(true);
                itemsToInsert.add(outlet);
                outlet.setOperationPerformed(ActionType.INSERT);
            } else {
                OutletDetails existingOutlet = OutletDetails.of(savedList.get(outlet.getOutletcode()));
                outlet.setId(existingOutlet.getId());
                outlet.setVersion(existingOutlet.getVersion() + 1);

                String outlethash = outlet.getHash();
                String existingHash = existingOutlet.getHash();

                if (!Objects.equals(outlet.getHash(), existingOutlet.getHash())) {
                    outlet.setChanges(CdmDiffUtil.getChanges(outlet,existingOutlet));
                    outlet.setOperationPerformed(ActionType.UPDATE);
                    outlet.setChanged(true);
                    itemsToUpdate.add(outlet);
                }
            }
        }
        result.add(itemsToInsert);
        result.add(itemsToUpdate);
        return result;
    }

    public List<User> getRetailerParent(OutletDetails outletDetails, String designationType) {
        List<User> parentsList = new ArrayList<>();
        // Get the outlet's associated user
        User user = outletDetails.getUserName();
        if (user == null || user.getLoginid() == null) {
            return parentsList;
        }
        // Find all user-parent mappings for this user's loginId
        List<UserParent> userParentRecords = userParentService.findByUserLoginId(user.getLoginid());
        if (userParentRecords == null || userParentRecords.isEmpty()) {
            return parentsList;
        }
        // For each parent, fetch the user and filter by designation
        for (UserParent userParent : userParentRecords) {
            String parentLoginid = userParent.getParent();
            User parentUser = userService.findByLoginId(parentLoginid);
            if (parentUser != null && parentUser.getDesignation() != null) {
                boolean hasDesignation = parentUser.getDesignation().stream()
                        .anyMatch(d -> d.equalsIgnoreCase(designationType));
                if (hasDesignation) {
                    parentsList.add(parentUser);
                }
            }
        }

        return parentsList;
    }

    @Override
    public Collection<OutletDetails> batchSave(Collection<OutletDetails> outletDetailsList){
        LOG.info("Size of list is "  + outletDetailsList.size());
        List<OutletDetails> outletDetails = new ArrayList<>(outletDetailsList);
        LOG.info("Pre Batch Save Called with size " + outletDetails.size());
        Map<String,User> savedUserList = preBatchSave(outletDetails);
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
      //                          record.changed(CK_USER.ID, false); // Avoid updating primary key
                                return record;
                            })
                            .collect(Collectors.toList())
            ).execute();
        }
        if(!saveItemsList.get(0).isEmpty() || !saveItemsList.get(1).isEmpty()){
            postBatchSave(outletDetails);
        }
        LOG.info("Batch save successful");
        CacheManager.getInstance().evictAll("dataintegration-outlets");
        return outletDetails;
    }

    public void postBatchSave(List<OutletDetails> outletDetailsList){
        List<OutletDetailsHierarchymetadata> outletDetailsHierarchymetadata = setOutletHierarchyMetadata(outletDetailsList);
        saveOutletDetailHierarchyMetadata(outletDetailsHierarchymetadata);
    }


}
