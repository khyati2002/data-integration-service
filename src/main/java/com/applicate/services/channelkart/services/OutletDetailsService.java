package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.*;
import com.applicate.services.channelkart.utils.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.RawValue;
import com.applicate.services.channelkart.cache.DistributedCache;
import com.applicate.services.channelkart.client.properties.PropertyDefinition;
import com.applicate.services.channelkart.client.properties.PropertyRegistry;
import com.applicate.services.channelkart.models.diff.Change;
import com.applicate.services.channelkart.models.enums.ApplicationCategory;
import com.applicate.services.channelkart.models.enums.RoleName;
import com.applicate.services.channelkart.permission.services.AttributeUpdateOverrideManager;
import com.applicate.services.channelkart.repository.OutletDetailsRepository;

import com.applicate.services.channelkart.response.OperationResponse;
import com.applicate.services.channelkart.response.OperationStatus;
import com.applicate.services.channelkart.security.SecurityContextUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class OutletDetailsService extends AbstractCDMService<OutletDetails> {

    public static final String RETAILER = "retailer";
    private static final String WHOLESALER = "wholesaler";

    private final SupplierInfoService supplierInfoService;
    private final RoleService roleService;
    private final UserService userService;
    private LocationService locationService;
    private OutletDetailsRepository outletDetailsRepository;

    public static final String OUTLETS_CACHE_DOMAIN = "outlets";

    @Autowired
    private PropertyRegistry propertyRegistry;

    @Autowired
    private HierarchyMetaDataService hierarchyMetaDataService;

    @Autowired
    private DistributedCache distributedCache;
    @Autowired
    private PreProcessPipelineService preProcessPipelineService;

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private AttributeUpdateOverrideManager attributeUpdateOverrideManager;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public OutletDetailsService(OutletDetailsRepository outletDetailsRepository, UserService userService, SupplierInfoService supplierInfoService, RoleService roleService, LocationService locationService) {
        super(outletDetailsRepository);
        this.userService = userService;
        this.supplierInfoService = supplierInfoService;
        this.roleService = roleService;
        this.locationService = locationService;
        this.outletDetailsRepository = outletDetailsRepository;
    }

    private CustomerAccountsService getCustomerAccountsService() {
        return SpringContext.getBean(CustomerAccountsService.class);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public OutletDetails save(OutletDetails outletDetails) {
        return saveInternal(outletDetails);
    }


    public void createAssociatedData(OutletDetails outlet) {
        if (outlet.getUserName() != null) {
            addAssociatedData(outlet);
        } else if (propertyRegistry.getValue(PropertyDefinition.APPLICATION_CATETORY)
                .equals(ApplicationCategory.RETAIL.name())) {
            createRetailUser(outlet);
        }
    }

    public void refreshLocation(OutletDetails outletDetails) {
        /* location */
        if (NullUtils.isNotNull(outletDetails.getLocationHierarchy())) {
            Location location = outletDetails.getLocation();
            location = locationService.findLocationOrPersistLocation(location);
            outletDetails.setLocation(location);
            entityManager.detach(outletDetails.getLocation());
            outletDetails.setLocationHierarchy(location);
        }
    }

    public void fillRetailer(OutletDetails outlet, boolean hierarchy) {
        if (outlet.getUserName() != null) {
            if (StringUtils.isBlank(outlet.getUserName().getId())) {
                User tempuser = TimerUtils.withTime("Time taken UserService findByLoginId from outlet ",
                        () -> userService.findByLoginId(outlet.getUserName().getLoginId(), true, hierarchy));
                outlet.setRetailerInfo(tempuser);
            } else {
                outlet.setRetailerInfo(outlet.getUserName());
            }
        }
    }

    public OutletDetails prepareOutletDetails(OutletDetails outletDetails) {
//        /* location */
        refreshLocation(outletDetails);

        /* Retailer Info/Username */
        TimerUtils.withTime("prepareOutletDetails Time taken to fillRetailer ",
                () -> fillRetailer(outletDetails, false));
       return outletDetails;
    }

    void printLogsForNullHierarchy(OutletDetails outletDetails, String message){
        message += " for outletCode {}";
        if(propertyRegistry.getAsBoolean(PropertyDefinition.LOGS_FOR_NULL_LOCATION) &&
                (outletDetails.getLocationHierarchy()==null || outletDetails.getLocationHierarchy().getLocationHierarchy()==null)){
            logger.error(message, outletDetails.getOutletCode());
        }
    }

    public void clearCache(String lob, String outletCode) {
        if (StringUtils.isNotBlank(outletCode)) {
            distributedCache.clearCache(lob, OUTLETS_CACHE_DOMAIN, outletCode);
           // distributedCache.clearCache(lob, MicroOutletDetailsService.CACHE_DOMAIN, outletCode);
            userService.clearCache(lob, outletCode);
        }
    }

    public void clearCache(String lob, OutletDetails outlet) {
        if (outlet != null) {
            clearCache(lob, outlet.getOutletCode());
        }
    }


    public OutletDetails saveInternal(OutletDetails outletDetails) {
        clearCache(SecurityContextUtils.getLob(), outletDetails);
        createAssociatedData(outletDetails);
        printLogsForNullHierarchy(outletDetails,"Location null before prepare outlet details");
        OutletDetails tempoutlet = prepareOutletDetails(outletDetails);
        List<HierarchyMetaData> immediateParents = tempoutlet.getImmediateParent();
        if (immediateParents != null && !immediateParents.isEmpty()) {
            List<HierarchyMetaData> existingMetadata = new ArrayList<>();
            List<HierarchyMetaData> newMetadata = new ArrayList<>();
            for (HierarchyMetaData hierarchyMetadata : immediateParents) {
                populateHierarchy(hierarchyMetadata, existingMetadata, newMetadata, tempoutlet);
            }
            if (!newMetadata.isEmpty()) {
                try {
                    List<HierarchyMetaData> savedData = hierarchyMetaDataService.batchSave(newMetadata);
                    existingMetadata.addAll(savedData);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            if (!existingMetadata.isEmpty())
                tempoutlet.setImmediateParent(existingMetadata);
        }
        printLogsForNullHierarchy(tempoutlet,"Location null before setting hierarchy");
        setHierarchy(tempoutlet);

        tempoutlet.setNormalizedHierarchy(UserService.getNormalizedHierarchy(tempoutlet.getHierarchy()));

//        TimerUtils.withTime("Time taken to set supplier ",
//                () -> {
                    try {
         setOutletSupplier(tempoutlet);
                   } catch (JsonProcessingException e) {
                       logger.error("Error while setting supplier in outlet extended attribute");
                   }
//                });
        printLogsForNullHierarchy(tempoutlet,"Location null before saving outlet");
        if(tempoutlet.getMapped() == null) tempoutlet.setMapped(true);
        OutletDetails saved = super.save(tempoutlet);
//        AuditLogger.log("OutletDetails Updated",logMessage, AuditLogger.Status.SUCCESS, "OutletDetails", AuditLogger.Operations.UPDATE.toString(),null);
//        if (ObjectUtils.isEmpty(outletDetails.getChanges())) {
//            AuditLogger.log(LOG_TYPE, "Created new Outlet with outletCode '{}'", outletDetails.getOutletCode());
//        } else {
//            AuditLogger.log(LOG_TYPE, "Outlet with outletCode '{}' updated with data '{}'",
//                    outletDetails.getOutletCode(), EntityUtils.getDataChanges(outletDetails.getChanges()));
//        }
        clearCache(SecurityContextUtils.getLob(), outletDetails);
        return saved;

    }


    private void createRetailUser(OutletDetails outlet) {
        User user = new User();
        user.setActiveStatus(outlet.getActiveStatus());
        user.setLoginId(outlet.getOutletCode());
        user.setUserAccountId(outlet.getOutletCode());
        user.setLocationHierarchy(outlet.getLocationHierarchy());
        user.setMobile(outlet.getContactno());
        user.setName(org.apache.commons.lang.StringUtils.isEmpty(outlet.getOutletName()) ? outlet.getOutletCode() : outlet.getOutletName());
        user.setImmediateParent(replicateRetailerOutletParent(outlet.getImmediateParent()));
        user.setDesignation(Set.of(RETAILER));
        List<Role> roles = roleService.getRoleAsList(RoleName.ROLE_USER.name());
        user.setRoles(roles);
        outlet.setUserName(user);
        outlet.setImmediateParent(new ArrayList<>(1));
        createAssociateDataWithLock(outlet);
    }

    private void createAssociateDataWithLock(OutletDetails outlet) {
//        User user = TimerUtils.withTime("Time taken to execute getUser([[" + outlet.getUserName().getLoginId()
//                + "]]) for outlet[[" + outlet.getOutletCode() + "]]", k ->
        User user = getUser(outlet.getUserName());
        outlet.setUserName(user);
        if(user.getDesignation().contains(RETAILER) || user.getDesignation().contains(WHOLESALER)) {
            outlet.setActiveStatus(outlet.getUserName().getActiveStatus());
        }
        if (ObjectUtils.isNotEmpty(user.getImmediateParent())) {
            List<HierarchyMetaData> hms = user.getImmediateParent().stream().map(parent -> {
                HierarchyMetaData hm = new HierarchyMetaData();
                hm.setHierarchy(user.getLoginId() + " > "
                        + (org.apache.commons.lang.StringUtils.isEmpty(parent.getHierarchy())
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

    @Override
    public OutletDetails refresh(OutletDetails cdmObject) {
        OutletDetails dbRecord = CdmDiffUtil.withOldModel(() -> findByOutletCode(cdmObject.getOutletCode(), true));
        if (dbRecord != null) {
            OutletDetails returnObj = EntityUtils.deepClone(dbRecord);
            cdmObject.setOldModel(dbRecord.getOldModel());
            attributeUpdateOverrideManager.mergeProperties(cdmObject, dbRecord);
            EntityUtils.copyProperties(cdmObject, returnObj, "version", "userName");
            if(NullUtils.isNotNull(cdmObject.getUserName())){
                returnObj.setUserName(cdmObject.getUserName());
            }
            return returnObj;
        }
        return cdmObject;
    }



    private User getUser(User user) {
        User out;
//        User od = TimerUtils.withTime("Time taken to execute findUserByLogindId:[[ " + user.getLoginId() + "]]",
//                k -> userService.refresh(user));
        User od = userService.refresh(user);
        if (od.getId() == null) {
            od = validateAndGetUser(user);
//            User u = TimerUtils.withTime(
//                    "Time taken to execute findUserByLogindId:[[" + user.getLoginId() + "]] with disabled cache",
//                    k -> userService.findByLoginId(user.getLoginId(), false));

            User u = userService.findByLoginId(user.getLoginId(), false);
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

    private User updateUser(User user) {
        User out;
        User outUser;
        String existingHash = user.getHash();
        if (user.canHash() && org.apache.commons.lang.StringUtils.isNotEmpty(existingHash) && existingHash.equals(user.hash())
                && user.getChanges().isEmpty()) {
            return user;
        } else {
            // Non-retailers which are already present in system must not be uploaded as
            // retailer.
//            logger.debug("Existing user hash for update user : {}, user can hash : {}, user hash : {}", existingHash,
//                    user.canHash(), user.canHash() ? user.hash() : "cannot hash");
            outUser = validateAndGetUser(user);

//            User u = TimerUtils.withTime(
//                    "Time taken to execute findUserByLogindId:[[" + user.getLoginId() + "]] with disabled cache",
//                    k -> userService.findByLoginId(user.getLoginId(), false));
            User u = userService.findByLoginId(user.getLoginId(), false);
            if (u != null) {
                out = u;
            } else {
                out = outUser;
            }
            return out;
        }
    }


    private User validateAndGetUser(User user) {
        OperationResponse operationResponse = preProcessPipelineService.process(user, Optional.empty());
        if (operationResponse.getStatus().equals(OperationStatus.Success)) {
            Set<String> hierarchyStr = populateUserParentHierarchy(user);
            if (!hierarchyStr.isEmpty()) {
                String hierarchy = org.apache.commons.lang.StringUtils.join(hierarchyStr, ",");
                if (org.apache.commons.lang.StringUtils.isNotEmpty(hierarchy)) {
                    user.setHierarchy(hierarchy);
                    user.setNormalizedHierarchy(UserService.getNormalizedHierarchy(user.getHierarchy()));
                } else {
                  logger.warn("Hierarchy logs: Null hierarchy found for user {}. Skipping setHierarchy() operation", user.getLoginId());
                }
            }
            return userService.save(user);
        }
        throw new RuntimeException("Preprocess pipeline failed");
    }
//        else {
//            throw new PreprocessFailedException(pipelineService.getError(operationResponse), operationResponse);
//        }


    private Set<String> populateUserParentHierarchy(User user){
        Set<String> hierarchyStr=new HashSet<>();
        if (ObjectUtils.isNotEmpty(user.getImmediateParent())) {
            Set<String> uniqueParents = user.getImmediateParent().stream().map(HierarchyMetaData::getImmediateParent).collect(Collectors.toSet());
            uniqueParents.forEach(parent -> {
                List<HierarchyMetaData> hierarchyMetaDataList = (List) hierarchyMetaDataService.findByImmediateParent(parent);
                if (hierarchyMetaDataList.isEmpty()) {
                    hierarchyStr.add(user.getLoginId() + " > " + parent + " > " + getCustomerAccountsService().getAdminLoginId());
                } else {
                    hierarchyMetaDataList.forEach(hmList -> hierarchyStr.add(user.getLoginId() + " > "
                            + (org.apache.commons.lang.StringUtils.isEmpty(hmList.getHierarchy())
                            ? hmList.getImmediateParent() + " > " + getCustomerAccountsService().getAdminLoginId()
                            : hmList.getHierarchy())));
                }
            });
        }
        return hierarchyStr;
    }


    //
    private void addAssociatedData(OutletDetails outlet) {

        if (propertyRegistry.getValue(PropertyDefinition.APPLICATION_CATETORY).equals(ApplicationCategory.RETAIL.name()) && (outlet.getUserName().getDesignation().contains(RETAILER) || outlet.getUserName().getDesignation().contains(WHOLESALER))) {
            outlet.getChanges().forEach(changed ->

                    setUserAssociateData(outlet,changed)
            );
        }
        GlobalLock.withLock(outlet.getUserName().getLoginId(), s ->
                TimerUtils.withTime("Time taken to execute updateUser([[" + outlet.getUserName().getLoginId() + "]]) for outlet[[" + outlet.getOutletCode() + "]]", () -> createAssociateDataWithLock(outlet)));


    }


    private void setUserAssociateData(OutletDetails outlet, Change<Serializable> changed) {
        if (changed.getName().equalsIgnoreCase("activestatus")) {
            outlet.getUserName().setActiveStatus(outlet.getActiveStatus());
        }
        if (changed.getName().equalsIgnoreCase("outletname")) {
            outlet.getUserName().setName(outlet.getOutletName());
        }
        if (changed.getName().equalsIgnoreCase("contactno")) {
            outlet.getUserName().setMobile(outlet.getContactno());
        }
        if (changed.getName().equalsIgnoreCase("locationhierarchy")) {
            outlet.getUserName().setLocationHierarchy(outlet.getLocationHierarchy());
        }
        if (changed.getName().equalsIgnoreCase("immediateparent")) {
            List<HierarchyMetaData> parentList = outlet.getImmediateParent().stream()
                    .filter(h -> !h.getImmediateParent().equalsIgnoreCase(outlet.getUserName().getLoginId()))
                    .collect(Collectors.toList());
            if (!parentList.isEmpty()) {
                outlet.getUserName().setImmediateParent(replicateRetailerOutletParent(parentList));
                outlet.setImmediateParent(new ArrayList<>(1));
            }
        }
    }


    private void populateHierarchy(HierarchyMetaData hierarchyMetadata, List<HierarchyMetaData> existingMetadata,
                                   List<HierarchyMetaData> newMetadata, OutletDetails tempoutlet) {
        if (hierarchyMetadata.getId() == null) {
            String parentHierarchy = hierarchyMetadata.getHierarchy();
            // Dangerous code, this has to be fixed. Very bad workaround
            if (parentHierarchy != null) {
                Arrays.asList(parentHierarchy.split(",")).stream().forEach(tempHierarchy -> {
                    List<String> hierarchyusers = Arrays.asList(tempHierarchy.split(" > ")).stream().filter(parent->!parent.equals(getCustomerAccountsService().getAdminLoginId())).collect(Collectors.toList());
                    String loginId = hierarchyusers.get(hierarchyusers.size() - 1);
                    List<HierarchyMetaData> lastParent = (List<HierarchyMetaData>) hierarchyMetaDataService
                            .findByImmediateParent(loginId);
                    if(lastParent.isEmpty()){
                        HierarchyMetaData hmd=new HierarchyMetaData();
                        hmd.setImmediateParent(loginId);
                        hmd.setHierarchy(loginId + " > " + getCustomerAccountsService().getAdminLoginId());
                        setHierarchyElement(hmd,hierarchyusers,hierarchyMetadata,existingMetadata,newMetadata,tempoutlet);
                    }else {
                        lastParent.stream().forEach(element -> setHierarchyElement(element, hierarchyusers,
                                hierarchyMetadata, existingMetadata, newMetadata, tempoutlet));
                    }
                });
            } else {
                // Handle the case at which Hierarchy is null
//                TimerUtils.withTime("Time taken to read and populate hierarchyMetadata record ", () -> {
                    List<HierarchyMetaData> lastParent = (List<HierarchyMetaData>) hierarchyMetaDataService
                            .findByImmediateParent(hierarchyMetadata.getImmediateParent());
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


    private void setHierarchyElement(HierarchyMetaData element, List<String> hierarchyusers,
                                     HierarchyMetaData hierarchyMetadata, List<HierarchyMetaData> existingMetadata,
                                     List<HierarchyMetaData> newMetadata, OutletDetails tempoutlet) {
        String hierarchy = element.getHierarchy();
        if (hierarchy != null) {
            List<String> tempList = new ArrayList<>(hierarchyusers);
            tempList.remove(tempList.size() - 1);
            tempList.add(hierarchy);
            String joinedHierarchy = StringUtils.join(tempList, " > ");

            HierarchyMetaData hm = hierarchyMetaDataService.findByHierarchy(joinedHierarchy);
            if (hm != null && existingMetadata.stream().noneMatch(np->np.getHierarchy().equals(joinedHierarchy))) {
                existingMetadata.add(hm);
            } else if(NullUtils.isNull(hm) && newMetadata.stream().noneMatch(np->np.getHierarchy().equals(joinedHierarchy))) {
                HierarchyMetaData tempHierarchyMetaData = new HierarchyMetaData();
                EntityUtils.copyProperties(hierarchyMetadata, tempHierarchyMetaData);
                tempHierarchyMetaData.setHierarchy(joinedHierarchy);
                Location location = tempoutlet.getLocationHierarchy();
                tempHierarchyMetaData.setLocationHierarchy((location == null) ? null : location.getLocationHierarchy());
                tempHierarchyMetaData.setLob(tempoutlet.getLob());
                newMetadata.add(tempHierarchyMetaData);
            }
        }
    }

    private void setHierarchy(OutletDetails tempoutlet) {
        if (tempoutlet.getImmediateParent() != null && !tempoutlet.getImmediateParent().isEmpty()) {
            String hierarchy = String.join(",",
                    tempoutlet.getImmediateParent().stream().map(s -> s.hierarchy).collect(Collectors.toList()));
            if(StringUtils.isNotEmpty(hierarchy)) {
                tempoutlet.setHierarchy(hierarchy);
            }else{
               // logger.warn("Hierarchy logs: Null hierarchy found for outlet {}. Skipping setHierarchy() operation",tempoutlet.getOutletCode());
            }
        }
    }

    private List<HierarchyMetaData> replicateRetailerOutletParent(List<HierarchyMetaData> hms) {
        if (NullUtils.isNull(hms)) {
            return new ArrayList<>(1);
        }
        List<HierarchyMetaData> parentList = new ArrayList<>(hms.size());
        hms.forEach(hm -> parentList.add(EntityUtils.deepClone(hm)));
        return parentList;
    }

    //
    public void setOutletSupplier(OutletDetails outletDetails) throws JsonProcessingException {
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

    public OutletDetails findByOutletCode(String outletCode, boolean cache) {
        if (outletCode == null) {
            return null;
        }
        Function<String, OutletDetails> function = (String oc) -> {
            OutletDetailsService service = SpringContext.getBean(OutletDetailsService.class);
            return service.getLoadedOutletObject(oc);
        };

        return (cache)
                ? distributedCache.withCache(SecurityContextUtils.getLob(), OUTLETS_CACHE_DOMAIN, outletCode, function)
                : function.apply(outletCode);
    }

    public OutletDetails getLoadedOutletObject(String outletCode) {
        OutletDetails outlet = outletDetailsRepository.findByOutletCode(outletCode);
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
                User u = userService.getLoadedUserObject(outlet.getUserName().getLoginId(), true);
                outlet.setUserName(u);
            }
        }
        return outlet;
    }

    public OutletDetails findByOutletCode(String outletCode) {
        return findByOutletCode(outletCode, true);
    }
}
