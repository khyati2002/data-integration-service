package com.applicate.services.channelkart.enrichments;

import com.applicate.services.channelkart.models.CommonDataModel;
import java.util.ArrayList;
import java.util.List;

public class EnrichmentOperationResult {

    private Status status;
    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }
    public EnrichmentOperationResult(Status status) {
        this.status=status;
    }

    private List<EnrichmentResult> enrichmentResults=new ArrayList<EnrichmentResult>();
    public List<EnrichmentResult> getEnrichmentResults() {
        return enrichmentResults;
    }
    public void setEnrichmentResults(List<EnrichmentResult> enrichmentResults) {
        this.enrichmentResults = enrichmentResults;
    }

    private List<CommonDataModel> enrichedData;


    public List<CommonDataModel> getEnrichedData() {
        return enrichedData;
    }

    public void setEnrichedData(List<CommonDataModel> enrichedData) {
        this.enrichedData = enrichedData;
    }


}
