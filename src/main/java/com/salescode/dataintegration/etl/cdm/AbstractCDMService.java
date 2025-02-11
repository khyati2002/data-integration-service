package com.salescode.dataintegration.etl.cdm;

import com.salescode.channelkart.batch.BatchService;
import com.salescode.channelkart.batch.hash.BatchContainer;
import com.salescode.channelkart.converters.EnrichmentPhase;
import com.salescode.channelkart.exceptions.CustomRuntimeException;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.utils.*;
import com.salescode.dataintegration.etl.cdm.util.ServiceLocator;
import com.salescode.dataintegration.etl.enrichment.EnrichmentOperationResult;
import com.salescode.dataintegration.etl.enrichment.EnrichmentResult;
import com.salescode.dataintegration.etl.enrichment.service.DataEnrichmentService;
import com.salescode.jooq.generated.DefaultSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.jooq.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.jooq.Field;
import java.lang.reflect.ParameterizedType;
import org.springframework.core.env.Environment;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.salescode.jooq.generated.tables.CkOutletDetails.CK_OUTLET_DETAILS;
import static org.jooq.impl.DSL.name;

@Slf4j
public abstract class AbstractCDMService<T extends CommonDataModel> implements CommonDataModelService<T> {

    private Class<T> persistentClass;

    @Autowired
    private Environment env;

    @Autowired
    private DataEnrichmentService dataEnrichmentService;

    @Autowired
    private EntityUtils entityUtils;

    @Autowired
    private BatchService batchService;

