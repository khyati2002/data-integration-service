package com.salescode.dim.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.List;
import java.util.Set;

public class KryoConfig {

    public static Kryo createKryo() {
        Kryo kryo = new Kryo();

        // Register custom serializers for immutable collections
        kryo.register(List.of().getClass(), new ImmutableListSerializer());
        kryo.register(Set.of().getClass(), new ImmutableSetSerializer());

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
}

