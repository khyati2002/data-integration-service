package com.salescode.dim.etl.validation;

import com.salescode.dim.etl.ValidationResult;
import com.salescode.dim.interfaces.TypeAwareEtlStep;

public interface Validation<T> extends TypeAwareEtlStep {

    ValidationResult apply(T cdm);

    @Override
    default EtlType getSourceType() {
        return EtlType.VALIDATION;
    }
}
