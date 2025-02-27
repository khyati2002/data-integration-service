package com.applicate.services.channelkart.converters;

import com.fasterxml.jackson.databind.util.StdConverter;
import com.salescode.dim.jooq.impl.HierarchyMetadata;

import java.util.List;


public class HierarchyMetaDataToStringConverter extends StdConverter<List<HierarchyMetadata>,String> {


	@Override
	public String convert(List<HierarchyMetadata> hierarchyMetaDataList) {
		StringBuilder immediateParent = new StringBuilder("");
		if(hierarchyMetaDataList!=null && !hierarchyMetaDataList.isEmpty() ) {
			for(int i=0;i<hierarchyMetaDataList.size();i++) {
				HierarchyMetadata hierarchy = hierarchyMetaDataList.get(i);
				if(i!=0) {
					immediateParent.append(",");
				}
				immediateParent.append( hierarchy.getParent());
			}
		}
		return immediateParent.toString();
	}
}
