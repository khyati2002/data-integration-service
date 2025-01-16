package com.applicate.services.channelkart.enrichments;


import com.applicate.services.channelkart.models.CommonDataModel;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.Comparator;

@Entity
@Table(name = "ck_enrichment_info")
@Getter
@Setter
public class EnrichmentInfo extends CommonDataModel implements Comparator<EnrichmentInfo> {
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

    private String language;
    private String implementation;//"com.test.execute.Rule1"

    @Override
    public int compare(EnrichmentInfo r1, EnrichmentInfo r2) {
        if (r1.getPriority() < r2.getPriority())
            return 1;
        else if (r1.getPriority() > r2.getPriority())
            return -1;
        return 0;
    }

}