    @Autowired
    private DSLContext dsl;

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
            cdmObject.setVersion(version);
            cdmObject.setId(dbRecord.getId());
            cdmObject.setHash(dbRecord.getHash());
            return cdmObject;
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
        final T inObject =cdmObject;
        String existingHash = cdmObject.getHash();
        TimerUtils.withTime("Time taken to generate Hash "+cdmObject.getClass().getName()+":"+cdmObject.getId(),
                () -> addHash(inObject));
        if (!cdmObject.forceHash() && cdmObject.canHash() && StringUtils.isNotEmpty(existingHash) && existingHash.equals(cdmObject.getHash())) {
            // no need to save this record because this hash is same
           // logger.info("Hash already present in database: {}", existingHash);
            return cdmObject;
        }
        T saved =null;
        if(isNativeBatchSave(cdmObject)) {
            throw new CustomRuntimeException("Native batch save failed");
//            saved=TimerUtils.withTime("Time taken to native finish native save operation "+cdmObject.getClass().getName()+":"+cdmObject.getId(), () -> nativeBatchSave(Arrays.asList(inObject),null).get(0));
        }else {
            cdmObject = fillCommonAttributes(cdmObject);
            final T object = cdmObject;
            saved =TimerUtils.withTime("Time taken to JPA save  "+cdmObject.getClass().getSimpleName()+":"+cdmObject.getId(),()-> {
                T object1 = preSaveEnrichment(object);
                Table<?> table = EntityUtils.getInstance().getDSLContextTable(object1.getClass());
                var record = dsl.newRecord(table,object1);
                T sreturn = (T) dsl.insertInto(table)
                             .set(record)
                             .onDuplicateKeyUpdate()
                             .set(record)
                             .returning() // Fetch the inserted/updated record
                             .fetchOne()
                             .into(object1.getClass());
                fixChanges(object1, sreturn);
                return sreturn;
            });
            TimerUtils.withTime("Time taken to execute entityListener" ,()-> {
//                if (finalCdmObject.isCreate()) {
//                    eventBroadcaster.broadcast(finalSaved,EntityOperation.INSERT);
//                } else {
//                    eventBroadcaster.broadcast(finalSaved,EntityOperation.UPDATE);
//                }
            });
            return saved;
        }
//            if(!cdmObject.isCreate()){
//                apiFilterAuthorizationManager.assertPermission( cdmObject);
//            }
    }

    private void fixChanges(T actualObjectWithChange, T dbSavedObject) {
        if (actualObjectWithChange != null && dbSavedObject != null) {
            dbSavedObject.setCreate(actualObjectWithChange.isCreate());
        }
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

    public T fillCommonAttributes(T cdm) {
        return TimerUtils.withTime("Time Taken to refresh entity of "+cdm.getClass().getSimpleName(), k->fillCommonAttributes(cdm, false, new HashSet<>(), null));
    }

    public List<T> batchSave(Iterable<T> iterObj) {
     return batchSave(iterObj,null);
    }

    public List<T> batchSave(Iterable<T> iterObj, IdGenerator idGenerator) {
        iterObj.forEach(this::preSaveEnrichment);
        if(isNativeBatchSave(iterObj)) {
            throw new UnsupportedOperationException("nativeBatchSave not supported");
           // return nativeBatchSave(iterObj, idGenerator);
        }else {
            BatchContainer<T> container = splitElements(iterObj, idGenerator);
            List<T> elementsToSaveAsList = container.getAllElementstoSave();
            saveAll(elementsToSaveAsList);
            //return ListUtils.union(container.getDuplicateElementsAsList(), savedData);
            return elementsToSaveAsList;

        }
    }

    private void saveAll(Iterable<T> items) {
//        items.forEach(element-> apiFilterAuthorizationManager.assertPermission(element))
        List<T> savedItems = new ArrayList<>();
        List<T> itemsToInsert = new ArrayList<>();

        for (T item: items) {
            itemsToInsert.add(item);
        }

      Class<? extends CommonDataModel> clazz = itemsToInsert.get(0).getClass();
     // TableImpl table = EntityUtils.getInstance().getDSLContextTable(itemsToInsert.get(0).getClass());

        String tablename = EntityUtils.getInstance().getTableName(clazz);

        Table<?> table = new DefaultSchema().getTable(tablename);


        List<Field<?>> fields = getTableFields(dsl, table);
        List<Query> insertQueries = new ArrayList<>();
        for (T item : items) {
         //   Object[] values = getFieldValues(item, fields);

            Object[] values = getFieldValues(item, fields);

            // Create the base insert query
            Insert<?> insertQuery = dsl.insertInto(table)
                    .columns(fields)
                    .values(values);

            // Convert the query to a SQL string and append ON DUPLICATE KEY UPDATE manually
            String sql = insertQuery.getSQL() + " ON DUPLICATE KEY UPDATE ";

            // Build the update part dynamically
            List<String> updateClauses = new ArrayList<>();
            for (Field<?> field : fields) {
                updateClauses.add(field.getName() + " = VALUES(" + field.getName() + ")");
            }

            sql += String.join(", ", updateClauses);

            // Execute the raw SQL query
            insertQueries.add(dsl.query(sql, insertQuery.getBindValues().toArray()));
//            insertQueries.add(
//                    dsl.insertInto(table)
//                            .columns(fields)
//                            .values(values)
//                            .onDuplicateKeyUpdate()
//                            .set(fields, values);
//            );


        }
        dsl.batch(insertQueries).execute();
    }

    private List<Field<?>> getTableFields(DSLContext dslContext, Table<?> table) {
        return Arrays.asList(table.fields()); // Get table column fields dynamically
    }

    private Object[] getFieldValues(Object entity, List<Field<?>> tableFields) {
        List<Object> values = new ArrayList<>();

        for (Field<?> field : tableFields) {
            try {
                java.lang.reflect.Field entityField = entity.getClass().getDeclaredField(field.getName());
                entityField.setAccessible(true);
                values.add(entityField.get(entity));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                values.add(null); // Handle missing or inaccessible fields gracefully
            }
        }
        return values.toArray();
    }

    private BatchContainer<T> splitElements(Iterable<T> elements, IdGenerator generator) {
        List<T> newRecords = new ArrayList<>();
        List<T> existingRecords = new ArrayList<>();
        Map<String, T> existingRecordsWithHash = new HashMap<>();
        for (T element : elements) {
            fillCommonAttributes(element, generator);
            if (element.isCreate()) {
                addHash(element);
                newRecords.add(element);
            } else {
                addHash(element);
                existingRecords.add(element);
            }
        }
        if (existingRecords.isEmpty()) {
            BatchContainer<T> batchContainer = new BatchContainer<>();
            batchContainer.setElementsToInsert(newRecords);
            return batchContainer;
        }

        if(isHashableType(elements)) {
            existingRecords.forEach(rec->existingRecordsWithHash.put((rec).getHash(), rec));
            BatchContainer<T> batchContainer = batchService.splitElements(existingRecordsWithHash);
            batchContainer.setElementsToInsert(newRecords);
          //  logger.debug("BatchSave: duplicate records:{}, records to save/update :{}", batchContainer.getDuplicateElements().size(), batchContainer.getAllElementstoSave().size());
            return batchContainer;
        }else {
            BatchContainer<T> batchContainer=new BatchContainer<>();
            batchContainer.setElementsToInsert(newRecords);
            batchContainer.setElementsToUpdate(existingRecords);
            return batchContainer;
        }
    }

    private boolean isHashableType(Iterable<T> elements) {
        Iterator<T> iterator = elements.iterator();
        if (iterator.hasNext()) {
            T element = iterator.next();
            return element.canHash();
        }
        return false;
    }

    protected void addHash(CommonDataModel model) {
        if (model.canHash()) {
            batchService.addHashIfPresent( model);
        }
    }

    public T fillCommonAttributes(T cdm, IdGenerator idGenerator) {
        return TimerUtils.withTime("Time Taken to refresh entity of "+cdm.getClass().getSimpleName(), k->fillCommonAttributes(cdm, false, new HashSet<>(), idGenerator));
    }

    @SuppressWarnings("unchecked")
    public T fillCommonAttributes(T cdm, boolean createOnly,Set<String> visitedTree, IdGenerator idGenerator) {
        boolean fillModifyAttributes = !createOnly || cdm.getId() == null;
        if (cdm.getId() == null) {
            if (idGenerator != null) {
                cdm.setId(idGenerator.getId(cdm));
            } else {
                //noinspection deprecation
              //  cdm.setId(entityUtils.generateId(cdm));
            }
        }
//        TimerUtils.withTime("Time taken to find changed value", () -> deltaAnalyzer.findAndSetDeltaChanges(cdm));
//
//        setOperationType(cdm);
//
//        if (cdm.getCreationTime() == null) {
//            cdm.setCreationTime(Calendar.getInstance().getTime());
//        }
//        if(cdm.getActiveStatus() == null){
//            cdm.setActiveStatus(ActiveStatus.ACTIVE);
//        }
//
////        if (cdm instanceof TransactionDataModel) {
////            ((TransactionDataModel) cdm).setSystemTime(Calendar.getInstance().getTime());
////        }
//
//        if (cdm.getCreatedBy() == null) {
//            cdm.setCreatedBy(SecurityContextUtils.getPrincipal());
//        }
//
//        if (!DataSourceUtils.isDefaultDataSource(SecurityContextUtils.getLob())) {
//            cdm.setLob(SecurityContextUtils.getLob());
//        }
//
//        if (fillModifyAttributes) {
//            cdm.setLastModifiedTime(Calendar.getInstance().getTime());
//            cdm.setModifiedBy(SecurityContextUtils.getPrincipal());
//        }
//        processAggregations(cdm, visitedTree, idGenerator);
        return cdm;
    }

    private void processAggregations(T cdm, Set<String> visitedTree, IdGenerator idGenerator) {
//        String key = cdm.getId();
//        if (!visitedTree.contains(key)) {
//            visitedTree.add(key);
//            Field[] fields = cdm.getClass().getDeclaredFields();
//            for (Field field : fields) {
//                Type returnType = field.getGenericType();
//                if (returnType instanceof ParameterizedType) {
//                    ParameterizedType type = (ParameterizedType) returnType;
//                    Type[] typeArguments = type.getActualTypeArguments();
//                    for (Type typeArgument : typeArguments) {
//                        visitParameterizedCDMType(cdm,field,typeArgument,visitedTree,idGenerator);
//                    }
//                } else if (CommonDataModel.class.isAssignableFrom((Class<?>) returnType)) {
//                    visitCDMType(cdm,field,returnType,visitedTree,idGenerator);
//                }
//            }
//        }
    }



    private boolean isNativeBatchSave(Iterable<T> elements) {
        Iterator<T> iterator = elements.iterator();
        if (iterator.hasNext()) {
            T element = iterator.next();
            return isNativeBatchSave(element);
        }
        return false;
    }

    private boolean isNativeBatchSave(T element) {
        return Boolean.parseBoolean(env.getProperty("native.batch.save."+element.getClass().getSimpleName(), "false"));
    }

    private T preSaveEnrichment(T cdm){
       EnrichmentOperationResult er = dataEnrichmentService.enrich(cdm, EnrichmentPhase.PRE_SAVE);
        if(!er.getStatus().equals(EnrichmentResult.Status.OK)) {
            throw new RuntimeException((ObjectUtils.isNotEmpty(er.getEnrichmentResults()))?er.getEnrichmentResults().get(0).getMessage():
                    "Some error occured with pre-enrichment while storing "+cdm.toString());
        }
        return (T) (ObjectUtils.isNotEmpty(er.getEnrichedData())?er.getEnrichedData().get(0):cdm);
    }

}
