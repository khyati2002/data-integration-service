package com.salescode.dim.etl.validation;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.salescode.dim.jooq.generated.tables.pojos.ValidationRule;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractValidationRule<T extends CommonDataModel> implements Validation<T> {

    private ValidationRule validationRule;

}