package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.UserMetadataType;
import com.applicate.services.channelkart.repository.UserMetadataRepository;
import com.applicate.services.channelkart.utils.CdmDiffUtil;
import com.applicate.services.channelkart.enrichments.EnrichmentPhase;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dim.etl.enrichment.service.EnrichmentInfoRegistry;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.scanner.ExternalRegistryScanner;
import com.salescode.dim.cache.CacheManager;
import com.salescode.dim.cache.Cacheable;
import com.salescode.dim.jooq.generated.tables.records.CkUserMetadataRecord;
import com.salescode.dim.jooq.impl.UserMetadata;
import com.applicate.services.channelkart.exceptions.EnrichmentFailException;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_USER_METADATA;

public class UserMetadataService extends AbstractCDMService<UserMetadata> {

    private static final String CACHE_NAME = "dataintegration-usermetadata";
    private final UserMetadataRepository userMetadataRepository;

    private final DataEnrichmentService dataEnrichmentService;
    private final EnrichmentInfoRegistry enrichmentInfoRegistry;
    private final ETLRegistry etlRegistry;

    public UserMetadataService() {
        super();
        this.userMetadataRepository = new UserMetadataRepository(getDslContext());
        ExternalRegistryScanner externalRegistryScanner = ExternalRegistryScanner.getInstance();
        etlRegistry = ETLRegistry.getInstance(externalRegistryScanner);
        enrichmentInfoRegistry = new EnrichmentInfoRegistry(getDslContext());
        dataEnrichmentService = new DataEnrichmentService(enrichmentInfoRegistry, etlRegistry);
    }

    @Cacheable(cacheName = CACHE_NAME)
    public UserMetadata getByLoginIdAndTypeAndValue(String loginId, UserMetadataType type, String value){
        return userMetadataRepository.getByLoginIdAndTypeAndValue(loginId, type.name(), value);
    }

    @Cacheable(cacheName = CACHE_NAME)
    public List<UserMetadata> getByTypeAndValue(UserMetadataType type, String value){
        return userMetadataRepository.getByTypeAndValue(type.name(), value);
    }

    @Cacheable(cacheName = CACHE_NAME)
    public List<UserMetadata> getByLoginId(String loginId){
        return userMetadataRepository.getByLoginId(loginId);
    }

    private void preSaveEnrichment(UserMetadata userMetadata){
        OperationResult or = dataEnrichmentService.enrich(userMetadata, EnrichmentPhase.PRE_SAVE);
        if(!or.getStatus().equals(OperationResult.Status.OK)){
            String errorMessages = or.getStepResults().stream()
                    .filter(step -> step.getStatus() != OperationResult.Status.OK && step.getMessage() != null)
                    .map(OperationResult.StepResult::getMessage)
                    .collect(Collectors.joining(", "));

            throw new EnrichmentFailException("Pre save enrichment error: " + errorMessages);
        }
    }

    @Override
    public Collection<UserMetadata> batchSave(Collection<UserMetadata> userMetadataList) {
        List<UserMetadata> metadataList = new ArrayList<>(userMetadataList);
        List<List<UserMetadata>> saveItemsList = getItemsToSaveList(metadataList);

        List<UserMetadata> itemsToInsert = saveItemsList.get(0);
        List<UserMetadata> itemsToUpdate = saveItemsList.get(1);

        if (!itemsToInsert.isEmpty()) {
            getDslContext().batchInsert(
                    itemsToInsert.stream()
                            .map(meta -> getDslContext().newRecord(CK_USER_METADATA, meta))
                            .collect(Collectors.toList())
            ).execute();
        }

        if (!itemsToUpdate.isEmpty()) {
            getDslContext().batchUpdate(
                    itemsToUpdate.stream()
                            .map(meta -> {
                                CkUserMetadataRecord records = getDslContext().newRecord(CK_USER_METADATA, meta);
                                records.changed(CK_USER_METADATA.ID, false);
                                return records;
                            })
                            .collect(Collectors.toList())
            ).execute();
        }

        CacheManager.getInstance().evictAll(CACHE_NAME);
        return metadataList;
    }

    /**
     * Splits the list into items for insertion and items for update, checking for changes.
     */
    private List<List<UserMetadata>> getItemsToSaveList(List<UserMetadata> metadataList) {

        List<String> ids = metadataList.stream()
                .map(UserMetadata::getId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, com.salescode.dim.jooq.generated.tables.pojos.UserMetadata> savedMap =
                new HashMap<>();

        if (!ids.isEmpty()) {
            savedMap = getDslContext().selectFrom(CK_USER_METADATA)
                    .where(CK_USER_METADATA.ID.in(ids))
                    .fetch()
                    .intoMap(CK_USER_METADATA.ID, metaRecord -> metaRecord.into(com.salescode.dim.jooq.generated.tables.pojos.UserMetadata.class));
        }

        List<UserMetadata> itemsToInsert = new ArrayList<>();
        List<UserMetadata> itemsToUpdate = new ArrayList<>();

        for (UserMetadata meta : metadataList) {
            String key = meta.getId();
            com.salescode.dim.jooq.generated.tables.pojos.UserMetadata savedPojo = savedMap.get(key);
            UserMetadata savedMeta = UserMetadata.of(savedPojo);

            fillAttributes(meta, savedMeta);
            fillCommonAttributes(meta);

            if (meta.getLoginid() == null) {
                meta.setLoginid(SecurityContextUtils.getPrincipal());
            }

            super.addHash(meta);

            if (savedMeta == null) {
                preSaveEnrichment(meta);
                meta.setVersion(0);
                meta.setOperationPerformed(ActionType.INSERT);
                meta.setChanged(true);
                itemsToInsert.add(meta);
            } else {
                if (!Objects.equals(meta.getHash(), savedMeta.getHash())) {
                    preSaveEnrichment(meta);
                    meta.setId(savedMeta.getId());
                    meta.setVersion(savedMeta.getVersion() == null ? 0 : savedMeta.getVersion() + 1);
                    meta.setChanges(CdmDiffUtil.getChanges(meta, savedMeta));
                    meta.setOperationPerformed(ActionType.UPDATE);
                    meta.setChanged(true);
                    itemsToUpdate.add(meta);
                } else {
                    meta.setId(savedMeta.getId());
                    meta.setVersion(savedMeta.getVersion());
                    meta.setChanged(false);
                }
            }
        }

        return Arrays.asList(itemsToInsert, itemsToUpdate);
    }
}