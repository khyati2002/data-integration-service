package com.salescode.dim.jooq.impl;

import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TempMasterMapping extends com.salescode.dim.jooq.generated.tables.pojos.TempMasterMapping implements Serializable {

    //private String userloginid;

    private String parent;

    private String feature;

//    public String getUserLoginId() {
//        return userloginid;
//    }
//
//    public void setUserLoginId(String userLoginId) {
//        this.userloginid = userLoginId;
//    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        if (!super.equals(o)) return false;
//        TempMasterMapping that = (TempMasterMapping) o;
//        return Objects.equals(userloginid, that.userloginid) && Objects.equals(parent, that.parent) && Objects.equals(feature, that.feature);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(super.hashCode(), userloginid, parent, feature);
//    }
}