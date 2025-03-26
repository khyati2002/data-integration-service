package com.salescode.dim;

import org.apache.flink.types.PojoTestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StreamingRawDataTest {

    @Test
    void serializationShouldNotFallbackToKryo() {
        // This Flink test-utils method verifies that serialization does not fall back to Kryo
        // It does not test whether the serialization/deserialization actually work
        // If you make a mistake defining the TypeInfo for the class, this test will succeed
        // but the serialization may still fail or give unexpected results.
        PojoTestUtils.assertSerializedAsPojoWithoutKryo(StreamingRawData.class);

        // This test is redundant if you implement the other test, below.
    }
}