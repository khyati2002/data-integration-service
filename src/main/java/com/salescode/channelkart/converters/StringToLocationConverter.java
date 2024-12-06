package com.salescode.channelkart.converters;


import com.fasterxml.jackson.databind.util.StdConverter;
import com.salescode.channelkart.services.LocationService;
import com.salescode.channelkart.services.ServiceLocator;
import com.salescode.jooq.generated.tables.pojos.CkLocation;
import org.springframework.stereotype.Component;

@Component
public class StringToLocationConverter extends StdConverter<String, CkLocation> {

	@Override
	public CkLocation convert(String value) {
		if(value != null) {
			LocationService service=(LocationService) ServiceLocator.lookup(CkLocation.class);
			return service.findByLocationHierarchy(value);
		}
		return null;
	}
}


