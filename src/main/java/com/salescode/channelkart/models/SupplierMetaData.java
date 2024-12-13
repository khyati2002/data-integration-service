/**
 * 
 */
package com.salescode.channelkart.models;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;

/**
 * @author Snehadeep Vikram
 *
 */
@Entity
@Table(name="ck_supplier_metadata")
public class SupplierMetaData extends CommonDataModel {
	private static final long serialVersionUID = 1L;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(referencedColumnName="loginid")
	@JsonBackReference
	private User user;
	private Integer min;
	private Integer max;
	private String level;
	private String type;
	/**
	 * @return the user
	 */
	public User getUser() {
		return user;
	}
	/**
	 * @param user the user to set
	 */
	public void setUser(User user) {
		this.user = user;
	}
	/**
	 * @return the min
	 */
	public Integer getMin() {
		return min;
	}
	/**
	 * @param min the min to set
	 */
	public void setMin(Integer min) {
		this.min = min;
	}
	/**
	 * @return the max
	 */
	public Integer getMax() {
		return max;
	}
	/**
	 * @param max the max to set
	 */
	public void setMax(Integer max) {
		this.max = max;
	}
	/**
	 * @return the level
	 */
	public String getLevel() {
		return level;
	}
	/**
	 * @param level the level to set
	 */
	public void setLevel(String level) {
		this.level = level;
	}
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	
//	@Override
//    public boolean equals(Object obj) {
//        if (this == obj)
//            return true;
//        if (obj == null)
//            return false;
//        if (getClass() != obj.getClass())
//            return false;
//        SupplierMetaData other = (SupplierMetaData) obj;
//        return checkEquals(getId(),other.getId()) && checkEquals((user!=null && user.getLoginId()!=null)?user.getLoginId():null,other.getUser()!=null?other.getUser().getLoginId():null)
//        		&&  checkEquals(min,other.getMin()) && checkEquals(max,other.getMax()) && checkEquals(level,other.getLevel())   && checkEquals(type,other.getType());
//    }
	
	@Override
	public int hashCode() {
		return getId()!=null?getId().hashCode():super.hashCode();
	}
	
}
