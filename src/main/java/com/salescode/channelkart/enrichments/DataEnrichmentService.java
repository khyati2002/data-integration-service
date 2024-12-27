package com.salescode.channelkart.enrichments;

import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.security.SecurityContextUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DataEnrichmentService {

    public EnrichmentOperationResult enrich(CommonDataModel cdm, EnrichmentPhase phase) {

        String lob = SecurityContextUtils.getLob();

        List<EnrichmentInfo> rules = EnrichmentRegistry.INSTANCE.get(lob, cdm.getClass().getSimpleName(), phase).stream().sorted(Comparator.comparingInt(EnrichmentInfo::getPriority)).collect(Collectors.toList());

        @SuppressWarnings("serial") List<CommonDataModel> cdmlist = new ArrayList<CommonDataModel>() {{
            add(cdm);
        }};

        List<EnrichmentResult> erResults = new ArrayList<>();

        for (EnrichmentInfo info : rules) {

            List<EnrichmentResult> results = cdmlist.stream().map(f -> DataEnrichmentEngine.INSTANCE.execute(f, info)).collect(Collectors.toList());

            List<CommonDataModel> currentCDMS = new ArrayList<>();

            for (EnrichmentResult result : results) {

                if (result.getEnrichedData() != null && !result.getEnrichedData().isEmpty()) {

                    currentCDMS.addAll(result.getEnrichedData());
                }
            }
            if (!currentCDMS.isEmpty()) {
                cdmlist = currentCDMS;
            }

            erResults.addAll(results);

        }

        EnrichmentOperationResult ers = evaluateResults(erResults);

        ers.setEnrichedData(cdmlist);

        return ers;

    }

    public EnrichmentOperationResult getResults(List<EnrichmentResult> ruleResult) {
        return evaluateResults(ruleResult);
    }

    private EnrichmentOperationResult evaluateResults(List<EnrichmentResult> ruleResult) {

        Map<Status, List<EnrichmentResult>> resultsByStatus = ruleResult.stream().collect(Collectors.groupingBy(EnrichmentResult::getStatus));

        Status status = resultsByStatus.get(Status.ERROR) == null && resultsByStatus.get(Status.CONFLICT) == null ? Status.OK : resultsByStatus.get(Status.CONFLICT) != null ? Status.CONFLICT : Status.ERROR;

        EnrichmentOperationResult vr = new EnrichmentOperationResult(status);

        if (resultsByStatus.get(Status.ERROR) != null)
            vr.getEnrichmentResults().addAll(resultsByStatus.get(Status.ERROR));

        if (resultsByStatus.get(Status.WARNING) != null)
            vr.getEnrichmentResults().addAll(resultsByStatus.get(Status.WARNING));

        if (resultsByStatus.get(Status.CONFLICT) != null)
            vr.getEnrichmentResults().addAll(resultsByStatus.get(Status.CONFLICT));

        return vr;
    }

}
