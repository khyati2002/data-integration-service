package com.salescode.dim.jooq.impl;

import com.salescode.dim.jooq.generated.tables.pojos.SchemeLocationBifurcations;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeOutletBifurcations;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeProductBifurcations;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class SchemeDefination extends com.salescode.dim.jooq.generated.tables.pojos.SchemeDefination implements Serializable {
    private List<SchemeProductBifurcations> schemeProductBifurcationsList;
    private List<SchemeOutletBifurcations> schemeOutletBifurcationsList;
    private List<SchemeCalculation> schemeCalculation;
    private List<SchemeLocationBifurcations> schemeLocationBifurcationsList;
}
