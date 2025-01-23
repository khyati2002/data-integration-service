package com.applicate.services.channelkart.enrichments;
import com.applicate.services.channelkart.models.CommonDataModel;
import java.util.List;
public class EnrichmentResult {

    public static EnrichmentResult OK = new EnrichmentResult(Status.OK);
    public static EnrichmentResult ERROR = new EnrichmentResult(Status.ERROR);
    public static EnrichmentResult WARNING = new EnrichmentResult(Status.WARNING);
    private Status status;
    private String message;
    private String errorCode;
    private EnrichmentInfo enrichmentInfo;

    private List<CommonDataModel> enrichedData;

    private CommonDataModel originalModel;

    public void setOriginalModel(CommonDataModel originalModel) {
        this.originalModel = originalModel;
    }

    public CommonDataModel getOriginalModel() {
        return this.originalModel;
    }


    public List<CommonDataModel> getEnrichedData() {
        return enrichedData;
    }

    public void setEnrichedData(List<CommonDataModel> enrichedData) {
        this.enrichedData = enrichedData;
    }

    public EnrichmentInfo getEnrichmentInfo() {
        return enrichmentInfo;
    }

    public void setEnrichmentInfo(EnrichmentInfo enrichmentInfo) {
        this.enrichmentInfo = enrichmentInfo;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
    private EnrichmentResult(Status status) {
        this(status,null);
    }

    public EnrichmentResult(Status status, String message, String errorCode, EnrichmentInfo enrichmentInfo, List<CommonDataModel> enrichedData, CommonDataModel originalModel) {
        this.status = status;
        this.message = message;
        this.errorCode = errorCode;
        this.enrichmentInfo = enrichmentInfo;
        this.enrichedData = enrichedData;
        this.originalModel = originalModel;
    }

    public EnrichmentResult(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public EnrichmentResult(Status status,String message,String errorCode) {
        this.status = status;
        this.message = message;
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

}
