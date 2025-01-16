package com.applicate.services.channelkart.validations;


import com.applicate.services.channelkart.models.CommonDataModel;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Comparator;

@Setter
@Getter
@Entity
@Table(name = "ck_validation_rule")
public class RuleInfo extends CommonDataModel implements Comparator<RuleInfo> {
    private String code;
    private String description;
    private String documentLink;
    private int severity;
    private int priority;
    private boolean enabled;
    private String type;

    private String language;
    private String implementation;//"com.test.execute.Rule1"

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public int compare(RuleInfo r1, RuleInfo r2) {
        if (r1.getPriority() < r2.getPriority()) return 1;
        else if (r1.getPriority() > r2.getPriority()) return -1;
        return 0;
    }

}
