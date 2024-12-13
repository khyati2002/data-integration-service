package com.salescode.channelkart.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.models.User;
import com.salescode.channelkart.models.SupplierMetaData;
import com.salescode.channelkart.utils.CollectionUtils;

import java.util.Date;
import java.util.List;

public class SupplierInfo extends CommonDataModel {

    private String name;
    private String mobile;
    private String email;
    private String loginId;

    public String getLoginId() {
        return loginId;
    }

    public Date getNextPjpDate() {
        return nextPjpDate;
    }

    private List<SupplierMetaData> supplierMetaData;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date nextPjpDate;

    public SupplierInfo(){

    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public void setNextPjpDate(Date nextPjpDate) {
        this.nextPjpDate = nextPjpDate;
    }

    public SupplierInfo(User user){
        this.loginId = user.getLoginId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.mobile = user.getMobile();
        this.supplierMetaData = user.getSupplierMetaData();
		if (!CollectionUtils.isEmptyOrNull(user.getSupplierMetaData())) {
			SupplierMetaData metadata = user.getSupplierMetaData().get(0);
			if (metadata != null) {
				super.setId(metadata.getId());
			}
		}    
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<SupplierMetaData> getSupplierMetaData() {
        return supplierMetaData;
    }

    public void setSupplierMetaData(List<SupplierMetaData> supplierMetaData) {
        this.supplierMetaData = supplierMetaData;
    }
}
