package com.salescode.channelkart.converters;

import com.fasterxml.jackson.databind.util.StdConverter;
import com.salescode.channelkart.models.Location;

public class LocationToStringConverter extends StdConverter<Location,String> {

	@Override
	public String convert(Location value) {
		if(value != null) {
			return value.getLocationHierarchy();
		}
		return null;
	}

}
