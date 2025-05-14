package com.salescode.dim.etl.enrichment.service;

import com.applicate.services.channelkart.enrichments.EnrichmentPhase;
import com.applicate.services.channelkart.models.CommonDataModel;
import com.salescode.dim.StreamingRawDataProcessor;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.OperationResult.StepResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.jooq.generated.tables.pojos.EnrichmentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.etl.OperationServiceHelper.getOperationResult;

/**
 * Service responsible for enriching data models based on configured enrichment rules.
 * Applies rules in order of priority and manages the enrichment lifecycle.
 */
public class DataEnrichmentService {

    private static final String ERROR_MESSAGE = "Enrichment error: ";

    private final EnrichmentInfoRegistry enrichmentInfoRegistry;
    private final ETLRegistry etlRegistry;

    Logger logger = LoggerFactory.getLogger(DataEnrichmentService.class);

    /**
     * Creates a new DataEnrichmentService with the required dependencies.
     *
     * @param enrichmentInfoRegistry registry containing enrichment rule information
     * @param etlRegistry            registry for obtaining enrichment implementations
     */
    public DataEnrichmentService(EnrichmentInfoRegistry enrichmentInfoRegistry, ETLRegistry etlRegistry) {
        this.enrichmentInfoRegistry = Objects.requireNonNull(enrichmentInfoRegistry, "EnrichmentInfoRegistry cannot be null");
        this.etlRegistry = Objects.requireNonNull(etlRegistry, "ETLRegistry cannot be null");
    }

    /**
     * Enriches a single CommonDataModel based on the specified EnrichmentPhase.
     *
     * @param cdm   the data model to enrich (must not be null)
     * @param phase the enrichment phase (must not be null)
     * @return the result of the enrichment operation
     * @throws NullPointerException if cdm or phase is null
     */
    public OperationResult enrich(CommonDataModel cdm, EnrichmentPhase phase) {
        Objects.requireNonNull(cdm, "CommonDataModel cannot be null");
        Objects.requireNonNull(phase, "EnrichmentPhase cannot be null");
        return enrich(Collections.singletonList(cdm), phase);
    }

    /**
     * Enriches a list of CommonDataModels based on the specified EnrichmentPhase.
     * Applies all relevant enrichment rules in priority order.
     *
     * @param currentDataModels the list of data models to enrich
     * @param phase             the enrichment phase
     * @return the operation result containing enriched data models and status
     * @throws NullPointerException if phase is null
     */
    public OperationResult enrich(List<CommonDataModel> currentDataModels, EnrichmentPhase phase) {
        Objects.requireNonNull(phase, "EnrichmentPhase cannot be null");

        if (currentDataModels == null || currentDataModels.isEmpty()) {
            return OperationResult.of(OperationResult.Status.OK, currentDataModels);
        }

        String modelType = currentDataModels.get(0).getClass().getSimpleName();
        List<EnrichmentInfo> enrichmentRules = fetchEnrichmentRules(phase, modelType);

        if (enrichmentRules.isEmpty()) {
            return OperationResult.of(OperationResult.Status.OK, currentDataModels);
        }

        List<CommonDataModel> resultantModels = new ArrayList<>(currentDataModels);
        List<EnrichmentResult> allResults = new ArrayList<>();

        for (EnrichmentInfo enrichment : enrichmentRules) {
            try {
                List<EnrichmentResult> currentStepResults = resultantModels.parallelStream()
                        .map(model -> applyEnrichment(model, enrichment))
                        .collect(Collectors.toList());

                allResults.addAll(currentStepResults);

                List<CommonDataModel> enrichedModels = currentStepResults.stream()
                        .filter(result -> result.getStepResultData() != null && !result.getStepResultData().isEmpty())
                        .flatMap(result -> result.getStepResultData().stream())
                        .collect(Collectors.toList());

                if (!enrichedModels.isEmpty()) {
                    resultantModels = enrichedModels;
                }
            } catch (Exception e) {
                logger.error("Error during enrichment: {}", e);
                logger.error("Error during enrichment: {}", e.getStackTrace());
                allResults.add(new StepResult(OperationResult.Status.ERROR, ERROR_MESSAGE + e.getMessage()));
            }
        }

        OperationResult result = evaluateResults(allResults);
        result.getOperationResultData().addAll(resultantModels);

        return result;
    }

    /**
     * Fetches and sorts enrichment rules applicable to the data model and phase.
     *
     * @param phase     the enrichment phase
     * @param modelType the data model type (simple class name)
     * @return a list of sorted enrichment rules
     */
    private List<EnrichmentInfo> fetchEnrichmentRules(EnrichmentPhase phase, String modelType) {
        return enrichmentInfoRegistry.getEnrichmentInfoByPhaseAndType(phase.name(), modelType)
                .stream()
                .sorted(Comparator.comparingInt(EnrichmentInfo::getPriority))
                .collect(Collectors.toList());
    }

    /**
     * Applies a single enrichment to a data model.
     *
     * @param cdm            the data model
     * @param enrichmentInfo the enrichment information
     * @return the result of the enrichment
     */
    private EnrichmentResult applyEnrichment(CommonDataModel cdm, EnrichmentInfo enrichmentInfo) {
        String implementationName = enrichmentInfo.getImplementation();
        try {
            AbstractEnrichment<CommonDataModel> enrichment = etlRegistry.getEnrichment(implementationName);
            enrichment.setEnrichmentInfo(enrichmentInfo);
            return enrichment.apply(cdm);
        } catch (Exception e) {
            return new StepResult(OperationResult.Status.ERROR, ERROR_MESSAGE + String.format("Implementation '%s' failed: %s", implementationName, e.getMessage()));
        }
    }

    /**
     * Evaluates the overall status based on individual enrichment results.
     *
     * @param results the list of enrichment results
     * @return the operation result with aggregated status
     */
    private OperationResult evaluateResults(List<EnrichmentResult> results) {
        Map<OperationResult.Status, List<StepResult>> resultsByStatus = results.stream()
                .map(StepResult.class::cast)
                .collect(Collectors.groupingBy(EnrichmentResult::getStatus));
        return getOperationResult(resultsByStatus);
    }
}
