package com.salescode.dim.jooq.impl;

import lombok.Getter;
import lombok.Setter;

@SuppressWarnings({"all", "unchecked", "rawtypes"})
@Getter
@Setter
public class HierarchyMetadata extends com.salescode.dim.jooq.generated.tables.pojos.HierarchyMetadata {
    private static final long serialVersionUID = -7546424289965519236L;

    private String immediateParent;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.deleteCharAt(sb.length() - 1);
        sb.append(", ").append(immediateParent);
        sb.append(")");
        return sb.toString();
    }

}
