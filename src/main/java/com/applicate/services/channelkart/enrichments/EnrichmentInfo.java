package com.applicate.services.channelkart.enrichments;

import com.applicate.services.channelkart.models.CommonDataModel;
import java.util.Comparator;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name="ck_enrichment_info")
public class EnrichmentInfo extends CommonDataModel implements Comparator<EnrichmentInfo>{
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private String code;
    private String description;
    private String documentLink;
    private int severity;
    private int priority;
    private boolean enabled;
    private String type;

    @Enumerated(EnumType.STRING)
    @NotNull
    private EnrichmentPhase phase;

    public EnrichmentPhase getPhase() {
        return phase;
    }
    public void setPhase(EnrichmentPhase phase) {
        this.phase = phase;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    private String language;
    private String implementation;//"com.test.execute.Rule1"

    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getDocumentLink() {
        return documentLink;
    }
    public void setDocumentLink(String documentLink) {
        this.documentLink = documentLink;
    }
    public int getSeverity() {
        return severity;
    }
    public void setSeverity(int severity) {
        this.severity = severity;
    }
    public int getPriority() {
        return priority;
    }
    public void setPriority(int priority) {
        this.priority = priority;
    }
    public boolean getEnabled() {
        return enabled;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public String getLanguage() {
        return language;
    }
    public void setLanguage(String language) {
        this.language = language;
    }
    public String getImplementation() {
        return implementation;
    }
    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }

    @Override
    public int compare(EnrichmentInfo r1, EnrichmentInfo r2) {
        if(r1.getPriority()<r2.getPriority())
            return 1;
        else if (r1.getPriority()>r2.getPriority())
            return -1;
        return 0;
    }

}
