/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.services;

import com.salescode.jooq.generated.tables.pojos.CkChannelHierarchyMetadata;
import com.salescode.jooq.generated.tables.pojos.CkDivision;
import com.salescode.jooq.generated.tables.pojos.CkHierarchyMetadata;
import com.salescode.jooq.generated.tables.pojos.CkOutletDetails;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.jooq.generated.tables.CkHierarchyMetadata.CK_HIERARCHY_METADATA;
import static com.salescode.jooq.generated.tables.CkOutletDetails.CK_OUTLET_DETAILS;
import static com.salescode.jooq.generated.tables.CkOutletDetailsHierarchymetadata.CK_OUTLET_DETAILS_HIERARCHYMETADATA;
import static com.salescode.jooq.generated.tables.CkUser.CK_USER;
import static com.salescode.jooq.generated.tables.CkUserdesignation.CK_USERDESIGNATION;

/**
 * The class ChannelHierarchyMetaDataService.
 *
 * @author Manish Srivastava
 * @since July 2020
 */
@Service
public class ChannelHierarchyMetaDataService {

    private static final String DESIGNATION = "designation";
    private final DSLContext dsl;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private DivisionService divisionService;


    public ChannelHierarchyMetaDataService(DSLContext dsl, DivisionService divisionService) {
        this.dsl = dsl;
        this.divisionService = divisionService;
    }

    //
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

    public Collection<CkChannelHierarchyMetadata> getOutletChannelHierarchy(CkOutletDetails outlet) {
        if (divisionService.isChannelDivisionPresent()) {
            List<CkDivision> divisions = (List<CkDivision>) divisionService.findByChannelDivisionOrderByLevelAsc();
            List<CkHierarchyMetadata> parents = getImmediateParent(outlet);
            return getData(parents, divisions, outlet.getOutletcode());
        }
        return List.of();
    }
    

    /// /	public Collection<ChannelHierarchyMetaData> getUserChannelHierarchy(User user) {
    /// /		if(divisionService.isChannelDivisionPresent()) {
    /// /			List<Division> divisions= (List<Division>)divisionService.findByChannelDivisionOrderByLevelAsc();
    /// /			Collection<HierarchyMetaData> parents= hierarchyMetaDataService.findByImmediateParent(user.getLoginId());
    /// /			return getData(parents,divisions,user.getLoginId());
    /// /		}
    /// /		return List.of();
    /// /	}

    private Comparator<Map<String, String>> findComparator(Collection<CkDivision> divisions) {
        return (o1, o2) -> {
            if (o1.get(DESIGNATION) != null && o2.get(DESIGNATION) != null) {
                Optional<Integer> levelo1 = divisions.stream().filter(element -> element.getDivisionName().equalsIgnoreCase(o1.get(DESIGNATION))).map(CkDivision::getLevel).findFirst();
                Optional<Integer> levelo2 = divisions.stream().filter(element -> element.getDivisionName().equalsIgnoreCase(o2.get(DESIGNATION))).map(CkDivision::getLevel).findFirst();
                int val1 = levelo1.orElse(0);
                int val2 = levelo2.orElse(0);
                if (val1 > val2)
                    return 1;
                else if (val1 < val2)
                    return -1;
                else return 0;
            }
            return 2;
        };
    }

    private void addDataInDataset(SortedSet<Map<String, String>> sortedset, Set<CkChannelHierarchyMetadata> dataset) {
        Iterator<Map<String, String>> itr = sortedset.iterator();
        CkChannelHierarchyMetadata temp = new CkChannelHierarchyMetadata();
        while (itr.hasNext()) {
            Map<String, String> mapdata = itr.next();
            if (mapdata.get(DESIGNATION) != null && divisionService.isChannelDivision(mapdata.get(DESIGNATION))) {
                checkAndInsertInChannelHierarchy(temp, mapdata.get("loginid"), mapdata.get("name"));
            }
        }
        if (temp.getLevel1supplier() != null) {
            dataset.add(temp);
        }
    }

