package com.salescode.channelkart.converters;


import com.fasterxml.jackson.databind.util.StdConverter;
import com.salescode.channelkart.services.LocationService;
import com.salescode.channelkart.services.ServiceLocator;
import com.salescode.jooq.generated.tables.pojos.CkLocation;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;

import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.salescode.jooq.generated.Tables.CK_LOCATION;

@Component
public class LocationToStringConverter extends StdConverter<CkLocation,String> {


	@Override
	public String convert(CkLocation value) {
		if(value != null && value.getLocationHierarchy() != null) {
			return value.getLocationHierarchy();
		}
		LocationService service=(LocationService) ServiceLocator.lookup(CkLocation.class);
		return service.findLocationString(value);

	}

}
