package com.applicate.services.channelkart.enrichments;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.utils.ReflectionUtils;
import com.applicate.services.channelkart.utils.TimerUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataEnrichmentEngine {

    public static final DataEnrichmentEngine INSTANCE = new DataEnrichmentEngine();
    private static final String ERROR_MESSAGE = "Some error occurred while Enriching input";
    private static final Logger logger = LoggerFactory.getLogger(DataEnrichmentEngine.class);

    public EnrichmentResult execute(CommonDataModel cdm, EnrichmentInfo enrichmentInfo) {
        EnrichmentResult result = null;
        try {
            AbstractEnrichment<CommonDataModel> ar = getEnrichment(enrichmentInfo);
            result = TimerUtils.withTime("DataEnrichmentEngine " + getImple(enrichmentInfo), () -> ar.apply(cdm));
        } catch (Exception e) {
            logger.error("Encrichment exception: ", e);
            cdm.addPreProcessPipelineException(ExceptionUtils.getStackTrace(e));
            result = new EnrichmentResult(Status.ERROR, ERROR_MESSAGE, ERROR_MESSAGE);
            result.setEnrichmentInfo(enrichmentInfo);
            return result;
        }
        result.setEnrichmentInfo(enrichmentInfo);
        return result;
    }

    private String getImple(EnrichmentInfo ar) {
        return ar != null ? ar.getImplementation() : "Implementation is null";
    }

    private AbstractEnrichment<CommonDataModel> getEnrichment(EnrichmentInfo enrichmentInfo) {
        AbstractEnrichment<CommonDataModel> abstractEnrichment = ReflectionUtils.createInstance(enrichmentInfo.getImplementation());
        abstractEnrichment.setEnrichmentInfo(enrichmentInfo);
        return abstractEnrichment;
    }

}
