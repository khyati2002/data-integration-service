package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.UserMetadataType;
import com.applicate.services.channelkart.repository.UserMetadataRepository;
import com.applicate.services.channelkart.utils.CdmDiffUtil;
import com.salescode.dim.cache.CacheManager;
import com.salescode.dim.cache.Cacheable;
import org.jooq.DSLContext;
import com.salescode.dim.jooq.generated.tables.records.CkUserMetadataRecord;
import com.salescode.dim.jooq.impl.UserMetadata;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_USER_METADATA;

public class UserMetadataService extends AbstractCDMService<UserMetadata> {

    private static final String CACHE_NAME = "dataintegration-usermetadata";
    private final UserMetadataRepository userMetadataRepository;

    public UserMetadataService() {
        super();
        this.userMetadataRepository = new UserMetadataRepository(getDslContext());
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

    @Override
    public Collection<UserMetadata> batchSave(Collection<UserMetadata> userMetadataList) {
        List<UserMetadata> metadataList = new ArrayList<>(userMetadataList);
        List<List<UserMetadata>> saveItemsList = getItemsToSaveList(metadataList);

        List<UserMetadata> itemsToInsert = saveItemsList.get(0);
        List<UserMetadata> itemsToUpdate = saveItemsList.get(1);

        getDslContext().transaction(config -> {
            DSLContext txnDsl = config.dsl();

            if (!itemsToInsert.isEmpty()) {
                txnDsl.batchInsert(
                        itemsToInsert.stream()
                                .map(meta -> txnDsl.newRecord(CK_USER_METADATA, meta)) // <-- Use txnDsl
                                .collect(Collectors.toList())
                ).execute();
            }

            if (!itemsToUpdate.isEmpty()) {
                txnDsl.batchUpdate(
                        itemsToUpdate.stream()
                                .map(meta -> {
                                    CkUserMetadataRecord records = txnDsl.newRecord(CK_USER_METADATA, meta); // <-- Use txnDsl
                                    records.changed(CK_USER_METADATA.ID, false);
                                    return records;
                                })
                                .collect(Collectors.toList())
                ).execute();
            }
        });

        CacheManager.getInstance().evictAll(CACHE_NAME);
        return metadataList;
    }

    /**
     * Splits the list into items for insertion and items for update, checking for changes.
     */
    private List<List<UserMetadata>> getItemsToSaveList(List<UserMetadata> metadataList) {
        List<String> values = metadataList.stream()
                .map(UserMetadata::getValue)
                .distinct()
                .collect(Collectors.toList());

        // Assumes a composite key of type and value for uniqueness,
        // matching the getByTypeAndValue method.
        // We fetch all existing metadata for the relevant values.
        Map<String, UserMetadata> savedMap = new HashMap<>();
        if (!values.isEmpty()) {
            getDslContext().selectFrom(CK_USER_METADATA)
                    .where(CK_USER_METADATA.VALUE.in(values))
                    .and(CK_USER_METADATA.TYPE.eq(UserMetadataType.MARKET_MAPPING.name()))
                    .fetchInto(UserMetadata.class)
                    .forEach(meta -> {
                        String key = meta.getType() + meta.getValue();
                        savedMap.put(key, meta);
                    });
        }

        List<UserMetadata> itemsToInsert = new ArrayList<>();
        List<UserMetadata> itemsToUpdate = new ArrayList<>();

        for (UserMetadata meta : metadataList) {
            fillCommonAttributes(meta);
            String key = meta.getType() + meta.getValue();
            UserMetadata savedMeta = savedMap.get(key);

            addHash(meta);

            if (savedMeta == null) {
                meta.setVersion(0);
                if (meta.getId() == null) {
                    meta.setId(meta.getType() + "-" + meta.getValue());
                }
                meta.setOperationPerformed(ActionType.INSERT);
                meta.setChanged(true);
                itemsToInsert.add(meta);
            } else {
                if (!Objects.equals(meta.getHash(), savedMeta.getHash())) {
                    meta.setId(savedMeta.getId());
                    meta.setVersion(savedMeta.getVersion());
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