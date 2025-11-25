package com.salescode.dim.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

public class KryoConfig {

    public static Kryo createKryo() {
        Kryo kryo = new Kryo();

        // Register custom serializers for immutable collections
        kryo.register(List.of().getClass(), new ImmutableListSerializer());
        kryo.register(Set.of().getClass(), new ImmutableSetSerializer());
        kryo.register(LocalDateTime.class, new LocalDateTimeKryoSerializer());

        return kryo;
    }

    // Custom serializer for List.of()
    public static class ImmutableListSerializer extends Serializer<List<?>> {
        @Override
        public void write(Kryo kryo, Output output, List<?> object) {
            kryo.writeObject(output, object.toArray());
        }

        @Override
        public List<?> read(Kryo kryo, Input input, Class<List<?>> aClass) {
            Object[] array = kryo.readObject(input, Object[].class);
            return List.of(array);
        }

    }

    // Custom serializer for Set.of()
    public static class ImmutableSetSerializer extends Serializer<Set<?>> {
        @Override
        public void write(Kryo kryo, Output output, Set<?> object) {
            kryo.writeObject(output, object.toArray());
        }

        @Override
        public Set<?> read(Kryo kryo, Input input, Class<Set<?>> aClass) {
            Object[] array = kryo.readObject(input, Object[].class);
            return Set.of(array);
        }

    }
    // Custom serializer for LocalDateTime
    public static class LocalDateTimeKryoSerializer extends Serializer<LocalDateTime> implements Serializable {
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
}

