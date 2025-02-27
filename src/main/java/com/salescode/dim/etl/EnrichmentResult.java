package com.salescode.dim.etl;

import com.applicate.services.channelkart.models.CommonDataModel;

import java.util.List;

public interface EnrichmentResult extends ProcessResult {

    List<CommonDataModel> getStepResultData();

    void setStepResultData(List<CommonDataModel> stepResultData);

}
