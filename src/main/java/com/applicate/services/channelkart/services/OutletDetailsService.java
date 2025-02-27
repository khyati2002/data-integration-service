package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.converters.HierarchyMetaDataToStringConverter;
import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.RawValue;
import com.salescode.dim.jooq.generated.tables.pojos.Location;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.User;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.jooq.DSLContext;

import java.util.*;
import java.util.stream.Collectors;

public class OutletDetailsService extends AbstractCDMService<OutletDetails> {

    public static final String RETAILER = "retailer";
    private static final String WHOLESALER = "wholesaler";

    private static UserService userService;
    private static HierarchyMetadataService hierarchyMetadataService;

    private static OutletDetailsService instance;

    private static DSLContext dsl;
    public OutletDetailsService(DSLContext dsl) {
        super(dsl);
        OutletDetailsService.dsl = dsl;
    }

    public static OutletDetailsService getInstance(DSLContext dsl) {
        if (instance == null) {
            instance = new OutletDetailsService(dsl);
            userService = new UserService(dsl);
            hierarchyMetadataService = new HierarchyMetadataService(dsl);
        }
        return instance;
    }

    @Override
    public List<OutletDetails> batchSave(List<OutletDetails> outletDetailsList) {
        OutletDetailsService.getInstance(dsl);
        populateOutletDetails(outletDetailsList);
        populateOutletHierarchyAndSupplierDetails(outletDetailsList);
        return List.of();
    }


    public void populateOutletHierarchyAndSupplierDetails(List<OutletDetails> outletDetailsList) {
        for (OutletDetails tempoutlet : outletDetailsList) {
            List<HierarchyMetadata> immediateParents = tempoutlet.getImmediateParent();
            if (immediateParents != null && !immediateParents.isEmpty()) {
                List<HierarchyMetadata> existingMetadata = new ArrayList<>();
                List<HierarchyMetadata> newMetadata = new ArrayList<>();
                for (HierarchyMetadata hierarchyMetadata : immediateParents) {
                    populateHierarchy(hierarchyMetadata, existingMetadata, newMetadata, tempoutlet);
                }
                if (!newMetadata.isEmpty()) {
                    List<HierarchyMetadata> savedData = hierarchyMetadataService.batchSave(newMetadata);
                    existingMetadata.addAll(savedData);
                }
                if (!existingMetadata.isEmpty())
                    tempoutlet.setImmediateParent(existingMetadata);
            }
            //  printLogsForNullHierarchy(tempoutlet, "Location null before setting hierarchy");
            setHierarchy(tempoutlet);

            tempoutlet.setNormalizedHierarchy(UserService.getNormalizedHierarchy(tempoutlet.getHierarchy()));

            // TimerUtils.withTime("Time taken to set supplier ",
//                    () -> {
////                        try {
////                            setOutletSupplier(tempoutlet);
////                        } catch (JsonProcessingException e) {
////                       //     logger.error("Error while setting supplier in outlet extended attribute");
////                        }
//           //         });
//        }
        }
    }

    public void populateOutletDetails(List<OutletDetails> outletDetailsList) {
        createAssociatedData(outletDetailsList);
    }

    private void setHierarchy(OutletDetails tempoutlet) {
        List<HierarchyMetadata> list = tempoutlet.getImmediateParent();
        String immediateParent = new HierarchyMetaDataToStringConverter().convert(list);
        if (immediateParent != null && !immediateParent.isEmpty()) {
            String hierarchy = String.join(",",
                    list.stream().map(s -> s.getHierarchy()).collect(Collectors.toList()));
            if (StringUtils.isNotEmpty(hierarchy)) {
                tempoutlet.setHierarchy(hierarchy);
                tempoutlet.setHierarchy(hierarchy);
            } else {
                //         logger.warn("Hierarchy logs: Null hierarchy found for outlet {}. Skipping setHierarchy() operation",tempoutlet.getOutletcode());
            }
        }
    }

//    public void setOutletSupplier(OutletDetails outletDetails) throws com.fasterxml.jackson.core.JsonProcessingException {
//        if ((outletDetails.getImmediateParent())!=null&& outletDetails.getImmediateParent().isEmpty() && outletDetails.getImmediateParent().stream().noneMatch(hierarchyMetaData -> (hierarchyMetaData.getHierarchy())!=null)) {
//            List<String> supplierList = supplierInfoService.findSuppliers(outletDetails);
//            ObjectNode extendedAttributes = (ObjectNode) outletDetails.getExtendedAttributes();
//            if (extendedAttributes == null) {
//                extendedAttributes = JSONUtils.getObjectMapper().createObjectNode();
//            }
//            extendedAttributes.putRawValue("supplier", new RawValue(JSONUtils.getObjectMapper().writeValueAsString(supplierList)));
//            outletDetails.setExtendedAttributes(extendedAttributes);
//        }
//    }

