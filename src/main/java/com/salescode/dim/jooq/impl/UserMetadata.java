package com.salescode.dim.jooq.impl;


import com.applicate.services.channelkart.converters.LocationPointDeserializer;
import com.applicate.services.channelkart.converters.LocationPointSerializer;

import com.applicate.services.channelkart.models.enums.UserMetadataType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

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

	private String loginId;
	private String value;
	private boolean isPrimary;

	private UserMetadataType type;

	@JsonSerialize(using = LocationPointSerializer.class)
	@JsonDeserialize(using = LocationPointDeserializer.class)
	private Point location;

	private BigDecimal latitude;
	private BigDecimal longitude;

	@Override
	public int hashCode() {
		return Objects.hash(loginId, type);
	}


	public String getType() {
		return this.type.toString();
	}
//	//public void setType(String type) {
//		this.type.toString() = type;
//	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		UserMetadata other = (UserMetadata) obj;
		return Objects.equals(loginId, other.loginId) &&
				Objects.equals(type, other.type);
	}
}
