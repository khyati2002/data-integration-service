package com.salescode.dataintegration.etl.transformer;

import com.salescode.dataintegration.etl.interfaces.TypeAwareEtlStep;

public interface Transformer<S, T> extends TypeAwareEtlStep {

    T transform(S s);

    @Override
    default EtlType getSourceType() {
        return EtlType.TRANSFORMER;
    }
}