    private void createAssociatedData(List<OutletDetails> outletDetailsList) {
        List<User> outletUserName = outletDetailsList.stream()
                .map(OutletDetails::getUserName)
                .collect(Collectors.toList());

        List<User> userList = getUser(outletUserName);
        for (int i = 0; i < userList.size(); i++) {
            outletDetailsList.get(i).setUserName(userList.get(i));
            if (userList.get(i).getDesignation().contains(RETAILER) || userList.get(i).getDesignation().contains(WHOLESALER)) {
                outletDetailsList.get(i).setActiveStatus(outletDetailsList.get(i).getUserName().getActiveStatus());
            }
            if (ObjectUtils.isNotEmpty(userList.get(i).getImmediateParent())) {
                User user = userList.get(i);
                List<HierarchyMetadata> hms = user.getImmediateParent().stream().map(parent -> {
                    HierarchyMetadata hm = new HierarchyMetadata();
                    hm.setHierarchy(user.getLoginid() + " > "
                            + (StringUtils.isEmpty(parent.getHierarchy())
                            ? parent.getParent() + " > " + "admin"
                            : parent.getHierarchy()));
                    String location = user.getLocationHierarchy();
                    hm.setLocationHierarchy((location == null) ? null : location);
                    hm.setImmediateParent(user.getLoginid());
                    return hm;
                }).collect(Collectors.toList());
                // If outlet defines its own immediate parent then it should not override by
                // user.
                // The whole hierarchy should be take care by populate hierarchy in next steps.
                if (ObjectUtils.isEmpty(outletDetailsList.get(i).getImmediateParent())) {
                    outletDetailsList.get(i).setImmediateParent(hms);
                }
            }
        }
    }

    private List<User> getUser(List<User> userList) {
        List<User> userSavedList = validateAndGetUser(userList);
        return userSavedList;
    }

    private List<User> validateAndGetUser(List<User> userList) {
        userList.forEach(user -> {
            Set<String> hierarchyStr = populateUserParentHierarchy(user);

            //   hierarchyMetadataService.batchSave()
            if (!hierarchyStr.isEmpty()) {
                String hierarchy = StringUtils.join(hierarchyStr, ",");
                if (StringUtils.isNotEmpty(hierarchy)) {
                    user.setHierarchy(hierarchy);
                    user.setNormalizedHierarchy(UserService.getNormalizedHierarchy(user.getHierarchy()));
                } else {
                    //        logger.warn("Hierarchy logs: Null hierarchy found for user {}. Skipping setHierarchy() operation", user.getLoginId());
                }
            }

        });
        return userService.batchSave(userList);
    }

    private Set<String> populateUserParentHierarchy(User user) {
        Set<String> hierarchyStr = new HashSet<>();
        if (ObjectUtils.isNotEmpty(user.getImmediateParent())) {
            Set<String> uniqueParents = user.getImmediateParent().stream().map(HierarchyMetadata::getParent).collect(Collectors.toSet());
            uniqueParents.forEach(parent -> {
                List<HierarchyMetadata> hierarchyMetaDataList = (List) hierarchyMetadataService.findByImmediateParent(parent);
                if (hierarchyMetaDataList.isEmpty()) {
                    hierarchyStr.add(user.getLoginid() + " > " + parent + " > " + "admin");
                } else {
                    hierarchyMetaDataList.forEach(hmList -> hierarchyStr.add(user.getLoginid() + " > "
                            + (StringUtils.isEmpty(hmList.getHierarchy())
                            ? hmList.getParent() + " > " + "admin"
                            : hmList.getHierarchy())));
                }
            });
        }
        return hierarchyStr;
    }

