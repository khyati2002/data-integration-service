package com.applicate.services.channelkart.converters;


import com.fasterxml.jackson.databind.util.StdConverter;
import com.applicate.services.channelkart.models.Location;
import com.applicate.services.channelkart.services.LocationService;
import com.applicate.services.channelkart.services.ServiceLocator;

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


