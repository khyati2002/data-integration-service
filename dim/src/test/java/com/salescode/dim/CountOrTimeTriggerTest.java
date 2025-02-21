package com.salescode.dim;

import org.apache.flink.streaming.api.windowing.triggers.TriggerResult;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.api.windowing.windows.Window;
import org.apache.flink.streaming.runtime.operators.windowing.TriggerTestHarness;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.junit.Test;

import static org.junit.Assert.*;

public class CountOrTimeTriggerTest {

    @Test
    public void testOnElementFiresOnMaxCount() throws Exception {
        TriggerTestHarness<Object, TimeWindow> harness = new TriggerTestHarness<>(CountOrTimeTrigger.of(3, 1000), new TimeWindow.Serializer());
        TimeWindow window = new TimeWindow(0, 1000);

        // First element: count becomes 1 and a timer is registered.
        assertEquals(TriggerResult.CONTINUE, harness.processElement(new StreamRecord<>(1), window));
        assertEquals(1L, harness.numProcessingTimeTimers(window));

        // Second element: count becomes 2.
        assertEquals(TriggerResult.CONTINUE, harness.processElement(new StreamRecord<>(2), window));

        // Third element: count reaches 3 so the trigger fires and clears the timer.
        assertEquals(TriggerResult.FIRE, harness.processElement(new StreamRecord<>(3), window));
        // Verify that the timer was cleared.
        assertEquals(0L, harness.numProcessingTimeTimers(window));

        // The count is reset, so the next element will be treated as the first.
        assertEquals(TriggerResult.CONTINUE, harness.processElement(new StreamRecord<>(4), window));
    }

    @Test
    public void testOnElementFiresOnTime() throws Exception {
        TriggerTestHarness<Object, TimeWindow> harness = new TriggerTestHarness<>(CountOrTimeTrigger.of(5, 1000), new TimeWindow.Serializer());
        TimeWindow window = new TimeWindow(0, 1000);

        // Process two elements; count is less than the max, so no firing.
        assertEquals(TriggerResult.CONTINUE, harness.processElement(new StreamRecord<>(1), window));
        assertEquals(TriggerResult.CONTINUE, harness.processElement(new StreamRecord<>(2), window));

        // Should have one processing time timer registered.
        assertEquals(1L, harness.numProcessingTimeTimers(window));

        // Advance processing time to fire timer.
        assertEquals(TriggerResult.FIRE, harness.advanceProcessingTime(1100, window));

        // Verify that the timer was cleared after firing.
        assertEquals(0L, harness.numProcessingTimeTimers(window));

        // Next element should register a new timer.
        assertEquals(TriggerResult.CONTINUE, harness.processElement(new StreamRecord<>(3), window));
        assertEquals(1L, harness.numProcessingTimeTimers(window));
    }

    @Test
    public void testClear() throws Exception {
        TriggerTestHarness<Object, TimeWindow> harness = new TriggerTestHarness<>(CountOrTimeTrigger.of(5, 1000), new TimeWindow.Serializer());
        TimeWindow window = new TimeWindow(0, 1000);

        // Add some elements.
        harness.processElement(new StreamRecord<>(1), window);
        harness.processElement(new StreamRecord<>(2), window);

        // Should have one timer.
        assertEquals(1L, harness.numProcessingTimeTimers(window));

        // Clear trigger state for the window.
        harness.clearTriggerState(window);

        // Verify timer was cleared.
        assertEquals(0L, harness.numProcessingTimeTimers(window));

        // Next element should create a new timer.
        assertEquals(TriggerResult.CONTINUE, harness.processElement(new StreamRecord<>(3), window));
        assertEquals(1L, harness.numProcessingTimeTimers(window));
    }

