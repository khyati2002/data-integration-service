package com.salescode.dis.flink.aggregator;

import org.apache.flink.api.common.functions.AggregateFunction;

import java.util.ArrayList;
import java.util.List;

public class ListAggregator<T> implements AggregateFunction<T, List<T>, List<T>> {
    @Override
    public List<T> createAccumulator() {
        return new ArrayList<>();
    }

    @Override
    public List<T> add(T value, List<T> accumulator) {
        accumulator.add(value);
        return accumulator;
    }

    @Override
    public List<T> getResult(List<T> accumulator) {
        return accumulator;
    }

    @Override
    public List<T> merge(List<T> a, List<T> b) {
        a.addAll(b);
        return a;
    }
}