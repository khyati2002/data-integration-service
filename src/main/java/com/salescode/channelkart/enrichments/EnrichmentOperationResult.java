package com.salescode.channelkart.enrichments;

import com.salescode.channelkart.models.CommonDataModel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class EnrichmentOperationResult {

    private Status status;
    private List<EnrichmentResult> enrichmentResults = new ArrayList<EnrichmentResult>();
    private List<CommonDataModel> enrichedData;

    public EnrichmentOperationResult(Status status) {
        this.status = status;
    }


}
