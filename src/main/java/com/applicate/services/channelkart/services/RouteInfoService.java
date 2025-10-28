package com.applicate.services.channelkart.services;

import com.salescode.dim.jooq.impl.RouteInfo;
import com.applicate.services.channelkart.repository.RouteInfoRepository;
import org.springframework.stereotype.Service;



@Service
public class RouteInfoService extends AbstractCDMService<RouteInfo> {

	private final RouteInfoRepository routeInfoRepository;

	public RouteInfoService(RouteInfoRepository repository) {
		this.routeInfoRepository = repository;
	}

}
