package com.salescode.dim.jooq.impl;

import java.io.Serializable;
import java.util.Objects;

public class EntityParentMapping extends com.salescode.dim.jooq.generated.tables.pojos.EntityParentMapping implements Serializable {
    private static final long serialVersionUID = -8805009398530115393L;
    private String designation;
    private String user;
    private String parent;
    private String parentDesignation;
    private String prodAuthCode;

    public String getDesignation() {
        return this.designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getParent() {
        return this.parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getParentDesignation() {
        return this.parentDesignation;
    }

    public void setParentDesignation(String parentDesignation) {
        this.parentDesignation = parentDesignation;
    }

    public String getUser() {
        return this.user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getProdAuthCode() {
        return this.prodAuthCode;
    }

    public void setProdAuthCode(String prodAuthCode) {
        this.prodAuthCode = prodAuthCode;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            if (!super.equals(o)) {
                return false;
            } else {
                EntityParentMapping that = (EntityParentMapping)o;
                return Objects.equals(this.designation, that.designation) && Objects.equals(this.user, that.user) && Objects.equals(this.parent, that.parent) && Objects.equals(this.parentDesignation, that.parentDesignation) && Objects.equals(this.prodAuthCode, that.prodAuthCode);
            }
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{super.hashCode(), this.designation, this.user, this.parent, this.parentDesignation, this.prodAuthCode});
    }
}

