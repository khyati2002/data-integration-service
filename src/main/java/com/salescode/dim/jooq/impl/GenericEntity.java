package com.salescode.dim.jooq.impl;

import com.salescode.dim.jooq.generated.tables.pojos.GenericObject;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSetter;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class GenericEntity extends GenericObject {

	private LocalDateTime date;

	@JsonSetter("date")
	public void setDate(String date) {


		ZonedDateTime zonedDateTime = ZonedDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME);
		LocalDateTime localDateTime = zonedDateTime.toLocalDateTime();
		setDate(localDateTime);

	}


}