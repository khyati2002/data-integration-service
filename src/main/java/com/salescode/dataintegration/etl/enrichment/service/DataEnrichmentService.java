package com.salescode.dataintegration.etl.enrichment.service;

import com.salescode.channelkart.converters.EnrichmentPhase;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.dataintegration.etl.enrichment.AbstractEnrichment;
import com.salescode.dataintegration.etl.enrichment.EnrichmentOperationResult;
import com.salescode.dataintegration.etl.enrichment.EnrichmentResult;
import com.salescode.dataintegration.etl.enrichment.registry.EnrichmentInfoRegistry;
import com.salescode.dataintegration.etl.registry.ETLRegistry;
import com.salescode.jooq.generated.tables.pojos.CkEnrichmentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dataintegration.etl.enrichment.EnrichmentResult.Status;

@Service
public class DataEnrichmentService {

    private static final Logger logger = LoggerFactory.getLogger(DataEnrichmentService.class);
    private static final String ERROR_MESSAGE = "An error occurred while enriching input";

    private final EnrichmentInfoRegistry enrichmentInfoRegistry;
    private final ETLRegistry etlRegistry;

    @Autowired
    public DataEnrichmentService(EnrichmentInfoRegistry enrichmentInfoRegistry, ETLRegistry etlRegistry) {
        this.enrichmentInfoRegistry = enrichmentInfoRegistry;
        this.etlRegistry = etlRegistry;
    }

    /**
     * Enriches the given CommonDataModel based on the specified EnrichmentPhase.
     *
     * @param cdm   the data model to enrich
     * @param phase the enrichment phase
     * @return the result of the enrichment operation
     */
    public EnrichmentOperationResult enrich(CommonDataModel cdm, EnrichmentPhase phase) {
        List<CommonDataModel> currentDataModels = new ArrayList<>(List.of(cdm));
        return enrich(currentDataModels, phase);
    }

    /**
     * Enriches the given list of CommonDataModels based on the specified EnrichmentPhase.
     *
     * @param currentDataModels the list of data models to enrich
     * @param phase             the enrichment phase
     * @return a list of enrichment operation results
     */
    public EnrichmentOperationResult enrich(List<CommonDataModel> currentDataModels, EnrichmentPhase phase) {
        if (currentDataModels == null || currentDataModels.isEmpty()) {
            return new EnrichmentOperationResult(Status.OK, currentDataModels);
        }
        List<EnrichmentResult> allEnrichmentResults = new ArrayList<>();
        List<CkEnrichmentInfo> enrichmentRules = fetchEnrichmentRules(currentDataModels.get(0).getClass().getSimpleName(), phase);
        for (CkEnrichmentInfo rule : enrichmentRules) {
            List<EnrichmentResult> enrichmentResults = applyRuleToDataModels(rule, currentDataModels);
            List<CommonDataModel> currentCDMS = extractEnrichedDataModels(enrichmentResults);
            if (!currentCDMS.isEmpty()) {
                currentDataModels = currentCDMS;
            }
            allEnrichmentResults.addAll(enrichmentResults);
        }
        EnrichmentOperationResult operationResult = evaluateResults(allEnrichmentResults);
        operationResult.setEnrichedData(currentDataModels);
        return operationResult;
    }


    /**
     * Fetches and sorts enrichment rules applicable to the data model and phase.
     *
     * @param type  the data model type
     * @param phase the enrichment phase
     * @return a list of sorted enrichment rules
     */
    private List<CkEnrichmentInfo> fetchEnrichmentRules(String type, EnrichmentPhase phase) {
        return enrichmentInfoRegistry.getEnrichmentInfoByPhase(phase).stream()
                .filter(info -> info.getType().equals(type))
                .sorted(Comparator.comparingInt(CkEnrichmentInfo::getPriority))
                .collect(Collectors.toList());
    }

    /**
     * Applies a single enrichment rule to a list of data models.
     *
     * @param rule       the enrichment rule
     * @param dataModels the data models to enrich
     * @return a list of enrichment results
     */
    private List<EnrichmentResult> applyRuleToDataModels(CkEnrichmentInfo rule, List<CommonDataModel> dataModels) {
        return dataModels.stream()
                .map(model -> applyEnrichment(model, rule))
                .collect(Collectors.toList());
    }

    /**
     * Extracts enriched data models from a list of enrichment results.
     *
     * @param enrichmentResults the enrichment results
     * @return a list of enriched data models
     */
    private List<CommonDataModel> extractEnrichedDataModels(List<EnrichmentResult> enrichmentResults) {
        return enrichmentResults.stream()
                .filter(result -> result.getEnrichedData() != null && !result.getEnrichedData().isEmpty())
                .flatMap(result -> result.getEnrichedData().stream())
                .collect(Collectors.toList());
    }

    /**
     * Evaluates the overall status based on individual enrichment results.
     *
     * @param results the list of enrichment results
     * @return the operation result with aggregated status
     */
    private EnrichmentOperationResult evaluateResults(List<EnrichmentResult> results) {
        Map<Status, List<EnrichmentResult>> resultsByStatus = results.stream()
                .collect(Collectors.groupingBy(EnrichmentResult::getStatus));

        Status finalStatus = determineFinalStatus(resultsByStatus);

        EnrichmentOperationResult operationResult = new EnrichmentOperationResult(finalStatus);
        operationResult.getEnrichmentResults().addAll(resultsByStatus.getOrDefault(Status.ERROR, Collections.emptyList()));
        operationResult.getEnrichmentResults().addAll(resultsByStatus.getOrDefault(Status.WARNING, Collections.emptyList()));
        operationResult.getEnrichmentResults().addAll(resultsByStatus.getOrDefault(Status.CONFLICT, Collections.emptyList()));

        return operationResult;
    }

    /**
     * Determines the final status based on the presence of different statuses in results.
     *
     * @param resultsByStatus a map of statuses to their corresponding results
     * @return the final status
     */
    private Status determineFinalStatus(Map<Status, List<EnrichmentResult>> resultsByStatus) {
        if (resultsByStatus.containsKey(Status.ERROR)) {
            return Status.ERROR;
        } else if (resultsByStatus.containsKey(Status.CONFLICT)) {
            return Status.CONFLICT;
        } else if (resultsByStatus.containsKey(Status.WARNING)) {
            return Status.WARNING;
        } else {
            return Status.OK;
        }
    }

    /**
     * Applies a single enrichment to a data model.
     *
     * @param cdm            the data model
     * @param enrichmentInfo the enrichment information
     * @return the result of the enrichment
     */
    private EnrichmentResult applyEnrichment(CommonDataModel cdm, CkEnrichmentInfo enrichmentInfo) {
        try {
            AbstractEnrichment<CommonDataModel> enrichment = etlRegistry.getEnrichment(enrichmentInfo.getImplementation());
            enrichment.setEnrichmentInfo(enrichmentInfo);
            return enrichment.apply(cdm);
        } catch (Exception e) {
//            cdm.addPreProcessPipelineException(ExceptionUtils.getStackTrace(e));
            return new EnrichmentResult(Status.ERROR, ERROR_MESSAGE + e.getMessage());
        }
    }

}