package com.applicate.services.channelkart.services;

import com.salescode.dim.jooq.generated.tables.pojos.ChannelHierarchyMetadata;
import com.salescode.dim.jooq.generated.tables.pojos.Division;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.OutletDetails;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_USER;
import static com.salescode.dim.jooq.generated.Tables.CK_USERDESIGNATION;

public class ChannelHierarchyMetadataService {

    private final DSLContext dsl;
    private static DivisionService divisionService;

    private static final String DESIGNATION = "designation";

    public ChannelHierarchyMetadataService(DSLContext dsl) {
      this.dsl = dsl;
      divisionService = new DivisionService();
    }

    public Collection<ChannelHierarchyMetadata> getOutletChannelHierarchy(OutletDetails outlet) {
        if(divisionService.isChannelDivisionPresent()) {
            List<Division> divisions= (List<Division>)divisionService.findByChannelDivisionOrderByLevelAsc();
            List<HierarchyMetadata> parents= outlet.getImmediateParent();
            return getData(parents,divisions,outlet.getOutletcode());
        }
        return List.of();
    }

    private Collection<ChannelHierarchyMetadata> getData(Collection<HierarchyMetadata> parents, Collection<Division> divisions, String ignoreLoginId){
        Set<ChannelHierarchyMetadata> dataset= new HashSet<>();
        if(isParentEmpty(parents)) {
          //  logger.error("Cannot find channel hierarchy as immediate parent found null or empty");
        }else {
            for(HierarchyMetadata parent: parents) {
                String temphierarchy= parent.getHierarchy();
                if(temphierarchy==null) {
               //     throw new EmptyParentHierarchyException(parent.getImmediateParent());
                }
                List<String> hierarchyLoginId = Arrays.asList(temphierarchy.split(" > "));
                List<Map<String, String>> data = dsl
                        .selectDistinct(
                                CK_USER.LOGINID.as("loginid"),
                                CK_USERDESIGNATION.DESIGNATION.as("designation"),
                                CK_USER.NAME.as("name")
                        )
                        .from(CK_USER)
                        .leftJoin(CK_USERDESIGNATION)
                        .on(CK_USER.LOGINID.eq(CK_USERDESIGNATION.LOGIN_ID))
                        .where(CK_USER.LOGINID.in(hierarchyLoginId)
                                .and(CK_USERDESIGNATION.DESIGNATION.isNotNull()))
                        .fetchMaps()
                        .stream()
                        .map(row -> row.entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        e -> e.getValue() != null ? e.getValue().toString() : null
                                )))
                        .collect(Collectors.toList());

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

    private void addDataInDataset(SortedSet<Map<String,String>> sortedset, Set<ChannelHierarchyMetadata> dataset){
        Iterator<Map<String,String>> itr= sortedset.iterator();
        ChannelHierarchyMetadata temp= new ChannelHierarchyMetadata();
        while(itr.hasNext()) {
            Map<String,String> mapdata= itr.next();
            if(mapdata.get(DESIGNATION) != null && divisionService.isChannelDivision(mapdata.get(DESIGNATION))) {
                checkAndInsertInChannelHierarchy(temp,mapdata.get("loginid"),mapdata.get("name"));
            }
        }
        if(temp.getLevel1supplier() != null) {
            dataset.add(temp);
        }
    }

    public void checkAndInsertInChannelHierarchy(ChannelHierarchyMetadata metadata, String loginid, String name) {
        if(metadata.getLevel1supplier() == null){
            metadata.setLevel1supplier(loginid);
            metadata.setLevel1supplierName(name);
        }else if(metadata.getLevel2supplier() == null){
            metadata.setLevel2supplier(loginid);
            metadata.setLevel2supplierName(name);
        }else if(metadata.getLevel3supplier() == null){
            metadata.setLevel3supplier(loginid);
            metadata.setLevel3supplierName(name);
        }
    }

    private Comparator<Map<String,String>> findComparator(Collection<Division> divisions){
        return (o1, o2) -> {
            if(o1.get(DESIGNATION)!= null && o2.get(DESIGNATION)!=null) {
                Optional<Integer> levelo1= divisions.stream().filter(element->element.getDivisionName().equalsIgnoreCase(o1.get(DESIGNATION))).map(Division::getLevel).findFirst();
                Optional<Integer> levelo2= divisions.stream().filter(element->element.getDivisionName().equalsIgnoreCase(o2.get(DESIGNATION))).map(Division::getLevel).findFirst();
                int val1= levelo1.orElse(0);
                int val2= levelo2.orElse(0);
                if(val1>val2)
                    return 1;
                else if(val1<val2)
                    return -1;
                else return 0;
            }
            return 2;
        };
    }

    private void getSingleSuppPerHierarchyMetadata(List<Map<String, String>> data, List<String> hierarchyLoginId, String ignoreLoginId, Set<ChannelHierarchyMetadata> dataset) {
        Map<String, List<Map<String, String>>> maploginIdDesignation = data.stream()
                .collect(Collectors.groupingBy(m->m.get("loginid")));
        Set<String> supp = new HashSet<>();
        for(String loginId:hierarchyLoginId) {
            if(!loginId.equals(ignoreLoginId) && maploginIdDesignation.containsKey(loginId) && isSupplier(maploginIdDesignation.get(loginId))) {
                supp.add(loginId);
                break;
            }
        }
        supp.forEach(s->{
            ChannelHierarchyMetadata temp= new ChannelHierarchyMetadata();
            temp.setLevel1supplier(s);
            temp.setLevel1supplierName(maploginIdDesignation.get(s).get(0).get("name"));
            dataset.add(temp);
        });
    }

    private boolean isParentEmpty(Collection<HierarchyMetadata> parents) {
        return parents== null || parents.isEmpty();
    }

    private boolean isDataNotEmpty(List<Map<String, String>> data) {
        return data != null && !data.isEmpty();
    }

    public boolean findSingleSupplierInHierarchyMetadata() {
        return false;
    }

    private boolean isSupplier(List<Map<String, String>> list) {
        return list.stream().anyMatch(m->m.get(DESIGNATION) != null && divisionService.isChannelDivision(m.get(DESIGNATION)));
    }

}

