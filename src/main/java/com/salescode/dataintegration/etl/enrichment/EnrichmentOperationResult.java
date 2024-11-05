package com.salescode.dataintegration.etl.enrichment;

import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.dataintegration.etl.enrichment.EnrichmentResult.Status;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class EnrichmentOperationResult {

    private Status status;
    private List<EnrichmentResult> enrichmentResults = new ArrayList<>();
    private List<CommonDataModel> enrichedData;

    public EnrichmentOperationResult(Status status) {
        this.status = status;
    }

    public EnrichmentOperationResult(Status status, List<CommonDataModel> enrichedData) {
        this.status = status;
        this.enrichedData = enrichedData;
    }

    public void merge(EnrichmentOperationResult operationResult) {
        this.status = this.getStatus().compareTo(operationResult.getStatus()) > 0 ? this.getStatus() : operationResult.getStatus();
        this.getEnrichmentResults().addAll(operationResult.getEnrichmentResults());
        this.enrichedData = operationResult.getEnrichedData();
    }
}
