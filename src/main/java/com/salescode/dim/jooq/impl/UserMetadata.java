package com.salescode.dim.jooq.impl;


import com.applicate.services.channelkart.models.enums.UserMetadataType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;


import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * JOOQ-based UserMetadata model that mirrors the JPA version
 * with proper Jackson serialization support for geometry fields.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserMetadata extends com.salescode.dim.jooq.generated.tables.pojos.UserMetadata implements Serializable {

	private static final long serialVersionUID = 1L;
	private String value;
	private boolean isPrimary;

	private UserMetadataType type;

//	@JsonSerialize(using = LocationPointSerializer.class)
//	@JsonDeserialize(using = LocationPointDeserializer.class)
//	private Point location;

	private BigDecimal latitude;
	private BigDecimal longitude;

	@JsonProperty("loginId")
	public String getLoginId() {
		return getLoginid(); // calls generated POJO getter
	}

	@JsonProperty("loginId")
	public void setLoginId(String loginId) {
		setLoginid(loginId); // calls generated POJO setter
	}


	public String getType() {
		return this.type.toString();
	}
//	//public void setType(String type) {
//		this.type.toString() = type;
//	}
// Point getter

}
