package com.salescode.dim.jooq.impl;

import com.salescode.dim.jooq.generated.tables.pojos.GenericObject;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSetter;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class GenericEntity extends GenericObject {

	// No shadow field here!
	// The parent class already defines `LocalDateTime date` with getters & setters.

	@JsonSetter("date")
	public void setDateFromJson(String date) {
		if (date == null) {
			super.setDate(null);
			return;
		}

		ZonedDateTime zonedDateTime = ZonedDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME);
		LocalDateTime localDateTime = zonedDateTime.toLocalDateTime();

		// Use the parent class setter (no overloads!)
		super.setDate(localDateTime);
	}
}