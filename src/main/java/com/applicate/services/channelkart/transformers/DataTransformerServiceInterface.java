package com.applicate.services.channelkart.transformers;

public interface DataTransformerServiceInterface<S, T> {
    T transformByName(String var1, String var2, S var3);

    T transformById(String var1, String var2, S var3);
}