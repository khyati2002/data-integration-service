package com.salescode.dim.etl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OperationServiceHelper {

    public static OperationResult getOperationResult(Map<OperationResult.Status, List<OperationResult.StepResult>> resultsByStatus) {
        OperationResult.Status finalStatus = determineFinalStatus(resultsByStatus);

        OperationResult operationResult = new OperationResult(finalStatus);
        operationResult.getStepResults()
                .addAll(resultsByStatus.getOrDefault(OperationResult.Status.ERROR, Collections.emptyList()));
        operationResult.getStepResults()
                .addAll(resultsByStatus.getOrDefault(OperationResult.Status.WARNING, Collections.emptyList()));
        operationResult.getStepResults()
                .addAll(resultsByStatus.getOrDefault(OperationResult.Status.CONFLICT, Collections.emptyList()));
        operationResult.getStepResults()
                .addAll(resultsByStatus.getOrDefault(OperationResult.Status.OK, Collections.emptyList()));

        return operationResult;
    }

    /**
     * Determines the final status based on the presence of different statuses in results.
     *
     * @param resultsByStatus a map of statuses to their corresponding results
     * @return the final status
     */
    public static OperationResult.Status determineFinalStatus(Map<OperationResult.Status, List<OperationResult.StepResult>> resultsByStatus) {
        for (OperationResult.Status status : Arrays.asList(OperationResult.Status.CONFLICT, OperationResult.Status.ERROR)) {
            if (resultsByStatus.containsKey(status) && !resultsByStatus.get(status).isEmpty()) {
                return status;
            }
        }
        return OperationResult.Status.OK;
    }
}
