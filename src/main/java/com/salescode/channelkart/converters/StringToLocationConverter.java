package com.salescode.channelkart.converters;


import com.fasterxml.jackson.databind.util.StdConverter;
import com.salescode.channelkart.models.Location;
import com.salescode.channelkart.services.LocationService;
import com.salescode.channelkart.services.ServiceLocator;

public class StringToLocationConverter extends StdConverter<String, Location> {

	@Override
	public Location convert(String value) {
		if(value != null) {
			LocationService service=(LocationService) ServiceLocator.lookup(Location.class);
			return service.findByLocationHierarchy(value);
		}
		return null;
	}
}


