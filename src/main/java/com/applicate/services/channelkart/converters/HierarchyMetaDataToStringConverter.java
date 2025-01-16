package com.applicate.services.channelkart.converters;


import com.fasterxml.jackson.databind.util.StdConverter;
import com.applicate.services.channelkart.models.HierarchyMetaData;

import java.util.List;

public class HierarchyMetaDataToStringConverter extends StdConverter<List<HierarchyMetaData>,String> {

	
	@Override
	public String convert(List<HierarchyMetaData> hierarchyMetaDataList) {
		StringBuilder immediateParent = new StringBuilder("");
		if(hierarchyMetaDataList!=null && !hierarchyMetaDataList.isEmpty() ) {
			for(int i=0;i<hierarchyMetaDataList.size();i++) {
				HierarchyMetaData hierarchy = hierarchyMetaDataList.get(i);
				if(i!=0) {
					immediateParent.append(",");
				}
				immediateParent.append( hierarchy.getImmediateParent());
			}
		}
		return immediateParent.toString();
	}
}
