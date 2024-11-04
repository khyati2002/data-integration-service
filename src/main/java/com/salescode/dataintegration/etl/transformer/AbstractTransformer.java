package com.salescode.dataintegration.etl.transformer;

import com.salescode.jooq.generated.tables.pojos.CkTransformerInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractTransformer<S, T> implements Transformer<S, T> {

    CkTransformerInfo transformerInfo;
}
