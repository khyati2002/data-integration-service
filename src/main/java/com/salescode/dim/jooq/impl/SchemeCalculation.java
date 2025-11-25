package com.salescode.dim.jooq.impl;

import com.salescode.dim.jooq.generated.tables.pojos.SchemeFreeproductinfo;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeMustBuyGroup;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class SchemeCalculation extends com.salescode.dim.jooq.generated.tables.pojos.SchemeCalculation implements Serializable {
    private List<SchemeMustBuyGroup> schemeMustBuyGroupList;
    private List<SchemeFreeproductinfo>schemeFreeproductinfoList;
}
