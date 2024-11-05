package com.salescode.dataintegration.etl.enrichment;

import com.salescode.channelkart.models.CommonDataModel;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EnrichmentResult {

    public static EnrichmentResult OK = new EnrichmentResult(Status.OK);
    public static EnrichmentResult ERROR = new EnrichmentResult(Status.ERROR);
    public static EnrichmentResult WARNING = new EnrichmentResult(Status.WARNING);

    private Status status;
    private String message;
    private List<? extends CommonDataModel> enrichedData;

    public EnrichmentResult(Status status) {
        this(status, null);
    }

    public EnrichmentResult(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public enum Status {
        OK, WARNING, ERROR, CONFLICT
    }

}
