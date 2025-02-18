package com.salescode.channelkart.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.salescode.channelkart.cache.DistributedCache;
import com.salescode.channelkart.repository.OutletDetailsRepository;
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
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


import static com.salescode.jooq.generated.Tables.CK_CUSTOMER_ACCOUNT;
import static com.salescode.jooq.generated.Tables.CK_OUTLET_DETAILS_HIERARCHYMETADATA;
import static com.salescode.jooq.generated.tables.CkUserdesignation.CK_USERDESIGNATION;
import static com.salescode.jooq.generated.tables.CkUserRoles.CK_USER_ROLES;



@Service
public class OutletDetailsService extends AbstractCDMService<CkOutletDetails> {

    public static final String RETAILER = "retailer";
    private static final String WHOLESALER = "wholesaler";

    public static final String OUTLETS_CACHE_DOMAIN = "outlets";

    private Logger logger = LoggerFactory.getLogger(this.getClass());

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
    private OutletDetailsRepository outletDetailsRepository;

    @Autowired
    private UserParentService userParentService;

    @Autowired
    private SupplierMetaDataService supplierMetaDataService;

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


    public void createAssociatedData(CkOutletDetails outlet,Map<String,CkUser> userMap,Map<String,CkUserParent> userParentMap, Map<Pair<String,String>,CkHierarchyMetadata> hierarchyMetadataMap) {
        if (outlet.getUserName() != null) {
            addAssociatedData(outlet,userMap,userParentMap,hierarchyMetadataMap);
        } else if (propertyRegistry.getValue(PropertyDefinition.APPLICATION_CATETORY)
                .equals(ApplicationCategory.RETAIL.name())) {
            createRetailUser(outlet, userMap, userParentMap,hierarchyMetadataMap);
        }
    }


