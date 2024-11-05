package com.salescode.dataintegration.etl.validation;

import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.jooq.generated.tables.pojos.CkValidationRule;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractValidationRule<T extends CommonDataModel> implements Validation<T> {

    private CkValidationRule validationRule;

}