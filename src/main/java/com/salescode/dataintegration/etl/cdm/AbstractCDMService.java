package com.salescode.dataintegration.etl.cdm;

import com.salescode.channelkart.converters.ActiveStatus;
import com.salescode.channelkart.converters.EnrichmentPhase;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.utils.CdmDiffUtil;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.channelkart.utils.SecurityContextUtils;
import com.salescode.dataintegration.etl.cdm.util.ServiceLocator;
import com.salescode.dataintegration.etl.enrichment.EnrichmentOperationResult;
import com.salescode.dataintegration.etl.enrichment.EnrichmentResult;
import com.salescode.dataintegration.etl.enrichment.service.DataEnrichmentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jooq.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.IdGenerator;

import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@SuppressWarnings("unchecked")
public abstract class AbstractCDMService<T extends CommonDataModel> implements CommonDataModelService<T> {

    private Class<T> persistentClass;

    protected final DSLContext dslContext;

    @Autowired
    private DataEnrichmentService enrichmentService;

    public AbstractCDMService(DSLContext dslContext) {
        this.dslContext = dslContext;
        if (getClass().getGenericSuperclass() instanceof ParameterizedType) {
            persistentClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            ServiceLocator.register(persistentClass, this);
        }
    }

    protected abstract Table<? extends Record> getTable();




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
        cdmObject = fillCommonAttributes(cdmObject);
        log.info("Saving {}, cdm: {}", cdmObject.getClass().getSimpleName(), cdmObject);
        cdmObject = preSaveEnrichment(cdmObject);
        EntityUtils.getInstance().getDSLContextTable(cdmObject.getClass());
        final Record record = dslContext.newRecord(getTable(), cdmObject);
        for (Field<?> field : getTable().fields()) {
            if (record.get(field) == null) {
                if (!field.getDataType().nullable()) {
                    throw new IllegalArgumentException("Field " + field.getName() + " cannot be null");
                }
                record.set(field, null);
            }
        }
        dslContext.insertInto(getTable()).set(record).onDuplicateKeyUpdate().set(record).execute();
        return cdmObject;
    }

    @Override
    @Transactional
    public List<T> batchSave(Iterable<T> iterObj) {
        return batchSave(iterObj, null);
    }

    public List<T> batchSave(Iterable<T> iterObj, IdGenerator idGenerator) {
        List<T> savedData = new ArrayList<>();
        List<T> duplicateElements = new ArrayList<>();

        // Pre-save enrichment
        for (T element : iterObj) {
            preSaveEnrichment(element);

            // Handle duplication checks based on the `IdGenerator` or your logic
            if (isDuplicate(element, idGenerator)) {
                duplicateElements.add(element);
            } else {
                // Use the provided `save` method for each element
                T savedElement = save(element);
                savedData.add(savedElement);
            }
        }

        // Combine duplicates and saved data
        return ListUtils.union(duplicateElements, savedData);
    }
    private boolean isDuplicate(T element, IdGenerator idGenerator) {
        // Implement your duplication check logic here
        return false;
    }


    private com.applicate.services.channelkart.batch.BatchContainer<T> splitElements(Iterable<T> elements, IdGenerator generator) {
        List<T> newRecords = new ArrayList<>();
        List<T> existingRecords = new ArrayList<>();
        Map<String, T> existingRecordsWithHash = new HashMap<>();
        for (T element : elements) {
//            fillCommonAttributes(element, generator);
            if (element.isCreate()) {
//                addHash(element);
                newRecords.add(element);
            } else {
//                addHash(element);
                existingRecords.add(element);
            }
        }
        if (existingRecords.isEmpty()) {
            com.applicate.services.channelkart.batch.BatchContainer<T> batchContainer = new com.applicate.services.channelkart.batch.BatchContainer<>();
            batchContainer.setElementsToInsert(newRecords);
            return batchContainer;
        }


        com.applicate.services.channelkart.batch.BatchContainer<T> batchContainer=new com.applicate.services.channelkart.batch.BatchContainer<>();
        batchContainer.setElementsToInsert(newRecords);
        batchContainer.setElementsToUpdate(existingRecords);
        return batchContainer;

    }


    public T fillCommonAttributes(T cdm) {
        return fillCommonAttributes(cdm, false, new HashSet<>());
    }

    public T fillCommonAttributes(T cdm, boolean createOnly, Set<String> visitedTree) {
        boolean fillModifyAttributes = !createOnly || cdm.getId() == null;
        if (cdm.getId() == null) {
            cdm.setId(UUID.randomUUID().toString());
        }
        if (cdm.getVersion() == null) {
            cdm.setVersion(0);
        } else {
            cdm.setVersion(cdm.getVersion() + 1);
        }

        if (cdm.getCreationTime() == null) {
            cdm.setCreationTime(Calendar.getInstance().getTime());
        }
        if (cdm.getActiveStatus() == null) {
            cdm.setActiveStatus(ActiveStatus.ACTIVE);
        }
        if (cdm.getCreatedBy() == null) {
            cdm.setCreatedBy(SecurityContextUtils.getPrincipal());
        }
        cdm.setLob(SecurityContextUtils.getLob());

        if (fillModifyAttributes) {
            cdm.setLastModifiedTime(Calendar.getInstance().getTime());
            cdm.setModifiedBy(SecurityContextUtils.getPrincipal());
        }
        return cdm;
    }

    protected void setChanges(T previous, T current){
        if (current.getOldModel() != null) {
            current.setOldModel( EntityUtils.deepClone(previous));
        }
    }
    public List<T> refresh(List<T> cdmobjects) {
        if (cdmobjects != null && !cdmobjects.isEmpty()) {
            Class<T> clazz = (Class<T>) cdmobjects.iterator().next().getClass();
//            List<List<T>> cdmbatch = ListUtils.partition(cdmobjects, fetchSize);
//            String str = "Time taken to refresh :" + cdmobjects.size() + ", enitity :" + clazz.getSimpleName();
//            return cdmbatch
//                    .stream()
//                    .map(l -> (List<T>) TimerUtils.withTime(str, s -> batchRefresh(clazz, l)))
//                    .flatMap(List::stream)
//                    .collect(Collectors.toList());
        }
        return cdmobjects;
    }


    private T preSaveEnrichment(T cdm){
        EnrichmentOperationResult er = enrichmentService.enrich(cdm, EnrichmentPhase.PRE_SAVE);
        if(!er.getStatus().equals(EnrichmentResult.Status.OK)) {
            throw new RuntimeException((ObjectUtils.isNotEmpty(er.getEnrichmentResults()))?er.getEnrichmentResults().get(0).getMessage():
                    "Some error occured with pre-enrichment while storing "+cdm.toString());
        }
        return (T) (ObjectUtils.isNotEmpty(er.getEnrichedData())?er.getEnrichedData().get(0):cdm);
    }





}