    //
    public List<Map<String, String>> findData(String hierarchyUserList) {
        List<Map<String, Object>> result = dsl.selectDistinct(
                        CK_USER.LOGINID.as("loginid"),
                        CK_USERDESIGNATION.DESIGNATION.as("designation"),
                        CK_USER.NAME.as("name")
                )
                .from(CK_USER)
                .leftJoin(CK_USERDESIGNATION)
                .on(CK_USER.LOGINID.eq(CK_USERDESIGNATION.LOGIN_ID))
                .where(CK_USER.LOGINID.in(hierarchyUserList))
                .and(CK_USERDESIGNATION.DESIGNATION.isNotNull())
                .fetchMaps();

        return result.stream()
                .map(row -> row.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> String.valueOf(entry.getValue()))) // Convert each value to String
                )
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Collection<CkChannelHierarchyMetadata> getData(Collection<CkHierarchyMetadata> parents, Collection<CkDivision> divisions, String ignoreLoginId) {
        Set<CkChannelHierarchyMetadata> dataset = new HashSet<>();
        if (isParentEmpty(parents)) {
            logger.error("Cannot find channel hierarchy as immediate parent found null or empty");
        } else {
            for (CkHierarchyMetadata parent : parents) {
                String temphierarchy = parent.getHierarchy();
                if (temphierarchy == null) {
                    //	throw new EmptyParentHierarchyException(parent.getImmediateParent());
                }
                List<String> hierarchyLoginId = Arrays.asList(temphierarchy.split(" > "));
                String hierarchyuserlist = "'" + StringUtils.join(hierarchyLoginId, "','") + "'";


                List<Map<String, String>> data = findData(hierarchyuserlist);
                if (isDataNotEmpty(data)) {
                    if (findSingleSupplierInHierarchyMetadata()) {
                        getSingleSuppPerHierarchyMetadata(data, hierarchyLoginId, ignoreLoginId, dataset);
                    } else {
                        Comparator<Map<String, String>> comparator = findComparator(divisions);
                        SortedSet<Map<String, String>> sortedset = new TreeSet<>(comparator);
                        sortedset.addAll(data);
                        addDataInDataset(sortedset, dataset);
                    }
                }
            }
        }
        return dataset;
    }

    //
    private boolean isDataNotEmpty(List<Map<String, String>> data) {
        return data != null && !data.isEmpty();
    }

    //
    private boolean isParentEmpty(Collection<CkHierarchyMetadata> parents) {
        return parents == null || parents.isEmpty();
    }











    private void getSingleSuppPerHierarchyMetadata(List<Map<String, String>> data, List<String> hierarchyLoginId, String ignoreLoginId, Set<CkChannelHierarchyMetadata> dataset) {
        Map<String, List<Map<String, String>>> maploginIdDesignation = data.stream()
                .collect(Collectors.groupingBy(m -> m.get("loginid")));
        Set<String> supp = new HashSet<>();
        for (String loginId : hierarchyLoginId) {
            if (!loginId.equals(ignoreLoginId) && maploginIdDesignation.containsKey(loginId) && isSupplier(maploginIdDesignation.get(loginId))) {
                supp.add(loginId);
                break;
            }
        }
        supp.forEach(s -> {
            CkChannelHierarchyMetadata temp = new CkChannelHierarchyMetadata();
            temp.setLevel1supplier(s);
            temp.setLevel1supplierName(maploginIdDesignation.get(s).get(0).get("name"));
            dataset.add(temp);
        });
    }

    private boolean isSupplier(List<Map<String, String>> list) {
        return list.stream().anyMatch(m -> m.get(DESIGNATION) != null && divisionService.isChannelDivision(m.get(DESIGNATION)));
    }

    public boolean findSingleSupplierInHierarchyMetadata() {

        return false;
    }

    //
    public void checkAndInsertInChannelHierarchy(CkChannelHierarchyMetadata metadata, String loginid, String name) {
        if (metadata.getLevel1supplier() == null) {
            metadata.setLevel1supplier(loginid);
            metadata.setLevel1supplierName(name);
        } else if (metadata.getLevel2supplier() == null) {
            metadata.setLevel2supplier(loginid);
            metadata.setLevel2supplierName(name);
        } else if (metadata.getLevel3supplier() == null) {
            metadata.setLevel3supplier(loginid);
            metadata.setLevel3supplierName(name);
        }
    }

}
