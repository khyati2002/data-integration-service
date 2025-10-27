package com.salescode.dim.jooq.impl;

import com.applicate.services.channelkart.client.properties.PropertyDefinition;
import com.applicate.services.channelkart.client.properties.PropertyRegistry;
import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.services.CategoryInfoService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import javax.validation.constraints.Pattern;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryPJP extends com.salescode.dim.jooq.generated.tables.pojos.OutletDetails implements Serializable {
    private static final long serialVersionUID = 1L;
    private String outletCode;
    private String loginId;
    private String beat;
    private Date pjpDate;
    private JsonNode dayAndFrequency;
    private String month;
    private @Pattern(
            regexp = "(^[0-9]*$)"
    ) String year;
    private String designation;
    private String sourceCode;
    private String sourceName;
    private String destinationCode;
    private String destinationName;
    private String type;
    private String status;
    private String statusRemarks;
    private String approvedBy;
    private String pjpPlan;
    private String referenceNumber;
    private String turnAroundTime;
    private int sequence;
    private Boolean outletVisited;
    private String beatId;
    private String supplierId;
    private static PropertyRegistry propertyRegistry;

    public String getBeatId() {
        return this.beatId;
    }

    public void setBeatId(String beatId) {
        this.beatId = beatId;
    }

    public Boolean getOutletVisited() {
        return this.outletVisited;
    }

    public void setOutletVisited(Boolean outletVisited) {
        this.outletVisited = outletVisited;
    }

    public String getTurnAroundTime() {
        return this.turnAroundTime;
    }

    public void setTurnAroundTime(String turnAroundTime) {
        this.turnAroundTime = turnAroundTime;
    }

    public String getPjpPlan() {
        return this.pjpPlan;
    }

    public void setPjpPlan(String pjpPlan) {
        this.pjpPlan = pjpPlan;
    }

    public Date getPjpDate() {
        return this.pjpDate;
    }

    public void setPjpDate(Date pjpDate) {
        this.pjpDate = pjpDate;
    }

    public String getLoginId() {
        return this.loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getOutletCode() {
        return this.outletCode;
    }

    public void setOutletCode(String outletCode) {
        this.outletCode = outletCode;
    }

    public String getBeat() {
        return this.beat;
    }

    public void setBeat(String beat) {
        this.beat = beat;
    }

    public JsonNode getDayAndFrequency() {
        return this.dayAndFrequency;
    }

    public void setDayAndFrequency(JsonNode dayAndFrequency) {
        this.dayAndFrequency = dayAndFrequency;
    }

    public String getMonth() {
        return this.month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getYear() {
        return this.year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getDesignation() {
        return this.designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation != null ? designation.toLowerCase() : null;
    }

    public String getSourceCode() {
        return this.sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getSourceName() {
        return this.sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getDestinationCode() {
        return this.destinationCode;
    }

    public void setDestinationCode(String destinationCode) {
        this.destinationCode = destinationCode;
    }

    public String getDestinationName() {
        return this.destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusRemarks() {
        return this.statusRemarks;
    }

    public void setStatusRemarks(String statusRemarks) {
        this.statusRemarks = statusRemarks;
    }

    public String getApprovedBy() {
        return this.approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getReferenceNumber() {
        return this.referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getSupplierId() {
        return this.supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String toString() {
        String var10000 = this.getOutletCode();
        return "DeliveryPJP [outletCode=" + var10000 + ", supplier=" + this.getLoginId() + ", month=" + this.month + ", year=" + this.year + "]";
    }

    public int hashCode() {
        int prime = 31;
        int result = super.hashCode();
        result = 31 * result + (this.loginId == null ? 0 : this.loginId.hashCode());
        result = 31 * result + (this.month == null ? 0 : this.month.hashCode());
        result = 31 * result + (this.outletCode == null ? 0 : this.outletCode.hashCode());
        result = 31 * result + (this.pjpDate == null ? 0 : this.pjpDate.hashCode());
        result = 31 * result + (this.year == null ? 0 : this.year.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj != null && this.getClass() == obj.getClass()) {
            if (!super.equals(obj)) {
                return false;
            } else {
                DeliveryPJP other = (DeliveryPJP)obj;
                if (!Objects.equals(this.outletCode, other.outletCode)) {
                    return false;
                } else if (!Objects.equals(this.loginId, other.loginId)) {
                    return false;
                } else if (!Objects.equals(this.month, other.month)) {
                    return false;
                } else {
                    return !Objects.equals(this.pjpDate, other.pjpDate) ? false : Objects.equals(this.year, other.year);
                }
            }
        } else {
            return false;
        }
    }

    public int getSequence() {
        return this.sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    private static PropertyRegistry getPropertyRegistry() {
        if (propertyRegistry == null) {
            propertyRegistry = (PropertyRegistry) ServiceLocator.lookup(PropertyRegistry.class);
        }

        return propertyRegistry;
    }

    public boolean canHash() {
        return getPropertyRegistry().getAsBoolean(PropertyDefinition.HASH_CHECK_FOR_PJP_ENTITY);
    }
}

