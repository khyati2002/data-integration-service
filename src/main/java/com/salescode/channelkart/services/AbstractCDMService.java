package com.salescode.channelkart.services;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.salescode.channelkart.batch.BatchService;
import com.salescode.channelkart.batch.hash.BatchContainer;
import com.salescode.channelkart.enrichments.DataEnrichmentService;
import com.salescode.channelkart.enrichments.EnrichmentOperationResult;
import com.salescode.channelkart.enrichments.EnrichmentPhase;
import com.salescode.channelkart.enrichments.Status;
import com.salescode.channelkart.exceptions.CustomRuntimeException;
import com.salescode.channelkart.exceptions.SystemRuntimeException;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.models.Role;
import com.salescode.channelkart.models.User;
import com.salescode.channelkart.models.diff.Change;
import com.salescode.channelkart.models.enums.ActiveStatus;
import com.salescode.channelkart.repository.CommonJpaRepository;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.services.enums.EntityOperation;
import com.salescode.channelkart.services.enums.OperationType;
import com.salescode.channelkart.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.hibernate.Session;
import org.hibernate.collection.internal.PersistentBag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractCDMService<T extends CommonDataModel> implements CommonDataModelService<T> {

    @Value("${spring.jpa.properties.hibernate.jdbc.fetch_size}")
    private int fetchSize;

    protected CommonJpaRepository<T, String> repository;

    private Class<?> persistentClass;

    private static final List<Class<Role>> restrictedList = Arrays.asList(Role.class);

    @Autowired
    private Environment env;

    @Autowired
    private BatchService batchService;

    @Autowired
    private DataEnrichmentService enrichmentService;
    @Autowired
    private ChangeEventBroadcaster eventBroadcaster;


    public AbstractCDMService(CommonJpaRepository<T, String> repository) {
        this.repository = repository;
        if (getClass().getGenericSuperclass() instanceof ParameterizedType) {
            persistentClass = (Class<?>)
                    ((ParameterizedType) getClass().getGenericSuperclass())
                            .getActualTypeArguments()[0];
            ServiceLocator.register(persistentClass, (CommonDataModelService<?>) this);
        }
    }


    @Override
    public T refresh(T cdmObject) {
        T dbRecord = CdmDiffUtil.withOldModel(() -> (T) EntityUtils.get().findRecords(cdmObject.getClass(), cdmObject));
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
        List<T> resultSet = EntityUtils.get().getGetKeyId(cdmObject);
        if (!resultSet.isEmpty()) {
            return resultSet.get(0).getId();
        } else {
            return EntityUtils.get().generateId(cdmObject, false);
        }
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public List<T> saveForList(T cdmObject, OperationType type) {
        return List.of(save(cdmObject, type));
    }

    @Override
    public void deleteById(String id, boolean failsOnEmptyRecord) {
        Optional<T> entityToDelete = repository.findById(id);
        if (entityToDelete.isEmpty() && failsOnEmptyRecord) {
            throw new ResourceNotFoundException("Record with entity id {}, not present in table. Please verify input data.", id);
        } else if (entityToDelete.isPresent()) {
            repository.deleteById(id);
//            eventBroadcaster.broadcast(entityToDelete.get(),EntityOperation.DELETE);
        }
    }

    protected void addHash(CommonDataModel model) {
        if (model.canHash()) {
            batchService.addHashIfPresent(model);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public T save(T cdmObject) {
        final T inObject = cdmObject;
        String existingHash = cdmObject.getHash();
        TimerUtils.withTime("Time taken to generate Hash " + cdmObject.getClass().getName() + ":" + cdmObject.getId(),
                () -> addHash(inObject));
        if (!cdmObject.forceHash() && cdmObject.canHash() && StringUtils.isNotEmpty(existingHash) && existingHash.equals(cdmObject.getHash())) {
            // no need to save this record because this hash is same
            return cdmObject;
        }
        T saved = null;
//        if(isNativeBatchSave(cdmObject)) {
//            saved=TimerUtils.withTime("Time taken to native finish native save operation "+cdmObject.getClass().getName()+":"+cdmObject.getId(), () -> nativeBatchSave(Arrays.asList(inObject),null).get(0));
//        }
//        else {
        cdmObject = fillCommonAttributes(cdmObject);
        if (!cdmObject.isCreate()) {
            //     apiFilterAuthorizationManager.assertPermission( cdmObject);
        }
        final T object = cdmObject;
        saved = TimerUtils.withTime("Time taken to JPA save  " + cdmObject.getClass().getSimpleName() + ":" + cdmObject.getId(), () -> {
            T object1 = preSaveEnrichment(object);
            T sreturn = repository.save(object1);
            fixChanges(object1, sreturn);
            return sreturn;
        });

        repository.flush();

//        return saved;
        T finalCdmObject = cdmObject;
        T finalSaved = saved;
        if (finalCdmObject.isCreate()) {
            eventBroadcaster.broadcast(finalSaved, EntityOperation.INSERT);
        } else {
            eventBroadcaster.broadcast(finalSaved, EntityOperation.UPDATE);
        }

        return finalSaved;
    }

    public T fillCommonAttributes(T cdm) {
        return fillCommonAttributes(cdm, false, new HashSet<>(), null);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public T save(T cdmObject, OperationType type) {
        return save(cdmObject);
    }


    @Transactional(propagation = Propagation.REQUIRED)
    public List<T> batchSave(Iterable<T> iterObj) throws Exception {
        return batchSave(iterObj, null);
    }


    private T preSaveEnrichment(T cdm) {
        EnrichmentOperationResult er = enrichmentService.enrich(cdm, EnrichmentPhase.PRE_SAVE);
        if (!er.getStatus().equals(Status.OK)) {
            throw new SystemRuntimeException((ObjectUtils.isNotEmpty(er.getEnrichmentResults())) ? er.getEnrichmentResults().get(0).getMessage() :
                    "Some error occured with pre-enrichment while storing " + cdm.toString());
        }
        return (T) (ObjectUtils.isNotEmpty(er.getEnrichedData()) ? er.getEnrichedData().get(0) : cdm);
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
        return Boolean.parseBoolean(env.getProperty("native.batch.save." + element.getClass().getSimpleName(), "false"));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public List<T> batchSave(Iterable<T> iterObj, IdGenerator idGenerator) throws Exception {
        iterObj.forEach(this::preSaveEnrichment);
        if (isNativeBatchSave(iterObj)) {
            throw new Exception("Native batch save not allowed");
        } else {
            BatchContainer<T> container = splitElements(iterObj, idGenerator);
            List<T> elementsToSaveAsList = container.getAllElementstoSave();
            List<T> savedData = saveAll(elementsToSaveAsList);
            return ListUtils.union(container.getDuplicateElementsAsList(), savedData);

        }
    }

    private void fixChanges(List<T> actualObjectWithChange, List<T> dbSavedObject) {
        if (actualObjectWithChange != null && dbSavedObject != null && actualObjectWithChange.size() == dbSavedObject.size()) {
            for (int i = 0; i < dbSavedObject.size(); i++) {
                fixChanges(actualObjectWithChange.get(i), dbSavedObject.get(i));
            }
        } else {
            //  logger.warn("null or empty objects passed to add changes.. , ignoring");
        }
    }

    private List<T> saveAll(Iterable<T> items) {
        //items.forEach(element-> apiFilterAuthorizationManager.assertPermission(element));
        List<T> saved = this.repository.saveAll(items);
        fixChanges((List<T>) items, saved); //hack to populate persist changes after putting to db, should find a better place to do this.
        repository.flush();
        //notifyBatchSave(saved);
        return saved;
    }

    public T fillCommonAttributes(T cdm, IdGenerator idGenerator) {
        return fillCommonAttributes(cdm, false, new HashSet<>(), idGenerator);
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

        BatchContainer<T> batchContainer = new BatchContainer<>();
        batchContainer.setElementsToInsert(newRecords);
        batchContainer.setElementsToUpdate(existingRecords);
        return batchContainer;

    }


    @SuppressWarnings("unchecked")
    public T fillCommonAttributes(T cdm, boolean createOnly, Set<String> visitedTree, IdGenerator idGenerator) {
        boolean fillModifyAttributes = !createOnly || cdm.getId() == null;
        if (cdm.getId() == null) {
            if (idGenerator != null) {
                cdm.setId(idGenerator.getId(cdm));
            } else {
                //noinspection deprecation
                cdm.setId(EntityUtils.get().generateId(cdm));
            }
        }
//        TimerUtils.withTime("Time taken to find changed value", () -> deltaAnalyzer.findAndSetDeltaChanges(cdm));
//
//        setOperationType(cdm);

        if (cdm.getCreationTime() == null) {
            cdm.setCreationTime(Calendar.getInstance().getTime());
        }
        if (cdm.getActiveStatus() == null) {
            cdm.setActiveStatus(ActiveStatus.ACTIVE);
        }

//        if (cdm instanceof TransactionDataModel) {
//            ((TransactionDataModel) cdm).setSystemTime(Calendar.getInstance().getTime());
//        }

        if (cdm.getCreatedBy() == null) {
            cdm.setCreatedBy(SecurityContextUtils.getPrincipal());
        }
//
//        if (!DataSourceUtils.isDefaultDataSource(SecurityContextUtils.getLob())) {
//            cdm.setLob(SecurityContextUtils.getLob());
//        }


        cdm.setLastModifiedTime(Calendar.getInstance().getTime());
        cdm.setModifiedBy("dis");
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
                        visitParameterizedCDMType(cdm, field, typeArgument, visitedTree, idGenerator);
                    }
                } else if (CommonDataModel.class.isAssignableFrom((Class<?>) returnType)) {
                    visitCDMType(cdm, field, returnType, visitedTree, idGenerator);
                }
            }
        }
    }

    private void visitParameterizedCDMType(T cdm, Field field, Type typeArgument, Set<String> visitedTree, IdGenerator idGenerator) {
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
                //     logger.error("stacktrace", e);
            }
        }
    }

    private void visitCDMType(T cdm, Field field, Type returnType, Set<String> visitedTree, IdGenerator idGenerator) {
        try {
            if (!restrictedList.contains(returnType)) {
                field.setAccessible(true);
                Optional<T> cdmOpt = Optional.ofNullable((T) field.get(cdm));
                cdmOpt.ifPresent(cdmAssociation -> fillCommonAttributes(cdmAssociation, true, visitedTree, idGenerator));
            }
        } catch (Exception e) {
            //  logger.error("stacktrace", e);
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

                //    logger.debug("Got some changes for entity: {} for id:{} changes:{}", dbSavedObject.getClass().getSimpleName(), dbSavedObject.getId(), findInternalChanges(actualObjectWithChange));
            }
        }
    }

    private void logActiveStatusChanges(T actualObjectWithChange) {
        Change<Serializable> changes = actualObjectWithChange.getChanges().stream().filter(change -> change.getName().equalsIgnoreCase("activeStatus")).collect(Collectors.toList()).get(0);
        //  logger.info("activeStatus updated for User :'{}' from '{}' to '{}' on '{}'", ((User) actualObjectWithChange).getLoginId(), changes.getPrevious(), changes.getCurrent(),  actualObjectWithChange.getLastModifiedTime());
    }

    public boolean hasActiveStatusChange(CommonDataModel cdm) {
        return cdm.getChanges().stream().anyMatch(change -> change.getName().equalsIgnoreCase("activeStatus"));
    }

    protected void setChanges(T previous, T current) {
        if (current.getOldModel() != null) {
            current.setOldModel(EntityUtils.deepClone(previous));
        }
    }


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

    protected List<? extends CommonDataModel> batchRefresh(Class<T> clazz, List<T> batchrecords) {
        List<T> dbRecords = (List<T>) EntityUtils.get().findRecords(clazz, new ArrayList<>(batchrecords));
        if (CollectionUtils.isNotEmpty(dbRecords)) {
            return batchrecords.stream().map(element -> {
                Optional<T> dataObj = dbRecords.stream().filter(p -> element.compare(p, true)).findFirst();
                if (dataObj.isPresent()) {
                    T dbElement = dataObj.get();
                    CdmDiffUtil.setOldModel(dbElement);
                    EntityUtils.copyProperties(element, dbElement, "id", "version");
                    return dbElement;
                }
                return element;
            }).collect(Collectors.toList());
        }
        return batchrecords;
    }

    @Override
    public T populateData(T cdmObject) {
        return cdmObject;
    }


}
