package com.salescode.dim.etl.transformation;

import com.salescode.dim.jooq.generated.tables.pojos.TransformerInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractTransformer<S, T> implements Transformer<S, T> {

    TransformerInfo transformerInfo;
}
