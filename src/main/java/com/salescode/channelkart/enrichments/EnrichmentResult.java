package com.salescode.channelkart.enrichments;

import com.salescode.channelkart.models.CommonDataModel;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class EnrichmentResult {

    public static EnrichmentResult OK = new EnrichmentResult(Status.OK);
    public static EnrichmentResult ERROR = new EnrichmentResult(Status.ERROR);
    public static EnrichmentResult WARNING = new EnrichmentResult(Status.WARNING);
    private Status status;
    private String message;
    private String errorCode;
    private EnrichmentInfo enrichmentInfo;

    private List<CommonDataModel> enrichedData;


    private EnrichmentResult(Status status) {
        this(status, null);
    }

    public EnrichmentResult(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public EnrichmentResult(Status status, String message, String errorCode) {
        this.status = status;
        this.message = message;
        this.errorCode = errorCode;
    }

}
