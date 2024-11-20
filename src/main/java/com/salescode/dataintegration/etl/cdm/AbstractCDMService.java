package com.salescode.dataintegration.etl.cdm;

import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.utils.CdmDiffUtil;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.dataintegration.etl.cdm.util.ServiceLocator;

import java.lang.reflect.ParameterizedType;
import java.util.List;

public abstract class AbstractCDMService<T extends CommonDataModel> implements CommonDataModelService<T> {

    private Class<T> persistentClass;

    public AbstractCDMService() {
        if (getClass().getGenericSuperclass() instanceof ParameterizedType) {
            persistentClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            ServiceLocator.register(persistentClass, this);
        }
    }


    @Override
    public T refresh(T cdmObject) {
        T dbRecord = CdmDiffUtil.withOldModel(() -> (T) EntityUtils.getInstance().findRecords(cdmObject.getClass(), cdmObject));
        if (dbRecord != null) {
            cdmObject.setOldModel(dbRecord.getOldModel());
            int version = dbRecord.getVersion();
            EntityUtils.getInstance().copyProperties(cdmObject, dbRecord);
            dbRecord.setVersion(version);
            return dbRecord;
        }
        cdmObject.setCreate(true);
        return cdmObject;
    }

    @Override
    public String getKey(T cdmObject) {
        List<T> resultSet = EntityUtils.getInstance().getGetKeyId(cdmObject);
        if (!resultSet.isEmpty()) {
            return resultSet.get(0).getId();
        } else {
            return EntityUtils.getInstance().generateId(cdmObject, false);
        }
    }

    @Override
    public T save(T cdmObject) {
        return cdmObject;
    }

}
