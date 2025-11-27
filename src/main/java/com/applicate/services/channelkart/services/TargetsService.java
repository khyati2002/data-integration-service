package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.enrichments.EnrichmentPhase;
import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.utils.CdmDiffUtil;
import com.applicate.services.channelkart.utils.IdGenerator;
import com.salescode.dim.cache.CacheManager;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dim.etl.enrichment.service.EnrichmentInfoRegistry;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.jooq.generated.tables.records.CkTargetsRecord;
import com.salescode.dim.jooq.impl.TargetResults;
import com.salescode.dim.jooq.impl.Targets;
import com.salescode.dim.jooq.impl.User;
import com.salescode.dim.scanner.ExternalRegistryScanner;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_TARGETS;


public class TargetsService extends AbstractCDMService<Targets> {
    private final UserService userService;
    private final DataEnrichmentService dataEnrichmentService;
    private final EnrichmentInfoRegistry enrichmentInfoRegistry;
    private ETLRegistry etlRegistry;
    private TargetResultsService targetResultsService;


    public TargetsService() {
        ExternalRegistryScanner externalRegistryScanner = ExternalRegistryScanner.getInstance();
        etlRegistry = ETLRegistry.getInstance(externalRegistryScanner);
        userService = new UserService();
        targetResultsService = new TargetResultsService();
        enrichmentInfoRegistry = new EnrichmentInfoRegistry(getDslContext());
        dataEnrichmentService = new DataEnrichmentService(enrichmentInfoRegistry, etlRegistry);
    }

    public static Targets cloneTarget(Targets obj) throws InvocationTargetException, IllegalAccessException {
        Targets target = new Targets();
        BeanUtils.copyProperties(obj, target);
        return target;

    }

    public List<Targets> preBatchSave(Collection<Targets> targets) {
        List<Targets> preparedTargets = new ArrayList<>();

        targets.forEach(target -> {
            if (target.getTargetcondition() == null)   target.setTargetcondition(0d);
            if (target.getId() == null)   target.setId(new IdGenerator(target.getClass().getSimpleName()).getId(target));
            try {
                if (target.getVersion() == null) {
                    preparedTargets.addAll(prepareTargets(target));
                } else {
                    if (target.getTarget() != null && !ObjectUtils.isEmpty(target.getTargetResults())) {
                        target.getTargetResults().forEach(entry -> {
                            populateUserAndOutlet(entry);
                            entry.setTargetId(target.getTargetId());
                            if (entry.getId() == null) entry.setId(new IdGenerator(entry.getClass().getSimpleName()).getId(entry));;
                            if (entry.getAchieved() == null) entry.setAchieved(0F);
                        });
                    }
                    preparedTargets.add(target);
                }
            } catch (Exception e) {
                throw new RuntimeException("Exception happend while saving targets " + e.getMessage(), e);
            }
        });

        return preparedTargets;
    }

    private void populateUserAndOutlet(TargetResults tr) {
        String user = tr.getLoginId();
        if (user != null) {
            User tempUser = userService.findByLoginId(user);
            if (tempUser != null) {
                if (tempUser.getLocationHierarchy() != null)
                    tr.setLocationHierarchy(tempUser.getLocationHierarchy());
                tr.setHierarchy(tempUser.getHierarchy());
                tr.setLoginId(user);
            }
        }
        String outlet = tr.getOutletCode();
        if (outlet != null) {
            tr.setOutletCode(outlet);
        }

    }

    public Targets fillTargetCommonAttributes(Targets target) {
//        if(StringUtils.isEmpty(target.getId())){
//            target.setId(entityUtils.generateId(target,false));
//        }
//        if(target.getTargetResults()!=null) {
//            target.getTargetResults().stream().filter(sd->sd.getTarget()==null).forEach(sd->sd.setTarget(target));
//        }
        populateCDMDataInTrResults(target);
        fillCommonAttributes(target);
        if (target.getTargetResults() != null && !target.getTargetResults().isEmpty()) {
            target.getTargetResults().stream().filter(sd -> sd.getId() == null).forEach(sd -> sd.setId(UUID.randomUUID().toString()));
        }
        return target;
    }

    private Targets populateCDMDataInTrResults(Targets target) {
        LocalDateTime time = LocalDateTime.now();
        target.setLastModifiedTime(time);
        if (target.getTargetResults() != null && !target.getTargetResults().isEmpty()) {
            target.getTargetResults().forEach(tres -> {
                if (tres.getCreationTime() == null) {
                    tres.setCreationTime(time);
                }
                if (tres.getCreatedBy() == null) {
                    tres.setCreatedBy(target.getCreatedBy());
                }
                if (tres.getModifiedBy() == null) {
                    tres.setModifiedBy(target.getModifiedBy());
                }
                tres.setLastModifiedTime(time);
                if (tres.getLob() == null) {
                    tres.setLob(target.getLob());
                }
            });
        }
        return target;
    }

