package com.salescode.dim.jooq.impl;

import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupplierMetaData extends com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata {

	private static final long serialVersionUID = 1L;

	private User user;
	private Integer min;
	private Integer max;
	private String level;
	private String type;

	public SupplierMetaData() {
		super();
	}

	public SupplierMetaData(com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata supplierMetaData) {
		super(supplierMetaData);
	}

	public static SupplierMetaData of(com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata supplierMetaData) {
		return new SupplierMetaData(supplierMetaData);
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	@Override
	public Integer getMin() {
		return min;
	}

	@Override
	public void setMin(Integer min) {
		this.min = min;
	}

	@Override
	public Integer getMax() {
		return max;
	}

	@Override
	public void setMax(Integer max) {
		this.max = max;
	}

	@Override
	public String getLevel() {
		return level;
	}

	@Override
	public void setLevel(String level) {
		this.level = level;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public void setType(String type) {
		this.type = type;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SupplierMetaData other = (SupplierMetaData) obj;
		return checkEquals(getId(), other.getId())
				&& checkEquals((user != null && user.getLoginId() != null) ? user.getLoginId() : null,
				other.getUser() != null ? other.getUser().getLoginId() : null)
				&& checkEquals(min, other.getMin())
				&& checkEquals(max, other.getMax())
				&& checkEquals(level, other.getLevel())
				&& checkEquals(type, other.getType());
	}

	@Override
	public int hashCode() {
		return getId() != null ? getId().hashCode() : super.hashCode();
	}

	private boolean checkEquals(Object a, Object b) {
		return a == null ? b == null : a.equals(b);
	}
}
