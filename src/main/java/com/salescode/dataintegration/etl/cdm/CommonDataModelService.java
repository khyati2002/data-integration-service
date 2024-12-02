package com.salescode.dataintegration.etl.cdm;

import com.salescode.channelkart.models.CommonDataModel;
import org.jooq.Record;
import org.springframework.util.IdGenerator;

import java.util.List;

public interface CommonDataModelService<T extends CommonDataModel> {

    public T refresh(T cdmObject);

    public String getKey(T cdmObject);

    public T save(T cdmObject);

    List<T> batchSave(Iterable<T> iterObj);
    List<T> batchSave(Iterable<T> iterObj,IdGenerator idGenerator);
}
