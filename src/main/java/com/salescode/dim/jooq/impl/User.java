package com.salescode.dim.jooq.impl;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.jooq.generated.tables.pojos.AuthRole;
import com.salescode.dim.jooq.generated.tables.pojos.HierarchyMetadata;
import com.salescode.dim.jooq.generated.tables.pojos.Location;
import com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class User extends com.salescode.dim.jooq.generated.tables.pojos.User implements Serializable {

    private static final long serialVersionUID = 6364280713919356300L;

    private List<AuthRole> roles;
    private List<SupplierMetadata> supplierMetaData;
    private List<HierarchyMetadata> immediateParent;
    private Set<String> designation;
    @Getter(value = AccessLevel.NONE)
    private Location locationHierarchy;

    public void setHierarchy(Location hierarchy) {
        locationHierarchy = JSONUtils.getObjectMapper().convertValue(hierarchy, Location.class);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.deleteCharAt(sb.length() - 1);
        sb.append(", ").append(roles);
        sb.append(", ").append(supplierMetaData);
        sb.append(", ").append(immediateParent);
        sb.append(", ").append(designation);
        sb.append(", ").append(locationHierarchy);
        sb.append(")");
        return sb.toString();
      }
}


