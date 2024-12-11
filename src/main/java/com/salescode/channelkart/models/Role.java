package com.salescode.channelkart.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name="ck_auth_role")
public class Role extends CommonDataModel{
	private static final long serialVersionUID = 1L;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "name", length = 50, unique = true)
    @NotNull
    private String name;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Role)) return false;
		if (!super.equals(o)) return false;

		Role role = (Role) o;

		return name.equals(role.name);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + name.hashCode();
		return result;
	}


	@Override
	public String toString() {
		return "Role [name=" + name + "]";
	}
	
	
}
