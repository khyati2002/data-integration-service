package com.salescode.dataintegration.etl.validation;

import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.validations.RuleInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractRule<T extends CommonDataModel> implements Validation<T> {

    private RuleInfo validationRule;

}