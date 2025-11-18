package com.salescode.dim.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneOffset;

public class LocalDateTimeKryoSerializer extends Serializer<LocalDateTime> implements Serializable {
    @Override
    public void write(Kryo kryo, Output output, LocalDateTime localDateTime) {
        output.writeLong(localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli());
    }

    @Override
    public LocalDateTime read(Kryo kryo, Input input, Class<LocalDateTime> type) {
        long epochMilli = input.readLong();
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneOffset.UTC);
    }
}
