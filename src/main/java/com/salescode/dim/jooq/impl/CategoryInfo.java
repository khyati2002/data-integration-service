package com.salescode.dim.jooq.impl;

import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryInfo extends com.salescode.dim.jooq.generated.tables.pojos.CategoryInfo implements Serializable {

    private String categoryCode;

    private String categoryValue;

    private String newDescription;

    private String oldDescription;

    private String feature;

    private String name;

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }


    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getCategoryValue() {
        return categoryValue;
    }

    public void setCategoryValue(String categoryValue) {
        this.categoryValue = categoryValue;
    }

    public String getNewDescription() {
        return newDescription;
    }

    public void setNewDescription(String newDescription) {
        this.newDescription = newDescription;
    }

    public String getOldDescription() {
        return oldDescription;
    }

    public void setOldDescription(String oldDescription) {
        this.oldDescription = oldDescription;
    }

    public String getName(){ return name;}

    public void setName(String name) { this.name = name; }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CategoryInfo that = (CategoryInfo) o;
        return Objects.equals(categoryCode, that.categoryCode) && Objects.equals(categoryValue, that.categoryValue) && Objects.equals(newDescription, that.newDescription) && Objects.equals(oldDescription, that.oldDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), categoryCode, categoryValue, newDescription, oldDescription);
    }

}
