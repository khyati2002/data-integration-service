package com.salescode.dim.jooq.impl;

import com.salescode.dim.jooq.generated.tables.pojos.AuthRole;
import com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSetter;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User extends com.salescode.dim.jooq.generated.tables.pojos.User implements Serializable {

    private static final long serialVersionUID = 6364280713919356300L;

    private transient List<AuthRole> roles;
    private List<SupplierMetadata> supplierMetaData;
    private List<HierarchyMetadata> immediateParent;
    private Set<String> designation;
    @Getter(value = AccessLevel.NONE)
    private Location locationHierarchy;

    public User(){
        super();
    }

    private User(com.salescode.dim.jooq.generated.tables.pojos.User user) {
        super(user);
    }

    public static User of(com.salescode.dim.jooq.generated.tables.pojos.User user) {
        if(user == null) {
            return null;
        }
        return new User(user);
    }

    public Location getLocation() {
        return locationHierarchy;
    }

    public String getLoginId() {
        return getLoginid();
    }

    @JsonSetter("loginId")
    public void setLoginId(String loginId) {
        setLoginid(loginId);
    }

    public String getUserAccountId() {
        return getUseraccountid();
    }

    @JsonSetter("userAccountId")
    public void setUserAccountId(String userAccountId) {
        setUseraccountid(userAccountId);
    }

    @JsonSetter("designation")
    public void setDesignation(Set<String> designation) {
        this.designation = (designation != null)
                ? designation.stream().map(String::toLowerCase).collect(java.util.stream.Collectors.toSet())
                : null;
    }

    public Set<String> getDesignation() {
        return designation;
    }

    @JsonSetter("supplierMetaData")
    public void setSupplierMetaData(List<SupplierMetadata> supplierMetaData) {
        this.supplierMetaData = supplierMetaData;
    }

    public List<SupplierMetadata> getSupplierMetaData() {
        return supplierMetaData;
    }

    @JsonSetter("immediateParent")
    public void setImmediateParent(List<HierarchyMetadata> immediateParent) {
        this.immediateParent = immediateParent;
    }

    public List<HierarchyMetadata> getImmediateParent() {
        return immediateParent;
    }

    public boolean hasDesignation(String toMatch) {
        return this.designation != null && this.designation.stream().anyMatch(d -> d.equals(toMatch));
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