package com.salescode.dataintegration.etl.transformer;

import com.salescode.channelkart.transformers.TransformerInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractTransformer<S, T> implements Transformer<S, T> {

    TransformerInfo transformerInfo;
}
