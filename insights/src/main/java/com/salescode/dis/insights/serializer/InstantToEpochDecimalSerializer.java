package com.salescode.dis.insights.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;

public class InstantToEpochDecimalSerializer extends StdSerializer<Instant> {

    public InstantToEpochDecimalSerializer() {
        super(Instant.class);
    }

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value != null) {
            BigDecimal epochDecimal = BigDecimal.valueOf(value.getEpochSecond())
                    .add(BigDecimal.valueOf(value.getNano(), 9)); // 9 decimal places
            gen.writeNumber(epochDecimal);
        } else {
            gen.writeNull();
        }
    }
}
