/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.services;


import com.salescode.channelkart.models.ChannelHierarchyMetaData;
import com.salescode.channelkart.models.Division;
import com.salescode.channelkart.models.HierarchyMetaData;
import com.salescode.channelkart.models.OutletDetails;
import com.salescode.channelkart.utils.EntityUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;



/**
 * The class ChannelHierarchyMetaDataService.
 *
 * @author Manish Srivastava
 * @since July 2020
 */
@Service
public class ChannelHierarchyMetaDataService {

    private static final String DESIGNATION = "designation";
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private DivisionService divisionService;


    public ChannelHierarchyMetaDataService( DivisionService divisionService) {
        this.divisionService = divisionService;
    }


    public Collection<ChannelHierarchyMetaData> getOutletChannelHierarchy(OutletDetails outlet) {
        if(divisionService.isChannelDivisionPresent()) {
            List<Division> divisions= (List<Division>)divisionService.findByChannelDivisionOrderByLevelAsc();
            List<HierarchyMetaData> parents= outlet.getImmediateParent();
            return getData(parents,divisions,outlet.getOutletCode());
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

    private Comparator<Map<String, String>> findComparator(Collection<Division> divisions) {
        return (o1, o2) -> {
            if (o1.get(DESIGNATION) != null && o2.get(DESIGNATION) != null) {
                Optional<Integer> levelo1 = divisions.stream().filter(element -> element.getDivisionName().equalsIgnoreCase(o1.get(DESIGNATION))).map(Division::getLevel).findFirst();
                Optional<Integer> levelo2 = divisions.stream().filter(element -> element.getDivisionName().equalsIgnoreCase(o2.get(DESIGNATION))).map(Division::getLevel).findFirst();
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

    private void addDataInDataset(SortedSet<Map<String, String>> sortedset, Set<ChannelHierarchyMetaData> dataset) {
        Iterator<Map<String, String>> itr = sortedset.iterator();
        ChannelHierarchyMetaData temp = new ChannelHierarchyMetaData();
        while (itr.hasNext()) {
            Map<String, String> mapdata = itr.next();
            if (mapdata.get(DESIGNATION) != null && divisionService.isChannelDivision(mapdata.get(DESIGNATION))) {
                checkAndInsertInChannelHierarchy(temp, mapdata.get("loginid"), mapdata.get("name"));
            }
        }
        if (temp.getLevel1Supplier() != null) {
            dataset.add(temp);
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<ChannelHierarchyMetaData> getData(Collection<HierarchyMetaData> parents, Collection<Division> divisions, String ignoreLoginId){
        Set<ChannelHierarchyMetaData> dataset= new HashSet<>();
        if(isParentEmpty(parents)) {
            logger.error("Cannot find channel hierarchy as immediate parent found null or empty");
        }else {
            for(HierarchyMetaData parent: parents) {
                String temphierarchy= parent.getHierarchy();
                if(temphierarchy==null) {
                  //  throw new EmptyParentHierarchyException(parent.getImmediateParent());
                }
                List<String> hierarchyLoginId = Arrays.asList(temphierarchy.split(" > "));
                String hierarchyuserlist= "'"+ StringUtils.join(hierarchyLoginId,"','")+"'";
                List<Map<String,String>> data=  (List<Map<String,String>>) EntityUtils.get().findDataByQuery(Map.class,
                        "select distinct u.loginid as loginid, d.designation as designation, u.name from ck_user u left join ck_userdesignation d on u.loginid=d.login_id where u.loginid in ("+hierarchyuserlist+") and d.designation is not null",true);
                if(isDataNotEmpty(data)) {
                    if(findSingleSupplierInHierarchyMetadata()) {
                        getSingleSuppPerHierarchyMetadata(data,hierarchyLoginId,ignoreLoginId,dataset);
                    }else {
                        Comparator<Map<String,String>> comparator = findComparator(divisions);
                        SortedSet<Map<String,String>> sortedset= new TreeSet<>(comparator);
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
    private boolean isParentEmpty(Collection<HierarchyMetaData> parents) {
        return parents == null || parents.isEmpty();
    }











    private void getSingleSuppPerHierarchyMetadata(List<Map<String, String>> data, List<String> hierarchyLoginId, String ignoreLoginId, Set<ChannelHierarchyMetaData> dataset) {
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
            ChannelHierarchyMetaData temp = new ChannelHierarchyMetaData();
            temp.setLevel1Supplier(s);
            temp.setLevel1SupplierName(maploginIdDesignation.get(s).get(0).get("name"));
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
    public void checkAndInsertInChannelHierarchy(ChannelHierarchyMetaData metadata, String loginid, String name) {
        if (metadata.getLevel1Supplier() == null) {
            metadata.setLevel1Supplier(loginid);
            metadata.setLevel1SupplierName(name);
        } else if (metadata.getLevel2Supplier() == null) {
            metadata.setLevel2Supplier(loginid);
            metadata.setLevel2SupplierName(name);
        } else if (metadata.getLevel3Supplier() == null) {
            metadata.setLevel3Supplier(loginid);
            metadata.setLevel3SupplierName(name);
        }
    }

}