    @Test
    public void testMultipleWindowsStateSeparation() throws Exception {
        TriggerTestHarness<Object, TimeWindow> harness = new TriggerTestHarness<>(CountOrTimeTrigger.of(3, 1000), new TimeWindow.Serializer());
        TimeWindow window1 = new TimeWindow(0, 1000);
        TimeWindow window2 = new TimeWindow(1000, 2000);

        // Process one element in each window.
        assertEquals(TriggerResult.CONTINUE, harness.processElement(new StreamRecord<>(1), window1));
        assertEquals(TriggerResult.CONTINUE, harness.processElement(new StreamRecord<>(1), window2));

        // Both windows should have one timer each.
        assertEquals(1L, harness.numProcessingTimeTimers(window1));
        assertEquals(1L, harness.numProcessingTimeTimers(window2));

        // Fire window1 by reaching max count.
        assertEquals(TriggerResult.CONTINUE, harness.processElement(new StreamRecord<>(2), window1));
        assertEquals(TriggerResult.FIRE, harness.processElement(new StreamRecord<>(3), window1));
        // Timer for window1 should be cleared; window2 remains intact.
        assertEquals(0L, harness.numProcessingTimeTimers(window1));
        assertEquals(1L, harness.numProcessingTimeTimers(window2));
    }

    @Test
    public void testStateResetAfterProcessingTimeFiring() throws Exception {
        TriggerTestHarness<Object, TimeWindow> harness = new TriggerTestHarness<>(CountOrTimeTrigger.of(3, 1000), new TimeWindow.Serializer());
        TimeWindow window = new TimeWindow(0, 1000);

        // Process one element.
        assertEquals(TriggerResult.CONTINUE, harness.processElement(new StreamRecord<>(1), window));
        // Advance processing time to trigger the timer.
        assertEquals(TriggerResult.FIRE, harness.advanceProcessingTime(1100, window));
        // Process another element after the timer fired; state should be reset.
        assertEquals(TriggerResult.CONTINUE, harness.processElement(new StreamRecord<>(2), window));
        // Verify that a new timer has been registered.
        assertEquals(1L, harness.numProcessingTimeTimers(window));
    }

    @Test
    public void testClearOnEmptyWindow() throws Exception {
        TriggerTestHarness<Object, TimeWindow> harness = new TriggerTestHarness<>(CountOrTimeTrigger.of(3, 1000), new TimeWindow.Serializer());
        TimeWindow window = new TimeWindow(0, 1000);

        // Clear trigger state on an empty window (no elements processed).
        harness.clearTriggerState(window);
        // Verify that no processing time timers exist.
        assertEquals(0L, harness.numProcessingTimeTimers(window));
    }

    /**
     * Test that once the trigger state is cleared, the previously registered timer does not trigger.
     *
     * Because the test harness's advanceProcessingTime(...) method expects exactly one timer firing,
     * if no timer is scheduled it throws an exception. We catch that exception and assert that its message
     * indicates that no timer fired.
     */
    @Test
    public void testTimerNotTriggeringAfterClear() throws Exception {
        TriggerTestHarness<Object, TimeWindow> harness =
                new TriggerTestHarness<>(CountOrTimeTrigger.of(3, 1000), new TimeWindow.Serializer());
        TimeWindow window = new TimeWindow(0, 1000);

        // Process an element so that a timer is registered.
        assertEquals(TriggerResult.CONTINUE, harness.processElement(new StreamRecord<>(1), window));
        // Confirm that a timer is registered.
        assertEquals(1L, harness.numProcessingTimeTimers(window));

        // Clear the trigger state for the window.
        harness.clearTriggerState(window);
        // Verify that the timer has been cleared.
        assertEquals(0L, harness.numProcessingTimeTimers(window));

        // Advance processing time beyond the scheduled timer time.
        try {
            harness.advanceProcessingTime(1100, window);
            fail("Expected IllegalStateException since no timer should fire after clear.");
        } catch (IllegalStateException e) {
            // We expect an exception indicating that no timer fired.
            assertTrue(e.getMessage().contains("Fired timers: []"));
        }
    }
}