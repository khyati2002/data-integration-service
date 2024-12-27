package com.salescode.channelkart.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.salescode.channelkart.converters.ActiveStatusConverter;
import com.salescode.channelkart.converters.DateToClientTimeZoneStringConverter;
import com.salescode.channelkart.converters.JSONObjectConverter;
import com.salescode.channelkart.models.diff.Change;
import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.channelkart.utils.CdmDiffUtil;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

@Getter
@Setter
@MappedSuperclass
public class CommonDataModel implements Serializable {

    @Transient
    private boolean isCreate;

    @Getter
    @Setter
    @Transient
    @JsonIgnore
    private List<String> preProcessPipelineException;

    @Transient
    @JsonInclude()
    private Set<Change<Serializable>> changes;

    @Getter
    private transient CommonDataModel oldModel;


    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "com.salescode.channelkart.services.UUIDIdentifier")
    private String id;

    @Version
    private Integer version;

    @Convert(converter = ActiveStatusConverter.class)
    private ActiveStatus activeStatus;

    private String activeStatusReason;

    @JsonSerialize(converter = DateToClientTimeZoneStringConverter.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date creationTime;


    @JsonSerialize(converter = DateToClientTimeZoneStringConverter.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date lastModifiedTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String createdBy;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String modifiedBy;

    private String lob;

    private String source;

    @Column(columnDefinition = "json")
    @Convert(converter = JSONObjectConverter.class)
    private JsonNode extendedAttributes;

    @Column(unique = true, columnDefinition = "LONGTEXT")
    @JsonIgnore
    private String hash;

    public boolean isCreate() {
        return isCreate;
    }

    public void setCreate(boolean create) {
        isCreate = create;
    }

    @JsonGetter
    public Set<Change<Serializable>> getChanges() {
        if (this.changes == null) {
            this.changes = findChanges();
        }
        return this.changes;
    }

    @JsonSetter
    public void setChanges(Set<Change<Serializable>> changes) {
        this.changes = changes;
    }

    public Set<Change<Serializable>> findChanges() {
        return oldModel == null ? Collections.emptySet() : CdmDiffUtil.getChanges(this, this.getOldModel());
    }


    public void addPreProcessPipelineException(String stackTrace) {
        if (preProcessPipelineException == null) {
            this.preProcessPipelineException = new ArrayList<>();
        }
        preProcessPipelineException.add(stackTrace);
    }


    public void setOldModel(CommonDataModel oldModel) {
        this.oldModel = oldModel;
        setChanges(null);
    }


    @JsonIgnore
    @Transient
    public boolean isActive() {
        return activeStatus != null && activeStatus.equals(ActiveStatus.ACTIVE);
    }

}