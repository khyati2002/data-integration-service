package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.salescode.dim.jooq.generated.tables.pojos.ChannelHierarchyMetadata;
import com.salescode.dim.jooq.generated.tables.pojos.Division;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.OutletDetails;
import org.jooq.DSLContext;

import java.util.*;

public class ChannelHierarchyMetadataService{

    private final DSLContext dsl;

    public ChannelHierarchyMetadataService(DSLContext dsl) {
      this.dsl = dsl;
    }

    public Collection<ChannelHierarchyMetadata> getOutletChannelHierarchy(OutletDetails outlet) {
//        if(divisionService.isChannelDivisionPresent()) {
//            List<Division> divisions= (List<Division>)divisionService.findByChannelDivisionOrderByLevelAsc();
//            List<HierarchyMetadata> parents= outlet.getImmediateParent();
//            return getData(parents,divisions,outlet.getOutletcode());
//        }
        return List.of();
    }

//    private Collection<ChannelHierarchyMetaData> getData(Collection<HierarchyMetaData> parents, Collection<Division> divisions, String ignoreLoginId){
//        Set<ChannelHierarchyMetaData> dataset= new HashSet<>();
//        if(isParentEmpty(parents)) {
//            logger.error("Cannot find channel hierarchy as immediate parent found null or empty");
//        }else {
//            for(HierarchyMetaData parent: parents) {
//                String temphierarchy= parent.getHierarchy();
//                if(temphierarchy==null) {
//                    throw new EmptyParentHierarchyException(parent.getImmediateParent());
//                }
//                List<String> hierarchyLoginId = Arrays.asList(temphierarchy.split(" > "));
//                String hierarchyuserlist= "'"+StringUtils.join(hierarchyLoginId,"','")+"'";
//                List<Map<String,String>> data=  (List<Map<String,String>>) EntityUtils.get().findDataByQuery(Map.class,
//                        "select distinct u.loginid as loginid, d.designation as designation, u.name from ck_user u left join ck_userdesignation d on u.loginid=d.login_id where u.loginid in ("+hierarchyuserlist+") and d.designation is not null",true);
//                if(isDataNotEmpty(data)) {
//                    if(findSingleSupplierInHierarchyMetadata()) {
//                        getSingleSuppPerHierarchyMetadata(data,hierarchyLoginId,ignoreLoginId,dataset);
//                    }else {
//                        Comparator<Map<String,String>> comparator = findComparator(divisions);
//                        SortedSet<Map<String,String>> sortedset= new TreeSet<>(comparator);
//                        sortedset.addAll(data);
//                        addDataInDataset(sortedset, dataset);
//                    }
//                }
//            }
//        }
//        return dataset;
//    }


}

