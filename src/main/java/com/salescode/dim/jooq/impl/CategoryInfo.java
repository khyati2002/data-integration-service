package com.salescode.dim.jooq.impl;


import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;


@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoryInfo extends com.salescode.dim.jooq.generated.tables.pojos.CategoryInfo implements Serializable {

    public CategoryInfo() {
        super();
    }

    private CategoryInfo(CategoryInfo categoryInfo) {
        super(categoryInfo);
    }

    public static CategoryInfo of(CategoryInfo categoryInfo) {
        if (categoryInfo == null) {
            return null;
        }
        return new CategoryInfo(categoryInfo);
    }

}