    private List<Targets> prepareTargets(Targets targets) {

        List<Targets> targetsM = List.of(targets);

        targetsM.forEach(tr -> {
            if (targets.getTargetResults() != null) {
                TargetResults tempObj = targets.getTargetResults().get(0);
                if (tempObj.getAchieved() != 0) {
//                    tempObj.setTarget(targets);
                    if (tempObj.getTargetId() == null) tempObj.setTargetId(targets.getTargetId());
                    if (tempObj.getId() == null) tempObj.setId(targets.getId());
                    setUserInfo(tempObj);
                    String outlet = tempObj.getOutletCode();
                    if (outlet != null) {
                        tempObj.setOutletCode(outlet);
                    }
                } else {
                    targets.setTargetResults(null);
                }
            }
        });
        return targetsM;
    }

    private void setUserInfo(TargetResults tempObj) {
        String user = tempObj.getLoginId();
        if (user != null) {
            User tempUser = userService.findByLoginId(user);
            if (tempUser != null) {
                if (tempUser.getLocationHierarchy() != null) {
                    tempObj.setLocationHierarchy(tempUser.getLocationHierarchy());
                }
                tempObj.setHierarchy(tempUser.getHierarchy());
                tempObj.setLoginId(user);
            }
        }
    }

    @Override
    public Collection<Targets> batchSave(Collection<Targets> targets) {
        List<Targets> targetsList = preBatchSave(targets);
        List<List<Targets>> saveItemsList = getItemsToSaveList(targetsList);
        if (!saveItemsList.get(0).isEmpty()) {
            getDslContext().batchInsert(
                    saveItemsList.get(0).stream()
                            .map(target -> {
                                CkTargetsRecord targetsRecord = getDslContext().newRecord(CK_TARGETS, target);
                                targetsRecord.setChanged(true);
                                return targetsRecord;
                            }) // Convert to jOOQ Records
                            .collect(Collectors.toList())).execute();
        }
        if (!saveItemsList.get(1).isEmpty()) {
            getDslContext().batchUpdate(
                    saveItemsList.get(1).stream()
                            .map(target -> {
                                CkTargetsRecord targetsRecord = getDslContext().newRecord(CK_TARGETS, target);
                                targetsRecord.setChanged(true);
                                targetsRecord.changed(CK_TARGETS.ID, false); // Avoid updating primary key
                                return targetsRecord;
                            })
                            .collect(Collectors.toList())
            ).execute();
        }
        CacheManager.getInstance().evictAll("dataintegration-user");
        if (!saveItemsList.get(0).isEmpty() || !saveItemsList.get(1).isEmpty()) {
            postBatchSave(targetsList);
        }

        return targetsList;
    }

    public void postBatchSave(List<Targets> targetsList) {
        List<TargetResults> targetResults = new ArrayList<>();
        targetsList.forEach(target -> {
            if(!ObjectUtils.isEmpty(target.getTargetResults())) {
                targetResults.addAll(target.getTargetResults());
            }
        });
        if(!targetResults.isEmpty()) targetResultsService.batchSave(targetResults);
    }


    private List<List<Targets>> getItemsToSaveList(List<Targets> targetsList) {

        List<List<Targets>> result = new ArrayList<>();

        List<String> ids = targetsList.stream()
                .map(Targets::getId)
                .collect(Collectors.toList());

        Map<String, Targets> savedList = getDslContext().selectFrom(CK_TARGETS)
                .where(CK_TARGETS.ID.in(ids))
                .fetch()
                .intoMap(CK_TARGETS.ID, recordEntry -> recordEntry.into(Targets.class));

        List<Targets> itemsToInsert = new ArrayList<>();
        List<Targets> itemsToUpdate = new ArrayList<>();

        for (Targets target : targetsList) {
            fillAttributes(target, Targets.of(savedList.get(target.getId())));
            fillTargetCommonAttributes(target);
            new AttributeUpdateOverrideManager().overrideAttributes(target, savedList.get(target.getId()));
            super.addHash(target);
            preSaveEnrichment(target);
            if (savedList.get(target.getId()) == null) {
                target.setVersion(0);
                target.setOperationPerformed(ActionType.INSERT);
                itemsToInsert.add(target);

            } else {
                Targets savedEntry = Targets.of(savedList.get(target.getId()));
                target.setVersion(savedList.get(target.getId()).getVersion() + 1);
                target.setChanges(CdmDiffUtil.getChanges(target, savedEntry));
                target.setOperationPerformed(ActionType.UPDATE);
                itemsToUpdate.add(target);
            }
        }
        result.add(itemsToInsert);
        result.add(itemsToUpdate);
        return result;
    }

    private void preSaveEnrichment(Targets targets) {
        OperationResult or = dataEnrichmentService.enrich(targets, EnrichmentPhase.PRE_SAVE);
        if (!or.getStatus().equals(OperationResult.Status.OK)) {
            throw new RuntimeException("Pre save enrichment error");
        }
    }


}