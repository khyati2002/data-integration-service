package com.salescode.dataintegration.etl.cdm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.channelkart.converters.ActiveStatus;
import com.salescode.channelkart.converters.EnrichmentPhase;
import com.salescode.channelkart.exceptions.CustomRuntimeException;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.utils.*;
import com.salescode.dataintegration.etl.cdm.util.ServiceLocator;
import com.salescode.dataintegration.etl.enrichment.EnrichmentOperationResult;
import com.salescode.dataintegration.etl.enrichment.EnrichmentResult;
import com.salescode.dataintegration.etl.enrichment.service.DataEnrichmentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.jooq.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.jooq.Field;
import java.lang.reflect.ParameterizedType;
import org.springframework.core.env.Environment;

import java.util.*;


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
            int version = dbRecord.getVersion();
            cdmObject.setVersion(version);
            cdmObject.setId(dbRecord.getId());
            cdmObject.setHash(dbRecord.getHash());
            return cdmObject;
        }
        cdmObject.setCreate(true);
        return cdmObject;
    }


    public T refresh(T cdmObject,Map<String,T> recordsMap) {
        T dbRecord = CdmDiffUtil.withOldModel(() -> (T) EntityUtils.getInstance().findRecords(cdmObject.getClass(), cdmObject,recordsMap));
        if (dbRecord != null) {
            int version = dbRecord.getVersion()+1;
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

    @Override
    public List<T> batchSave(Iterable<T> iterObj) {
     return batchSave(iterObj,null);
    }


    public List<T> batchSave(Map<String, T> iterObj) {
        saveAll(iterObj);
        return List.of();
    }

    public List<T> batchSave(Iterable<T> iterObj, IdGenerator idGenerator) {
        if(isNativeBatchSave(iterObj)) {
            throw new UnsupportedOperationException("nativeBatchSave not supported");
           // return nativeBatchSave(iterObj, idGenerator);
        }else {
//            BatchContainer<T> container = splitElements(iterObj, idGenerator);
//            List<T> elementsToSaveAsList = container.getAllElementstoSave();
            saveAll(iterObj);
            //return ListUtils.union(container.getDuplicateElementsAsList(), savedData);
            return List.of();

        }
    }

    private void saveAll(Iterable<T> items) {
//        items.forEach(element-> apiFilterAuthorizationManager.assertPermission(element))
        List<T> savedItems = new ArrayList<>();
        List<T> itemsToInsert = new ArrayList<>();
        int count = 0;
        for (T item: items) {
            final T inObject = item;
            String existingHash = item.getHash();
            TimerUtils.withTime("Time taken to generate Hash "+item.getClass().getName()+":"+item.getId(),
                    () -> addHash(inObject));
            if (!item.forceHash() && item.canHash() && StringUtils.isNotEmpty(existingHash) && existingHash.equals(item.getHash())) {
                // no need to save this record because this hash is same
                // logger.info("Hash already present in database: {}", existingHash);
            }
            else {
                T object = fillCommonAttributes(item);
                T object1 = preSaveEnrichment(object);
                itemsToInsert.add(object1);
                count++;
            }
        }
      if(count < 1) return;

      Class<? extends CommonDataModel> clazz = itemsToInsert.get(0).getClass();
     // TableImpl table = EntityUtils.getInstance().getDSLContextTable(itemsToInsert.get(0).getClass());

        String tablename = EntityUtils.getInstance().getTableName(clazz);

        Table<?> table = EntityUtils.getInstance().getDSLContextTable(clazz);


        List<Field<?>> fields = getTableFields(dsl, table);
        List<Query> insertQueries = new ArrayList<>();
        for (T item : items) {
         //   Object[] values = getFieldValues(item, fields);
            if(item.getLastModifiedTime() == null) item.setLastModifiedTime(new Date());
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
            List<Object> obj = insertQuery.getBindValues();

            List<Object> bindValues = new ArrayList<>();
            ObjectMapper objectMapper = new ObjectMapper();

            for (Object value : insertQuery.getBindValues()) {
                if (value instanceof ObjectNode) {
                    try {
                        value = objectMapper.writeValueAsString(value);  // Convert to JSON string
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Error converting ObjectNode to JSON String", e);
                    }
                }
                bindValues.add(value);
            }

            insertQueries.add(dsl.query(sql, bindValues.toArray()));

        }
        dsl.batch(insertQueries).execute();
    }


    private Iterable<T> saveAll(Map<String,T> items) {
        List<T> savedItems = new ArrayList<>();
        List<T> itemsToInsert = new ArrayList<>();
        int count = 0;
        for (T item: items.values()) {
                itemsToInsert.add(item);
                count++;
        }
        saveAll(itemsToInsert);
        return itemsToInsert;

    }

    private java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        // Try exact match first
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            // Try camelCase version
            String camelCase = toCamelCase(fieldName);
            try {
                return clazz.getDeclaredField(camelCase);
            } catch (NoSuchFieldException ex) {
                // Check superclass if field not found
                Class<?> superClass = clazz.getSuperclass();
                if (superClass != null && !superClass.equals(Object.class)) {
                    return findField(superClass, fieldName);
                }
                log.debug("Field not found: {} (or camelCase: {})", fieldName, camelCase);
                return null;
            }
        }
    }

    private String toCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;

        for (int i = 0; i < str.length(); i++) {
            char currentChar = str.charAt(i);
            if (currentChar == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    result.append(Character.toUpperCase(currentChar));
                    nextUpper = false;
                } else {
                    result.append(i == 0 ? Character.toLowerCase(currentChar) : currentChar);
                }
            }
        }

        return result.toString();
    }

    private List<Field<?>> getTableFields(DSLContext dslContext, Table<?> table) {
        return Arrays.asList(table.fields()); // Get table column fields dynamically
    }

    private Object[] getFieldValues(Object entity, List<Field<?>> tableFields) {
        List<Object> values = new ArrayList<>();

        for (Field<?> field : tableFields) {
            try {
                java.lang.reflect.Field entityField = findField(entity.getClass(),field.getName());
                entityField.setAccessible(true);
                values.add(entityField.get(entity));
            } catch (IllegalAccessException e) {
                values.add(null); // Handle missing or inaccessible fields gracefully
            }
        }
        return values.toArray();
    }

    protected void addHash(CommonDataModel model) {
        if (model.canHash()) {
            addHashIfPresent(model);
        }
    }
    public <T extends CommonDataModel> T addHashIfPresent(T model) {
        String hash = model.hash();
        model.setHash(hash);
        return model;
    }

    private void setOperationType(CommonDataModel model) {
        Integer version = model.getVersion();
        if(version==null) {
            model.setCreate(true);
            model.setVersion(0);
        }
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

        setOperationType(cdm);

        if (cdm.getCreationTime() == null) {
            cdm.setCreationTime(Calendar.getInstance().getTime());
        }
        if(cdm.getActiveStatus() == null){
            cdm.setActiveStatus(ActiveStatus.ACTIVE);
        }

        if (cdm.getCreatedBy() == null) {
            cdm.setCreatedBy(SecurityContextUtils.getPrincipal());
        }

        if (cdm.getLob() == null) {
            cdm.setLob(SecurityContextUtils.getLob());
        }

        if (fillModifyAttributes) {
            cdm.setLastModifiedTime(Calendar.getInstance().getTime());
            cdm.setModifiedBy(SecurityContextUtils.getPrincipal());
        }

        return cdm;
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
