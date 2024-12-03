package com.salescode.channelkart.services;

import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.utils.CdmDiffUtil;
import com.salescode.channelkart.utils.EntityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.ParameterizedType;
import java.util.List;

@Slf4j
public abstract class AbstractCDMService<T extends CommonDataModel> implements CommonDataModelService<T> {

    @Value("${spring.jpa.properties.hibernate.jdbc.fetch_size:500}")
    private int fetchSize;

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
            EntityUtils.copyProperties(cdmObject, dbRecord);
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
        log.info("Saving {}, cdm: {}", cdmObject.getClass().getSimpleName(), cdmObject);
        return cdmObject;
    }

    protected void setChanges(T previous, T current) {
        if (current.getOldModel() != null) {
            current.setOldModel(EntityUtils.deepClone(previous));
        }
    }


}
