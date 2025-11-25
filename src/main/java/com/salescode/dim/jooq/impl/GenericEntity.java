package com.salescode.dim.jooq.impl;

import com.salescode.dim.jooq.generated.tables.pojos.GenericObject;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSetter;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class GenericEntity extends GenericObject {



	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
	private LocalDateTime date;



}