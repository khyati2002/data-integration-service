package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

import java.util.Map;
import java.util.Set;

public class ProcessRecordStatus extends ProcessFunction<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>, Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> {

    private static final long serialVersionUID = -2517068673681891602L;

    @Override
    public void processElement(
            Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>> record,
            Context ctx,
            Collector<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> out) {

        StreamingRawData streamingRawData = record.f0;
        Map<Class<? extends CommonDataModel>, Set<CommonDataModel>> dataset = record.f1;

        if (dataset == null) {
            // If dataset is null, it means there was a failure → send to side output
            ctx.output(DataStreamJob.FAILED_TRANSFORMATIONS, streamingRawData);
        } else {
            // Otherwise, collect the valid processed data
            out.collect(record);
        }
    }
}
