package com.applicate.services.channelkart.validations;

import com.applicate.services.channelkart.models.CommonDataModel;
import java.util.Comparator;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="ck_validation_rule")
public class RuleInfo extends CommonDataModel implements Comparator<RuleInfo>{
    private String code;
    private String description;
    private String documentLink;
    private int severity;
    private int priority;
    private boolean enabled;
    private String type;
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
    public boolean isEnabled() {
        return enabled;
    }
    public boolean getEnabled() {
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
    public int compare(RuleInfo r1, RuleInfo r2) {
        if(r1.getPriority()<r2.getPriority())
            return 1;
        else if (r1.getPriority()>r2.getPriority())
            return -1;
        return 0;
    }

}
