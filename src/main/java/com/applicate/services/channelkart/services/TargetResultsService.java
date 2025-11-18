/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.services;


import com.applicate.services.channelkart.enrichments.EnrichmentPhase;
import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.utils.CdmDiffUtil;
import com.applicate.services.channelkart.utils.IdGenerator;
import com.salescode.dim.cache.CacheManager;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dim.etl.enrichment.service.EnrichmentInfoRegistry;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.jooq.generated.tables.records.CkTargetResultsRecord;
import com.salescode.dim.jooq.generated.tables.records.CkTargetsRecord;
import com.salescode.dim.jooq.impl.TargetResults;
import com.salescode.dim.jooq.impl.User;
import com.salescode.dim.scanner.ExternalRegistryScanner;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_TARGETS;
import static com.salescode.dim.jooq.generated.Tables.CK_TARGET_RESULTS;

public class TargetResultsService extends AbstractCDMService<TargetResults> {

    private final DataEnrichmentService dataEnrichmentService;
    private final EnrichmentInfoRegistry enrichmentInfoRegistry;
    private final UserService userService;
    private ETLRegistry etlRegistry;

    public TargetResultsService() {
        ExternalRegistryScanner externalRegistryScanner = ExternalRegistryScanner.getInstance();
        etlRegistry = ETLRegistry.getInstance(externalRegistryScanner);
        enrichmentInfoRegistry = new EnrichmentInfoRegistry(getDslContext());
        dataEnrichmentService = new DataEnrichmentService(enrichmentInfoRegistry, etlRegistry);
        userService = new UserService();
    }


    @Override
    public Collection<TargetResults> batchSave(Collection<TargetResults> targets) {
        List<TargetResults> targetsList = preBatchSave(targets);
        List<List<TargetResults>> saveItemsList = getItemsToSaveList(targetsList);
        if (!saveItemsList.get(0).isEmpty()) {
            getDslContext().batchInsert(
                    saveItemsList.get(0).stream()
                            .map(target -> {
                                CkTargetResultsRecord targetsRecord = getDslContext().newRecord(CK_TARGET_RESULTS, target);
                                targetsRecord.setChanged((byte)1);
                                return targetsRecord;
                            }) // Convert to jOOQ Records
                            .collect(Collectors.toList())).execute();
        }
        if (!saveItemsList.get(1).isEmpty()) {
            getDslContext().batchUpdate(
                    saveItemsList.get(1).stream()
                            .map(target -> {
                                CkTargetResultsRecord targetsRecord = getDslContext().newRecord(CK_TARGET_RESULTS, target);
                                targetsRecord.setChanged((byte)1);
                                targetsRecord.changed(CK_TARGET_RESULTS.ID, false); // Avoid updating primary key
                                return targetsRecord;
                            })
                            .collect(Collectors.toList())
            ).execute();
        }
        CacheManager.getInstance().evictAll("dataintegration-user");
        return targets;
    }

    private List<List<TargetResults>> getItemsToSaveList(List<TargetResults> targetsList) {
        List<List<TargetResults>> result = new ArrayList<>();
        List<String> ids = targetsList.stream()
                .map(TargetResults::getId)
                .collect(Collectors.toList());

        Map<String, TargetResults> savedList = getDslContext().selectFrom(CK_TARGET_RESULTS)
                .where(CK_TARGET_RESULTS.ID.in(ids))
                .fetch()
                .intoMap(CK_TARGET_RESULTS.ID, trRecord -> trRecord.into(TargetResults.class));

        List<TargetResults> itemsToInsert = new ArrayList<>();
        List<TargetResults> itemsToUpdate = new ArrayList<>();

        for (TargetResults target : targetsList) {
            fillAttributes(target, TargetResults.of(savedList.get(target.getId())));
            fillCommonAttributes(target);
            new AttributeUpdateOverrideManager().overrideAttributes(target, savedList.get(target.getId()));
            super.addHash(target);
            preSaveEnrichment(target);
            if (savedList.get(target.getTargetId()) == null) {
                target.setVersion(0);
                target.setOperationPerformed(ActionType.INSERT);
                itemsToInsert.add(target);
            } else {
                TargetResults savedEntry = TargetResults.of(savedList.get(target.getId()));
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

    public List<TargetResults> preBatchSave(Collection<TargetResults> targets) {
        List<TargetResults> preparedTargets = new ArrayList<>();
        targets.forEach(entry -> {
            if (entry.getId() == null)   entry.setId(new IdGenerator(entry.getClass().getSimpleName()).getId(entry));
            if (entry.getActiveStatus() == null)   entry.setActiveStatus(ActiveStatus.ACTIVE);
            populateUserAndOutlet(entry);
            preparedTargets.add(entry);
        });
        return preparedTargets;
    }

    private void preSaveEnrichment(TargetResults targets) {
        OperationResult or = dataEnrichmentService.enrich(targets, EnrichmentPhase.PRE_SAVE);
        if (!or.getStatus().equals(OperationResult.Status.OK)) {
            throw new RuntimeException("Pre save enrichment error");
        }
    }

    private void populateUserAndOutlet(TargetResults tr) {
        String user = tr.getLoginId();
        if (user != null) {
            User tempUser = userService.findByLoginId(user);
            if (tempUser != null) {
                if (tempUser.getLocationHierarchy() != null) {
                    tr.setLocationHierarchy(tempUser.getLocationHierarchy());
                }
                tr.setHierarchy(tempUser.getHierarchy());
                tr.setLoginId(user);
            }
        }
        String outlet = tr.getOutletCode();
        if (outlet != null) {
            tr.setOutletCode(outlet);
        }

    }
}
