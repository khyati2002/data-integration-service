package com.salescode.dim.jooq.impl;

import com.applicate.services.channelkart.services.LocationService;
import com.applicate.services.channelkart.services.ServiceLocator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonGetter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSetter;

import java.io.Serializable;
import java.sql.Array;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ScoreDetails extends com.salescode.dim.jooq.generated.tables.pojos.ScoreDetails implements Serializable {

	private Location locationHierarchy;

	public ScoreDetails() {
		super();
	}

	private ScoreDetails(com.salescode.dim.jooq.generated.tables.pojos.ScoreDetails score) {
		super(score);
	}

	public static ScoreDetails of(com.salescode.dim.jooq.generated.tables.pojos.ScoreDetails score) {
		if (score == null) {
			return null;
		}
		return new ScoreDetails(score);
	}

	@JsonGetter("loginId")
	public String getLoginId() {
		return getLoginid();
	}

	@JsonSetter("loginId")
	public void setLoginId(String loginId) {
		setLoginid(loginId);
	}

	@JsonGetter("outletCode")
	public String getOutletCode() {
		return getOutletcode();
	}

	@JsonSetter("outletCode")
	public void setOutletCode(String outletCode) {
		setOutletcode(outletCode);
	}

	public void setLocationHierarchy(Location location) {
		LocationService locationService = (LocationService) ServiceLocator.lookup(Location.class);
		List<Location> list = new ArrayList<>();
		list.add(location);
		Location loc = locationService.findLocationOrPersistLocation(list).get(0);
		setLocationHierarchy(loc.getLocationHierarchy());

	}

	@JsonSetter("startDate")
	public void setStartDate(String date) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime localDateTime = LocalDateTime.parse(date, formatter);
		ZonedDateTime istDateTime = localDateTime.atZone(ZoneId.of("Asia/Kolkata"));
		ZonedDateTime utcDateTime = istDateTime.withZoneSameInstant(ZoneOffset.UTC);
		setStartDate(utcDateTime.toLocalDateTime());
	}

	@JsonSetter("endDate")
	public void setEndDate(String date) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime localDateTime = LocalDateTime.parse(date, formatter);
		ZonedDateTime istDateTime = localDateTime.atZone(ZoneId.of("Asia/Kolkata"));
		ZonedDateTime utcDateTime = istDateTime.withZoneSameInstant(ZoneOffset.UTC);
		setEndDate(utcDateTime.toLocalDateTime());
	}


}

