package com.salescode.dim.transformers;


import com.salescode.dim.interfaces.TypeAwareEtlStep;

public interface Transformer<S, T> extends TypeAwareEtlStep {

    T transform(S s);

    @Override
    default EtlType getSourceType() {
        return EtlType.TRANSFORMER;
    }
}
