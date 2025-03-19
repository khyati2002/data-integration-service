package com.salescode.dim;

import com.applicate.services.channelkart.models.CommonDataModel;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.util.Collector;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ListWindowFunction implements WindowFunction<
        List<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>>,  // Input type
        List<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>>,  // Output type
        Void,   // No key (since using `windowAll()`)
        GlobalWindow> {

    @Override
    public void apply(Void key, GlobalWindow window,
                      Iterable<List<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>>> input,
                      Collector<List<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>>> out) {

        for (List<Tuple2<StreamingRawData, Map<Class<? extends CommonDataModel>, Set<CommonDataModel>>>> batch : input) {
            out.collect(batch);  // Emit aggregated batch
        }
    }
}