    private void populateHierarchy(HierarchyMetadata hierarchyMetadata, List<HierarchyMetadata> existingMetadata,
                                   List<HierarchyMetadata> newMetadata, OutletDetails tempoutlet) {
        if (hierarchyMetadata.getId() == null) {
            String parentHierarchy = hierarchyMetadata.getHierarchy();
            // Dangerous code, this has to be fixed. Very bad workaround
            if (parentHierarchy != null) {
                Arrays.asList(parentHierarchy.split(",")).stream().forEach(tempHierarchy -> {
                    List<String> hierarchyusers = Arrays.asList(tempHierarchy.split(" > ")).stream().filter(parent -> !parent.equals("admin")).collect(Collectors.toList());
                    String loginId = hierarchyusers.get(hierarchyusers.size() - 1);
                    List<HierarchyMetadata> lastParent = (List<HierarchyMetadata>) hierarchyMetadataService
                            .findByImmediateParent(loginId);
                    if (lastParent.isEmpty()) {
                        HierarchyMetadata hmd = new HierarchyMetadata();
                        hmd.setImmediateParent(loginId);
                        hmd.setHierarchy(loginId + " > " + "admin");
                        setHierarchyElement(hmd, hierarchyusers, hierarchyMetadata, existingMetadata, newMetadata, tempoutlet);
                    } else {
                        lastParent.stream().forEach(element -> setHierarchyElement(element, hierarchyusers,
                                hierarchyMetadata, existingMetadata, newMetadata, tempoutlet));
                    }
                });
            } else {
                // Handle the case at which Hierarchy is null
                //             TimerUtils.withTime("Time taken to read and populate hierarchyMetadata record ", () -> {
                List<HierarchyMetadata> lastParent = (List<HierarchyMetadata>) hierarchyMetadataService
                        .findByImmediateParent(hierarchyMetadata.getParent());
                if (lastParent != null) {
                    existingMetadata.addAll(lastParent);
                }
                //     });
                // Handle the case where it is a new Hierarchy
            }
        } else {
            existingMetadata.add(hierarchyMetadata);
        }
    }

    private void setHierarchyElement(HierarchyMetadata element, List<String> hierarchyusers,
                                     HierarchyMetadata hierarchyMetadata, List<HierarchyMetadata> existingMetadata,
                                     List<HierarchyMetadata> newMetadata, OutletDetails tempoutlet) {
        String hierarchy = element.getHierarchy();
        if (hierarchy != null) {
            List<String> tempList = new ArrayList<>(hierarchyusers);
            tempList.remove(tempList.size() - 1);
            tempList.add(hierarchy);
            String joinedHierarchy = StringUtils.join(tempList, " > ");

            HierarchyMetadata hm = hierarchyMetadataService.findByHierarchy(joinedHierarchy);
            if (hm != null && existingMetadata.stream().noneMatch(np -> np.getHierarchy().equals(joinedHierarchy))) {
                existingMetadata.add(hm);
            } else if (hm == null && newMetadata.stream().noneMatch(np -> np.getHierarchy().equals(joinedHierarchy))) {
                HierarchyMetadata tempHierarchyMetaData = new HierarchyMetadata();
                tempHierarchyMetaData.setHierarchy(joinedHierarchy);
                Location location = tempoutlet.getLocation();
                tempHierarchyMetaData.setLocationHierarchy((location == null) ? null : location.getLocationHierarchy());
                tempHierarchyMetaData.setLob(tempoutlet.getLob());
                newMetadata.add(tempHierarchyMetaData);
            }
        }
    }
}

