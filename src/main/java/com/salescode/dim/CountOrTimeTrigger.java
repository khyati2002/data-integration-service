package com.salescode.dim;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.windowing.triggers.Trigger;
import org.apache.flink.streaming.api.windowing.triggers.TriggerResult;
import org.apache.flink.streaming.api.windowing.windows.Window;

public class CountOrTimeTrigger<T,W extends Window> extends Trigger<Object, W> {
    private final int maxCount;
    private final long intervalMs;

    private final ValueStateDescriptor<Integer> countDesc =
            new ValueStateDescriptor<>("count", Types.INT);
    private final ValueStateDescriptor<Long> timerDesc =
            new ValueStateDescriptor<>("timer", Types.LONG);

    CountOrTimeTrigger(int maxCount, long intervalMs) {
        this.maxCount = maxCount;
        this.intervalMs = intervalMs;
    }

    public static <T,W extends Window> CountOrTimeTrigger<T, W> of(int maxCount, long interval) {
        return new CountOrTimeTrigger<>(maxCount, interval);
    }

    @Override
    public TriggerResult onElement(Object element, long timestamp, W window, TriggerContext ctx) throws Exception {
        ValueState<Integer> countState = ctx.getPartitionedState(countDesc);
        ValueState<Long> timerState = ctx.getPartitionedState(timerDesc);

        int count = (countState.value() == null) ? 0 : countState.value();
        count++;
        countState.update(count);

        if (count >= maxCount) {
            // Reset count and clear timer
            countState.clear();
            Long timerTimestamp = timerState.value();
            if (timerTimestamp != null) {
                ctx.deleteProcessingTimeTimer(timerTimestamp);
                timerState.clear();
            }
            return TriggerResult.FIRE_AND_PURGE;
        } else if (count == 1) {
            // Register timer for the first element
            long nextFireTime = ctx.getCurrentProcessingTime() + intervalMs;
            ctx.registerProcessingTimeTimer(nextFireTime);
            timerState.update(nextFireTime);
        }

        return TriggerResult.CONTINUE;
    }

    @Override
    public TriggerResult onProcessingTime(long time, W window, TriggerContext ctx) throws Exception {
        // Clear states and fire
        ctx.getPartitionedState(countDesc).clear();
        ctx.getPartitionedState(timerDesc).clear();
        return TriggerResult.FIRE_AND_PURGE;
    }

    @Override
    public TriggerResult onEventTime(long time, W window, TriggerContext ctx) {
        return TriggerResult.CONTINUE;
    }

    @Override
    public void clear(W window, TriggerContext ctx) throws Exception {
        ctx.getPartitionedState(countDesc).clear();
        ValueState<Long> timerState = ctx.getPartitionedState(timerDesc);
        Long timerTimestamp = timerState.value();
        if (timerTimestamp != null) {
            ctx.deleteProcessingTimeTimer(timerTimestamp);
        }
        timerState.clear();
    }

    @Override
    public String toString() {
        return "CountOrTimeTrigger(" + maxCount + ", " + intervalMs + "ms)";
    }
}