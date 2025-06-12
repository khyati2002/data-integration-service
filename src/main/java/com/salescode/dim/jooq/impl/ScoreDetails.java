package com.salescode.dim.jooq.impl;

import com.applicate.services.channelkart.converters.LocationToStringConverter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonGetter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSetter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ScoreDetails extends com.salescode.dim.jooq.generated.tables.pojos.ScoreDetails implements Serializable {

	private Location locationHierarchy;

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
		LocationToStringConverter locationToStringConverter = new LocationToStringConverter();
		String loc = locationToStringConverter.convert(location);
		setLocationHierarchy(loc);
	}
	@JsonSetter("startDate")
	public void setStartDate(String date){
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime localDateTime = LocalDateTime.parse(date, formatter);
		setStartDate(localDateTime);
	}
	@JsonSetter("endDate")
	public void setEndDate(String date){
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime localDateTime = LocalDateTime.parse(date, formatter);
		setEndDate(localDateTime);
	}


}

