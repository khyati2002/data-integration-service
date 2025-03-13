package com.salescode.dim.etl;

import com.applicate.services.channelkart.models.CommonDataModel;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class OperationResult {

    public static OperationResult OK = new OperationResult(Status.OK);

    private final Status status;
    private final List<CommonDataModel> operationResultData = new ArrayList<>();
    private final List<StepResult> stepResults = new ArrayList<>();

    public OperationResult(Status status) {
        this.status = status;
    }

    public OperationResult(Status status, List<CommonDataModel> commonDataModels) {
        this.status = status;
        this.operationResultData.addAll(commonDataModels);
    }

    public static OperationResult of(Status status, List<CommonDataModel> operationResultData) {
        return new OperationResult(status, operationResultData);
    }

    public enum Status {
        OK, WARNING, ERROR, CONFLICT
    }

    @Builder
    @AllArgsConstructor
    @Data
    public static class StepResult implements ValidationResult, EnrichmentResult {

        public static StepResult OK = new StepResult(Status.OK);
        public static StepResult WARNING = new StepResult(Status.WARNING);
        public static StepResult ERROR = new StepResult(Status.ERROR);
        public static StepResult CONFLICT = new StepResult(Status.CONFLICT);

        private Status status;
        private String message;
        private List<CommonDataModel> stepResultData = new ArrayList<>();

        public StepResult(Status status) {
            this.status = status;
        }

        public StepResult(Status status, String message) {
            this.status = status;
            this.message = message;
        }

    }


}
