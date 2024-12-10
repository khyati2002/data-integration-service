package com.salescode.jooq.impl;

import com.salescode.jooq.CkSupplierMetadata;
import com.salescode.jooq.generated.tables.pojos.CkAuthRole;
import com.salescode.jooq.generated.tables.pojos.CkHierarchyMetadata;
import com.salescode.jooq.generated.tables.pojos.CkLocation;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
public class CkUser extends com.salescode.jooq.generated.tables.pojos.CkUser{

    private List<CkAuthRole> roles;
    private List<CkSupplierMetadata> supplierMetaData;
    private List<CkHierarchyMetadata> immediateParent;
    private Set<String> designation;
    private CkLocation location;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.deleteCharAt(sb.length() - 1);
        sb.append(", ").append(roles);
        sb.append(", ").append(supplierMetaData);
        sb.append(", ").append(immediateParent);
        sb.append(", ").append(designation);
        sb.append(", ").append(location);
        sb.append(")");
        return sb.toString();
      }
}