    public void refreshLocation(CkOutletDetails outletDetails, Map<String,CkLocation> locationMap) {
//        /* location */
        if (NullUtils.isNotNull(outletDetails.getLocation())) {
            CkLocation location = locationService.findLocationOrPersistLocation(outletDetails.getLocation(),locationMap);
            outletDetails.setLocation(location);
            outletDetails.setLocationHierarchy(location.getLocationHierarchy());
        }
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


    public CkOutletDetails save(CkOutletDetails cdmObject,Map<String,CkLocation> locationMap ,Map<String,CkUser> userMap,Map<String,CkUserParent> userParentMap, Map<Pair<String,String>,CkHierarchyMetadata> hierarchyMetadataMap){
        return saveInternal(cdmObject,locationMap,userMap,userParentMap,hierarchyMetadataMap);
    }

    public CkOutletDetails saveInternal(CkOutletDetails outletDetails, Map<String,CkLocation> locationMap,Map<String,CkUser> userMap,Map<String,CkUserParent> userParentMap, Map<Pair<String,String>,CkHierarchyMetadata> hierarchyMetadataMap) {
        clearCache(SecurityContextUtils.getLob(), outletDetails);
        refreshLocation(outletDetails, locationMap);
        TimerUtils.withTime("Time taken to createAssociatedData record ",
                () -> createAssociatedData(outletDetails, userMap,userParentMap,hierarchyMetadataMap));
        printLogsForNullHierarchy(outletDetails,"Location null before prepare outlet details");
        CkOutletDetails tempoutlet = outletDetails;
        List<CkHierarchyMetadata> immediateParents = tempoutlet.getImmediateParent();
        if (immediateParents != null && !immediateParents.isEmpty()) {
            List<CkHierarchyMetadata> existingMetadata = new ArrayList<>();
            List<CkHierarchyMetadata> newMetadata = new ArrayList<>();
            for (CkHierarchyMetadata hierarchyMetadata : immediateParents) {
                populateHierarchy(hierarchyMetadata, existingMetadata, newMetadata, tempoutlet,hierarchyMetadataMap);
            }
            if (!newMetadata.isEmpty()) {
                existingMetadata.addAll(newMetadata);
            }
            if (!existingMetadata.isEmpty())
                tempoutlet.setImmediateParent(existingMetadata);

            existingMetadata.forEach(hierarchy ->{
                hierarchyMetadataMap.put(Pair.of(hierarchy.getHierarchy(),hierarchy.getParent()),hierarchy);
            });
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
     if(tempoutlet.getLastModifiedTime() == null) tempoutlet.setLastModifiedTime(new Date());

     return tempoutlet;
        }


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


    private void createRetailUser(CkOutletDetails outlet,Map<String,CkUser> userMap,Map<String,CkUserParent> userParentMap, Map<Pair<String,String>,CkHierarchyMetadata> hierarchyMetadataMap) {
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
        createAssociateDataWithLock(outlet, userMap, userParentMap,hierarchyMetadataMap);
        //setRoles(user.getRoles(),user);
    }


    @Override
    public CkOutletDetails refresh(CkOutletDetails cdmObject) {
        CkOutletDetails dbRecord = CdmDiffUtil.withOldModel(() -> findByOutletCode(cdmObject.getOutletcode(), true));
        if (dbRecord != null) {
            int version = dbRecord.getVersion();
            cdmObject.setVersion(version);
            cdmObject.setId(dbRecord.getId());
            cdmObject.setHash(dbRecord.getHash());
            return cdmObject;
        }
        return cdmObject;
    }

    public CkOutletDetails findByOutletCode(String outletCode, boolean cache) {
        if (outletCode == null) {
            return null;
        }
        Function<String, CkOutletDetails> function = (String oc) -> {
            OutletDetailsService service = SpringContext.getBean(OutletDetailsService.class);
            return service.getLoadedOutletObject(oc);
        };

        return (cache)
                ? distributedCache.withCache(SecurityContextUtils.getLob(), OUTLETS_CACHE_DOMAIN, outletCode, function)
                : function.apply(outletCode);
    }

    public CkOutletDetails getLoadedOutletObject(String outletCode) {
        CkOutletDetails outlet = outletDetailsRepository.findByOutletCode(outletCode);
        return outlet;
    }

    private void createAssociateDataWithLock(CkOutletDetails outlet,Map<String,CkUser> userMap,Map<String,CkUserParent> userParentMap, Map<Pair<String,String>,CkHierarchyMetadata> hierarchyMetadataMap) {
        CkUser user = getUser(outlet.getUserName(),userMap,userParentMap,hierarchyMetadataMap);
        outlet.setUserName(user);
        outlet.setActiveStatus(outlet.getUserName().getActiveStatus());
    }


    private CkUser getUser(CkUser user,Map<String,CkUser> userMap,Map<String,CkUserParent> userParentMap,Map<Pair<String,String>,CkHierarchyMetadata> hierarchyMetadataMap) {
        CkUser out,u;
//        User od = TimerUtils.withTime("Time taken to execute findUserByLogindId:[[ " + user.getLoginId() + "]]",
//                k -> userService.refresh(user));
        CkUser od = userService.refresh(user,userMap);
        if (od.getId() == null) {
          od = validateAndGetUser(user,userMap,userParentMap,hierarchyMetadataMap);
            if(userMap.containsKey(user.getLoginid())){
                u = userMap.get(user.getLoginid());
            }
            else {
                u = userService.findByLoginId(user.getLoginid(), false);
            }
            if (u != null) {
                out = u;
            } else {
                out = od;
            }
        } else {
            out = updateUser(od,userMap,userParentMap,hierarchyMetadataMap);
        }
        return out;
    }

    private CkUser updateUser(CkUser user,Map<String,CkUser> userMap,Map<String,CkUserParent> userParentMap,Map<Pair<String,String>,CkHierarchyMetadata> hierarchyMetadataMap ) {
        CkUser out,u;
        CkUser outUser;
        outUser = validateAndGetUser(user,userMap,userParentMap,hierarchyMetadataMap);
        if(userMap.containsKey(user.getLoginid())){
            u = userMap.get(user.getLoginid());
        }
        else {
            u = userService.findByLoginId(user.getLoginid(), false);
        }
        if (u != null) {
            out = u;
        } else {
            out = outUser;
        }
            return out;
        }

  private CkUser validateAndGetUser(CkUser user,Map<String,CkUser> userMap,Map<String,CkUserParent> userParentMap,Map<Pair<String,String>,CkHierarchyMetadata> hierarchyMetadataMap) {
        OperationResponse operationResponse = TimerUtils.withTime(
                "Time Taken to exceute preprocess pipeline for class:[[" + user.getClass() + "]]",
                k -> preProcessPipelineService.process(user));

//        if (operationResponse.getStatus().compareTo(OperationStatus.Success) == 0) {
            Set<String> hierarchyStr = populateUserParentHierarchy(user,hierarchyMetadataMap);
            if (!hierarchyStr.isEmpty()) {
                String hierarchy = StringUtils.join(hierarchyStr, ",");
                if (StringUtils.isNotEmpty(hierarchy)) {
                    user.setHierarchy(hierarchy);
                    user.setNormalizedHierarchy(UserService.getNormalizedHierarchy(user.getHierarchy()));
                } else {
                  //  logger.warn("Hierarchy logs: Null hierarchy found for user {}. Skipping setHierarchy() operation", user.getLoginId());
                }
                return userService.save(user,userMap,userParentMap);
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



    private Set<String> populateUserParentHierarchy(CkUser user,Map<Pair<String,String>,CkHierarchyMetadata> hierarchyMetadataMap){
        Set<String> hierarchyStr=new HashSet<>();
        List<CkHierarchyMetadata> hierarchy = user.getImmediateParent();
        if (ObjectUtils.isNotEmpty(hierarchy)) {
            Set<String> uniqueParents = user.getImmediateParent().stream().map(CkHierarchyMetadata::getParent).collect(Collectors.toSet());
            uniqueParents.forEach(parent -> {
                List<CkHierarchyMetadata> hierarchyMetaDataList = (List) hierarchyMetaDataService.findByImmediateParent(parent,hierarchyMetadataMap);

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

    private void addAssociatedData(CkOutletDetails outlet,Map<String,CkUser> userMap,Map<String,CkUserParent> userParentMap, Map<Pair<String,String>,CkHierarchyMetadata> hierarchyMetadataMap){
        if (propertyRegistry.getValue(PropertyDefinition.APPLICATION_CATETORY).equals(ApplicationCategory.RETAIL.name()) && (outlet.getUserName().getDesignation().contains(RETAILER) || outlet.getUserName().getDesignation().contains(WHOLESALER))) {
                    Set<String> set = outlet.getUserName().getDesignation();
                    setUserAssociateData(outlet);
        }
        GlobalLock.withLock(outlet.getUserName().getLoginid(), s ->
                TimerUtils.withTime("Time taken to execute updateUser([[" + outlet.getUserName().getLoginid() + "]]) for outlet[[" + outlet.getOutletcode() + "]]", () -> createAssociateDataWithLock(outlet,userMap,userParentMap,hierarchyMetadataMap)));
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

    private void populateHierarchy(CkHierarchyMetadata hierarchyMetadata, List<CkHierarchyMetadata> existingMetadata,
                                   List<CkHierarchyMetadata> newMetadata, CkOutletDetails tempoutlet, Map<Pair<String,String>,CkHierarchyMetadata> hierarchyMetadataMap) {
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
                        hmd.setHierarchy(loginId + " > " + getCustomerAccountsService().getAdminLoginId());
                        hmd.setId(UUID.randomUUID().toString());
                        setHierarchyElement(hmd, hierarchyusers, hierarchyMetadata, existingMetadata, newMetadata, tempoutlet,hierarchyMetadataMap);
                    } else {
                        lastParent.stream().forEach(element -> setHierarchyElement(element, hierarchyusers,
                                hierarchyMetadata, existingMetadata, newMetadata, tempoutlet,hierarchyMetadataMap));
                    }
                });
            } else {
                   List<CkHierarchyMetadata> lastParent = (List<CkHierarchyMetadata>) hierarchyMetaDataService
                           .findByImmediateParent(hierarchyMetadata.getParent());
                    if (lastParent != null) {
                        existingMetadata.addAll(lastParent);
                    }
            }
        } else {
            existingMetadata.add(hierarchyMetadata);
        }
    }

    private void setHierarchyElement(CkHierarchyMetadata element, List<String> hierarchyusers,
                                     CkHierarchyMetadata hierarchyMetadata, List<CkHierarchyMetadata> existingMetadata,
                                     List<CkHierarchyMetadata> newMetadata, CkOutletDetails tempoutlet,Map<Pair<String,String>,CkHierarchyMetadata> hierarchyMetadataMap) {
        String hierarchy = element.getHierarchy();
        if (hierarchy != null) {
            List<String> tempList = new ArrayList<>(hierarchyusers);
            tempList.remove(tempList.size() - 1);
            tempList.add(hierarchy);
            String joinedHierarchy = StringUtils.join(tempList, " > ");
             CkHierarchyMetadata hm = hierarchyMetadataMap.entrySet()
                    .stream()
                    .filter(entry -> entry.getKey().getRight().equals(joinedHierarchy)) // Match left key
                    .map(Map.Entry::getValue) // Extract value
                    .findAny().orElse(hierarchyMetaDataService.findByHierarchy(joinedHierarchy)); // Get first match

            if (hm != null && existingMetadata.stream().noneMatch(np -> np.getHierarchy().equals(joinedHierarchy))) {
                existingMetadata.add(hm);
            } else if (NullUtils.isNull(hm) && newMetadata.stream().noneMatch(np -> np.getHierarchy().equals(joinedHierarchy))) {
                CkHierarchyMetadata tempHierarchyMetaData = new CkHierarchyMetadata();
                EntityUtils.copyProperties(hierarchyMetadata, tempHierarchyMetaData);
                tempHierarchyMetaData.setHierarchy(joinedHierarchy);
                CkLocation location = tempoutlet.getLocation();
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
                 logger.warn("Hierarchy logs: Null hierarchy found for outlet {}. Skipping setHierarchy() operation",tempoutlet.getOutletcode());
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
            logger.error(message, outletDetails.getOutletcode());
        }
    }

    public List<CkUserRoles> setRoles(Map<String,CkUser> userMap){
        return userMap.values()
                .stream()
                .map(user -> {
                    CkUserRoles userRoles = new CkUserRoles();
                    // Set the user ID
                    userRoles.setUserId(user.getId());
                    // Check if the user has any roles and then set the first role's id
                    if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                        userRoles.setRolesId(user.getRoles().get(0).getId());
                    } else {
                        // Optionally set a default value or handle the case where no roles exist
                        userRoles.setRolesId(null);
                    }
                    return userRoles;
                })
                .collect(Collectors.toList());
    }

    public List<CkUserdesignation> setDesignation(Map<String,CkUser> userMap){
        return userMap.values()
                .stream()
                .map(user -> {
                    CkUserdesignation userdesignation = new CkUserdesignation();
                    // Set the user ID
                    userdesignation.setLoginId(user.getLoginid());
                    // Check if the user has any roles and then set the first role's id
                    userdesignation.setDesignation(user.getDesignation().stream().collect(Collectors.joining(" ")));
                    return userdesignation;
                })
                .collect(Collectors.toList());
    }

    public void saveDesignation(List<CkUserdesignation> userDesignation) {
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

    public void saveOutletDetailHierarchyMetadata(List<CkOutletDetailsHierarchymetadata> outletDetailsHierarchymetadata) {
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

    public void saveRoles(List<CkUserRoles> userRoles) {
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

    private List<CkOutletDetailsHierarchymetadata> setOutletHierarchyMetadata(Map<String, CkOutletDetails> outletDetailsMap) {
        return outletDetailsMap.values().stream()
                .flatMap(outlet -> outlet.getImmediateParent().stream()
                        .map(hierarchy -> {
                            CkOutletDetailsHierarchymetadata metadata = new CkOutletDetailsHierarchymetadata();
                            // Set the outlet code
                            metadata.setOutletId(outlet.getId());
                            // Set the parent ID
                            metadata.setHierarchyMetadataId(hierarchy.getId());
                            return metadata;
                        })
                )
                .collect(Collectors.toList());
    }

    @Override
    public List<CkOutletDetails> batchSave(Iterable<CkOutletDetails> iterObj) {
        Map<String,CkOutletDetails> outletDetailsMap = new HashMap<>();
        Map<String,CkLocation> locationMap = new HashMap<>();
        Map<String,CkUser> userMap = new HashMap<>();
        Map<String,CkUserParent> userParentMap = new HashMap<>();
        Map<Pair<String,String>,CkHierarchyMetadata> hierarchyMetadataMap = new HashMap<>();
        iterObj.forEach(outletDetails -> outletDetailsMap.put(outletDetails.getOutletcode(),save(outletDetails, locationMap, userMap, userParentMap,hierarchyMetadataMap)));
        locationService.batchSave(locationMap);
        userParentService.batchSave(userParentMap);
        userService.batchSave(userMap);
        hierarchyMetaDataService.batchSave(hierarchyMetadataMap.values());
        List<CkUserRoles> roleList = setRoles(userMap);
        saveRoles(roleList);
        List<CkUserdesignation> userdesignationsList = setDesignation(userMap);
        saveDesignation(userdesignationsList);
        List<CkOutletDetails> res = super.batchSave(outletDetailsMap);
        List<CkOutletDetailsHierarchymetadata> outletDetailsHierarchymetadata = setOutletHierarchyMetadata(outletDetailsMap);
        saveOutletDetailHierarchyMetadata(outletDetailsHierarchymetadata);
        return res;
    }




}
