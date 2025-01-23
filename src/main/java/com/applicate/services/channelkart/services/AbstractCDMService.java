/*
 * Copyright (c) Applicate 2021. All rights reserved.
 * Use is subject to license terms.
 */
package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.abstractdatasource.DatabaseProfileRegistry;
import com.applicate.services.channelkart.batch.BatchService;
import com.applicate.services.channelkart.batch.hash.BatchContainer;
import com.applicate.services.channelkart.client.properties.PropertyDefinition;
import com.applicate.services.channelkart.client.properties.PropertyRegistry;
import com.applicate.services.channelkart.enrichments.*;
import com.applicate.services.channelkart.exceptions.CustomRuntimeException;
import com.applicate.services.channelkart.exceptions.ResourceNotFoundException;
import com.applicate.services.channelkart.exceptions.SystemRuntimeException;
import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.models.Role;
import com.applicate.services.channelkart.models.User;
import com.applicate.services.channelkart.models.diff.Change;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.repository.CommonJpaRepository;
import com.applicate.services.channelkart.security.SecurityContextUtils;
import com.applicate.services.channelkart.services.enums.EntityOperation;
import com.applicate.services.channelkart.services.enums.OperationType;
import com.applicate.services.channelkart.utils.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.collection.internal.PersistentBag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class AbstractCDMService<T extends CommonDataModel> implements CommonDataModelService<T> {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public  CommonJpaRepository<T, String> getRepository() {
        return repository;
    }

    protected CommonJpaRepository<T, String> repository;

    @Autowired
    protected EntityUtils entityUtils;

    @Autowired
    private BatchService batchService;

    @Autowired
    private EntityManager enityManager;

    @Autowired
    private Environment env;

    @Autowired
    private DataEnrichmentService enrichmentService;

    @Autowired
    private ChangeEventBroadcaster eventBroadcaster;

    @Autowired
    private PropertyRegistry propertyService;

    private static final List<Class<Role>> restrictedList = Arrays.asList(Role.class);

    @Value("${spring.jpa.properties.hibernate.jdbc.fetch_size}")
    private int fetchSize;

    private Class<?> persistentClass;

    public AbstractCDMService(CommonJpaRepository<T, String> repository) {
        this.repository = repository;
        if(getClass().getGenericSuperclass() instanceof ParameterizedType) {
            persistentClass = (Class<?>)
                    ((ParameterizedType) getClass().getGenericSuperclass())
                            .getActualTypeArguments()[0];
            ServiceLocator.register(persistentClass, (CommonDataModelService<?>) this);
        }
    }

    @Override
    public List<T> findAll() {
        return repository.findAll();
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public T save(T cdmObject) {
        final T inObject =cdmObject;
        String existingHash = cdmObject.getHash();
        TimerUtils.withTime("Time taken to generate Hash "+cdmObject.getClass().getName()+":"+cdmObject.getId(),
                () -> addHash(inObject));
        if (!cdmObject.forceHash() && cdmObject.canHash() && StringUtils.isNotEmpty(existingHash) && existingHash.equals(cdmObject.getHash())) {
            // no need to save this record because this hash is same
            logger.info("Hash already present in database: {}", existingHash);
            return cdmObject;
        }
        T saved =null;
        if(isNativeBatchSave(cdmObject)) {
            throw new CustomRuntimeException("Native batch save failed");
//            saved=TimerUtils.withTime("Time taken to native finish native save operation "+cdmObject.getClass().getName()+":"+cdmObject.getId(), () -> nativeBatchSave(Arrays.asList(inObject),null).get(0));
        }else {
            cdmObject = fillCommonAttributes(cdmObject);
//            if(!cdmObject.isCreate()){
//                apiFilterAuthorizationManager.assertPermission( cdmObject);
//            }
            final T object = cdmObject;
            saved =TimerUtils.withTime("Time taken to JPA save  "+cdmObject.getClass().getSimpleName()+":"+cdmObject.getId(),()-> {
                T object1 = preSaveEnrichment(object);
                T sreturn = repository.save(object1);
                fixChanges( object1, sreturn);
                return sreturn;
            });
            repository.flush();
        }
        T finalCdmObject = cdmObject;
        T finalSaved = saved;
        TimerUtils.withTime("Time taken to execute entityListener" ,()-> {
            if (finalCdmObject.isCreate()) {
                eventBroadcaster.broadcast(finalSaved,EntityOperation.INSERT);
            } else {
                eventBroadcaster.broadcast(finalSaved,EntityOperation.UPDATE);
            }
        });
        return saved;
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public T save(T cdmObject, OperationType type) {
        return save(cdmObject);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public List<T> saveForList(T cdmObject, OperationType type) {
        return List.of(save(cdmObject,type));
    }

    @Override
    public T findById(String id) {
        Optional<T> entity = repository.findById(id);
        return entity.orElse(null);
    }

    @Override
    public void deleteById(String id) {
        deleteById(id,false);
    }

    @Override
    public void deleteById(String id,boolean failsOnEmptyRecord){
        Optional<T> entityToDelete = repository.findById(id);
        if(entityToDelete.isEmpty() && failsOnEmptyRecord){
            throw new ResourceNotFoundException("Record with entity id {}, not present in table. Please verify input data.",id);
        }else if(entityToDelete.isPresent()){
            repository.deleteById(id);
            eventBroadcaster.broadcast(entityToDelete.get(), EntityOperation.DELETE);
        }
    }


    public T fillCommonAttributes(T cdm) {
        return TimerUtils.withTime("Time Taken to refresh entity of "+cdm.getClass().getSimpleName(), k->fillCommonAttributes(cdm, false, new HashSet<>(), null));
    }

    public T fillCommonAttributes(T cdm, IdGenerator idGenerator) {
        return TimerUtils.withTime("Time Taken to refresh entity of "+cdm.getClass().getSimpleName(), k->fillCommonAttributes(cdm, false, new HashSet<>(), idGenerator));
    }

    protected void addHash(CommonDataModel model) {
        if (model.canHash()) {
            batchService.addHashIfPresent( model);
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
                cdm.setId(entityUtils.generateId(cdm));
            }
        }
//        TimerUtils.withTime("Time taken to find changed value", () -> deltaAnalyzer.findAndSetDeltaChanges(cdm));

        setOperationType(cdm);

        if (cdm.getCreationTime() == null) {
            cdm.setCreationTime(Calendar.getInstance().getTime());
        }
        if(cdm.getActiveStatus() == null){
            cdm.setActiveStatus(ActiveStatus.ACTIVE);
        }

//        if (cdm instanceof TransactionDataModel) {
//            ((TransactionDataModel) cdm).setSystemTime(Calendar.getInstance().getTime());
//        }

        if (cdm.getCreatedBy() == null) {
            cdm.setCreatedBy(SecurityContextUtils.getPrincipal());
        }

        if (!DataSourceUtils.isDefaultDataSource(SecurityContextUtils.getLob())) {
            cdm.setLob(SecurityContextUtils.getLob());
        }

        if (fillModifyAttributes) {
            cdm.setLastModifiedTime(Calendar.getInstance().getTime());
            cdm.setModifiedBy(SecurityContextUtils.getPrincipal());
        }
        processAggregations(cdm, visitedTree, idGenerator);
        return cdm;
    }

    private void processAggregations(T cdm, Set<String> visitedTree, IdGenerator idGenerator) {
        String key = cdm.getId();
        if (!visitedTree.contains(key)) {
            visitedTree.add(key);
            Field[] fields = cdm.getClass().getDeclaredFields();
            for (Field field : fields) {
                Type returnType = field.getGenericType();
                if (returnType instanceof ParameterizedType) {
                    ParameterizedType type = (ParameterizedType) returnType;
                    Type[] typeArguments = type.getActualTypeArguments();
                    for (Type typeArgument : typeArguments) {
                        visitParameterizedCDMType(cdm,field,typeArgument,visitedTree,idGenerator);
                    }
                } else if (CommonDataModel.class.isAssignableFrom((Class<?>) returnType)) {
                    visitCDMType(cdm,field,returnType,visitedTree,idGenerator);
                }
            }
        }
    }

    private void visitParameterizedCDMType(T cdm,Field field,Type typeArgument,Set<String> visitedTree, IdGenerator idGenerator){
        if (typeArgument instanceof Class
                && CommonDataModel.class.isAssignableFrom((Class<?>) typeArgument)) {
            try {
                if (!restrictedList.contains(typeArgument)) {
                    field.setAccessible(true);
                    Optional<Collection<T>> tcdm = Optional.ofNullable((Collection<T>) field.get(cdm));
                    if (NullUtils.isNotNull(field.get(cdm)) && field.get(cdm).getClass() != PersistentBag.class) {
                        tcdm.ifPresent(oc -> oc.forEach(t -> fillCommonAttributes(t, true, visitedTree, idGenerator)));
                    }
                }
            } catch (Exception e) {
                logger.error("stacktrace", e);
            }
        }
    }
    private void visitCDMType(T cdm,Field field,Type returnType,Set<String> visitedTree, IdGenerator idGenerator){
        try {
            if (!restrictedList.contains(returnType)) {
                field.setAccessible(true);
                Optional<T> cdmOpt = Optional.ofNullable((T) field.get(cdm));
                cdmOpt.ifPresent(cdmAssociation -> fillCommonAttributes(cdmAssociation, true, visitedTree, idGenerator));
            }
        } catch (Exception e) {
            logger.error("stacktrace", e);
        }
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public List<T> batchSave(Iterable<T> iterObj) {
        return batchSave(iterObj,null);
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
            logger.debug("BatchSave: duplicate records:{}, records to save/update :{}", batchContainer.getDuplicateElements().size(), batchContainer.getAllElementstoSave().size());
            return batchContainer;
        }else {
            BatchContainer<T> batchContainer=new BatchContainer<>();
            batchContainer.setElementsToInsert(newRecords);
            batchContainer.setElementsToUpdate(existingRecords);
            return batchContainer;
        }
    }

    private List<T> saveAll(Iterable<T> items) {
//        items.forEach(element-> apiFilterAuthorizationManager.assertPermission(element));
        List<T> saved = this.repository.saveAll(items);
        fixChanges((List<T>)items, saved); //hack to populate persist changes after putting to db, should find a better place to do this.
        repository.flush();
        notifyBatchSave(saved);
        return saved;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public List<T> batchSave(Iterable<T> iterObj, IdGenerator idGenerator) {
        iterObj.forEach(this::preSaveEnrichment);
        if(isNativeBatchSave(iterObj)) {
            throw new UnsupportedOperationException("nativeBatchSave not supported");
//            return nativeBatchSave(iterObj, idGenerator);
        }else {
            BatchContainer<T> container = splitElements(iterObj, idGenerator);
            List<T> elementsToSaveAsList = container.getAllElementstoSave();
            List<T> savedData = saveAll(elementsToSaveAsList);
            return ListUtils.union(container.getDuplicateElementsAsList(), savedData);

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

    private void notifyBatchSave(List<? extends CommonDataModel> models) {
        Map<EntityOperation, ? extends List<? extends CommonDataModel>> objects = models.stream().collect(Collectors.groupingBy(this::findOperation, Collectors.toList()));
        List<? extends CommonDataModel> insertedItems = objects.get(EntityOperation.INSERT);
        List<? extends CommonDataModel> updatedItems = objects.get(EntityOperation.UPDATE);
        if (insertedItems != null) {
            eventBroadcaster.broadcast(insertedItems,EntityOperation.INSERT);
        }
        if (updatedItems != null) {
            eventBroadcaster.broadcast(updatedItems,EntityOperation.UPDATE);
        }
    }
    private EntityOperation findOperation(CommonDataModel model) {
        return model.isCreate()? EntityOperation.INSERT : EntityOperation.UPDATE;
    }
    private void setOperationType(CommonDataModel model) {
        Integer version = model.getVersion();
        if(version==null) {
            model.setCreate(true);
            model.setVersion(0);
        }
    }


    @Override
    @SuppressWarnings("unchecked")
    public T refresh(T cdmObject) {
        T dbRecord = CdmDiffUtil.withOldModel(() -> (T) entityUtils.findRecords(cdmObject.getClass(), cdmObject));
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
    public List<T> refresh(List<T> cdmobjects) {
        if (cdmobjects != null && !cdmobjects.isEmpty()) {
            Class<T> clazz = (Class<T>) cdmobjects.iterator().next().getClass();
            List<List<T>> cdmbatch = ListUtils.partition(cdmobjects, fetchSize);
            String str = "Time taken to refresh :" + cdmobjects.size() + ", enitity :" + clazz.getSimpleName();
            return cdmbatch
                    .stream()
                    .map(l -> (List<T>) TimerUtils.withTime(str, s -> batchRefresh(clazz, l)))
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }
        return cdmobjects;
    }

    /**
     *
     * Add changes from old objects to new objects.
     * Currently only adds changes in primitive member variables.
     *
     * @param previous from db
     * @param current with changes
     */
    protected void setChanges(T previous, T current){
        if (current.getOldModel() != null) {
            current.setOldModel( EntityUtils.deepClone(previous));
        }
    }

    private void fixChanges(List<T> actualObjectWithChange, List<T> dbSavedObject){
        if(actualObjectWithChange!=null && dbSavedObject!=null && actualObjectWithChange.size() == dbSavedObject.size()){
            for(int i=0; i< dbSavedObject.size();i++){
                fixChanges(actualObjectWithChange.get(i), dbSavedObject.get(i));
            }
        }else {
            logger.warn("null or empty objects passed to add changes.. , ignoring");
        }
    }

    private void fixChanges(T actualObjectWithChange, T dbSavedObject) {
        if (actualObjectWithChange != null && dbSavedObject != null) {
            dbSavedObject.setChanges(actualObjectWithChange.findChanges());
            dbSavedObject.setCreate(actualObjectWithChange.isCreate());
            if (dbSavedObject instanceof User) {
                if (hasActiveStatusChange(actualObjectWithChange)) {
                    logActiveStatusChanges(actualObjectWithChange);
                }

                logger.debug("Got some changes for entity: {} for id:{} changes:{}", dbSavedObject.getClass().getSimpleName(), dbSavedObject.getId(), findInternalChanges(actualObjectWithChange));
            }
        }
    }


    private void logActiveStatusChanges(T actualObjectWithChange) {
        Change<Serializable> changes = actualObjectWithChange.getChanges().stream().filter(change -> change.getName().equalsIgnoreCase("activeStatus")).collect(Collectors.toList()).get(0);
        logger.info("activeStatus updated for User :'{}' from '{}' to '{}' on '{}'", ((User) actualObjectWithChange).getLoginId(), changes.getPrevious(), changes.getCurrent(),  actualObjectWithChange.getLastModifiedTime());
    }

    public boolean hasActiveStatusChange(CommonDataModel cdm) {
        return cdm.getChanges().stream().anyMatch(change -> change.getName().equalsIgnoreCase("activeStatus"));
    }

    private Map<String, Object> findInternalChanges(T cdm) {
        Set<Change<Serializable>> changes = cdm.findChanges();
        Map<String, Object> internalChanges = new HashMap<>();
        for (Change<Serializable> change : changes) {
            Serializable previous = change.getPrevious();
            Serializable current = change.getCurrent();
            if ((previous instanceof CommonDataModel) && (current instanceof CommonDataModel)) {
                Set<Change<Serializable>> cdmChanges = CdmDiffUtil.getChanges((CommonDataModel)current, (CommonDataModel) previous);
                internalChanges.put(change.getName(), cdmChanges);
            } else {
                internalChanges.put(change.getName(), change);
            }
        }
        return internalChanges;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public T persistEntity(T cdmObject) {
        cdmObject= preSaveEnrichment(cdmObject);
        cdmObject = fillCommonAttributes(cdmObject);
        enityManager.persist(cdmObject);
        enityManager.flush();
        eventBroadcaster.broadcast(cdmObject,EntityOperation.INSERT);
        return cdmObject;
    }

    @Transactional
    public T createOrUpdate(T cdm) {
        T refreshedCdm = refresh(cdm);
        return save(refreshedCdm);
    }


    /**
     * Batch refresh.
     *
     * @param clazz the clazz
     * @param batchrecords the batchrecords
     * @return the list
     */
    protected List<? extends CommonDataModel> batchRefresh(Class<T> clazz, List<T> batchrecords) {
        List<T> dbRecords = (List<T>) entityUtils.findRecords(clazz, new ArrayList<>(batchrecords));
        if(CollectionUtils.isNotEmpty(dbRecords)) {
            return batchrecords.stream().map(element ->{
                Optional<T> dataObj= dbRecords.stream().filter(p-> element.compare(p, true)).findFirst();
                if(dataObj.isPresent()){
                    T dbElement= dataObj.get();
                    CdmDiffUtil.setOldModel(dbElement);
                    EntityUtils.copyProperties(element, dbElement, "id","version");
                    return dbElement;
                }
                return element;
            }).collect(Collectors.toList());
        }
        return batchrecords;
    }

    public String getKey(T cdmObject) {
        List<T> resultSet = entityUtils.getGetKeyId(cdmObject);
        if (!resultSet.isEmpty()) {
            return resultSet.get(0).getId();
        } else {
            return entityUtils.generateId(cdmObject, false);
        }
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
        EnrichmentOperationResult er = null;
            er =  enrichmentService.enrich(cdm, EnrichmentPhase.PRE_SAVE);

        if(!er.getStatus().equals(Status.OK)) {
            throw new SystemRuntimeException((ObjectUtils.isNotEmpty(er.getEnrichmentResults()))?er.getEnrichmentResults().get(0).getMessage():
                    "Some error occured with pre-enrichment while storing "+cdm.toString());
        }
        return (T) (ObjectUtils.isNotEmpty(er.getEnrichedData())?er.getEnrichedData().get(0):cdm);
    }

    @Override
    public void delete(CommonDataModel cdm){
        if(StringUtils.isNotEmpty(cdm.getId())){
            deleteById(cdm.getId());
        }else{
            logger.warn("No id present in {} cdm object[[{}]] . Skipping deletion.",cdm.getClass().getSimpleName(),cdm);
        }
    }

    public boolean exists(String fieldName,String value){
        JdbcTemplate jdbcTemplate= JdbcUtils.createJdbcTemplate(
                (DataSource) DatabaseProfileRegistry.getDataSourceHashMap()
                        .get(SecurityContextUtils.getLob()));
        String tableName =EntityUtils.get().getEntityInfo(persistentClass).getTableName();
        String columnName = EntityUtils.get().getEntityInfo(persistentClass).getFieldNameMap().get(fieldName);
        String sql = "SELECT count(1) FROM "+tableName+" WHERE "+columnName+" = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class,value)>0;
    }

}
