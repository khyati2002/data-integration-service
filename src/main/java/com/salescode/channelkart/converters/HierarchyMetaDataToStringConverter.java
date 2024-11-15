package com.salescode.channelkart.converters;


import com.fasterxml.jackson.databind.util.StdConverter;
import com.salescode.jooq.generated.tables.pojos.CkHierarchyMetadata;


import java.util.List;

public class HierarchyMetaDataToStringConverter extends StdConverter<List<CkHierarchyMetadata>,String> {


	@Override
	public String convert(List<CkHierarchyMetadata> hierarchyMetaDataList) {
		StringBuilder immediateParent = new StringBuilder("");
		if(hierarchyMetaDataList!=null && !hierarchyMetaDataList.isEmpty() ) {
			for(int i=0;i<hierarchyMetaDataList.size();i++) {
				CkHierarchyMetadata hierarchy = hierarchyMetaDataList.get(i);
				if(i!=0) {
					immediateParent.append(",");
				}
				immediateParent.append( hierarchy.getParent());
			}
		}
		return immediateParent.toString();
	}
}
