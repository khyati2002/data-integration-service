package com.salescode.channelkart.converters;


import com.fasterxml.jackson.databind.util.StdConverter;
import com.salescode.channelkart.models.HierarchyMetaData;
import com.salescode.channelkart.models.User;
import com.salescode.channelkart.services.HierarchyMetaDataService;
import com.salescode.channelkart.services.ServiceLocator;
import com.salescode.channelkart.services.UserService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class StringToHierarchyMetaDataConverter extends StdConverter<String,List<HierarchyMetaData>> {
	

	@Override
	public List<HierarchyMetaData> convert(String value) {
		List<HierarchyMetaData> userList = new ArrayList<>();
		if(value != null) {
			String[] splitValues = value.split(",");
			String[] loginids= new HashSet<>(Arrays.asList(splitValues)).toArray(new String[0]);
			UserService service=(UserService) ServiceLocator.lookup(User.class);
			HierarchyMetaDataService hierarchyMetaDataService =(HierarchyMetaDataService) ServiceLocator.lookup(HierarchyMetaData.class);
			for(String loginid:loginids){
				List<HierarchyMetaData> hierarchytemp = (List<HierarchyMetaData>) hierarchyMetaDataService.findByImmediateParent(loginid);
				List<HierarchyMetaData> hierarchyNew;
				if(hierarchytemp!=null && !hierarchytemp.isEmpty()) {
					hierarchyNew= hierarchytemp;
				}else {
					User user=service.findByLoginId(loginid);
					hierarchyNew= new ArrayList<>();
					HierarchyMetaData hm= new HierarchyMetaData();
					if(user != null) {
						hm.setImmediateParent(user.getLoginId());
						hm.setHierarchy(user.getHierarchy());
						hm.setLocationHierarchy((user.getLocationHierarchy() == null)?null:user.getLocationHierarchy().getLocationHierarchy());
					}else {
						hm.setImmediateParent(loginid);
					}
					hierarchyNew.add(hm);
				}
				userList.addAll(hierarchyNew);
			}
			return userList;
		}
		return List.of();
	}

}
