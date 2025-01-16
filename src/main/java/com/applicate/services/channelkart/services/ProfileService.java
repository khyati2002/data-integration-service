package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.abstractdatasource.AbstractDataSourceConstants;
import com.applicate.services.channelkart.cache.CacheOperationsConstant;
import com.applicate.services.channelkart.cache.DistributedCache;
import com.applicate.services.channelkart.exceptions.ResourceNotFoundException;
import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.models.MetaData;
import com.applicate.services.channelkart.models.Profile;
import com.applicate.services.channelkart.profiles.ProfileRegistry;
import com.applicate.services.channelkart.repository.ProfileRepository;
import com.applicate.services.channelkart.security.SecurityContextUtils;
import com.applicate.services.channelkart.utils.DataPair;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfileService extends AbstractCDMService<Profile> {

    private final DistributedCache cache;

    private final MetaDataService metaDataService;

    private final ProfileRegistry profileRegistry;

    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository repository, DistributedCache cache, MetaDataService metaDataService, ProfileRegistry profileRegistry, ProfileRepository profileRepository) {
        super(repository);
        this.cache = cache;
        this.metaDataService = metaDataService;
        this.profileRegistry = profileRegistry;
        this.profileRepository = profileRepository;
    }

    public List<Profile> findByType(String type) {
        return profileRegistry.get(SecurityContextUtils.getLob(), type);
    }

    public List<Profile> findByTypeFromDb(String type) {
        return profileRepository.findByType(type);
    }

    public List<Profile> findByTypeFromDb(List<String> type) {
        return profileRepository.findByTypeIn(type);
    }

    public List<Profile> findByLobAndType(String lob, String type) {
        return profileRepository.findByLobAndType(lob, type);
    }

    public List<Profile> findByLobAndType(String lob, List<String> type) {
        return profileRepository.findByLobAndTypeIn(lob, type);
    }

    public Profile findByNameAndType(String name, String type) {
        return findByType(type)
                .stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Could not find profile with name:" + name + ", and type:" + type));
    }

    public Profile findByName(String name) {
        return this.profileRegistry.get(SecurityContextUtils.getLob())
                .stream().filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Could not find profile with name:" + name));
    }

    /**
     * Save.
     *
     * @param cdmObject the cdm object
     * @return the profile
     */
    @Override
    public Profile save(Profile cdmObject) {
        try {
            return super.save(cdmObject);
        } finally {
            clearCache(cdmObject);
        }
    }

    /**
     * Batch save.
     *
     * @param iterObj the iter obj
     * @return the list
     */
    @Override
    public List<Profile> batchSave(Iterable<Profile> iterObj) {
        try {
            return super.batchSave(iterObj);
        } finally {
            if (iterObj != null) {
                iterObj.forEach(this::clearCache);
            }
        }
    }

    /**
     * Delete by id.
     *
     * @param id the id
     */
    @Override
    public void deleteById(String id) {
        Profile profile = findById(id);
        if (profile != null) {
            try {
                super.deleteById(id);
            } finally {
                clearCache(profile);
            }
        }

    }

    /**
     * Delete by name.
     *
     * @param name the name
     */
    public void deleteByName(String name) {
        Profile profile = findByName(name);
        if (profile != null) {
            try {
                super.deleteById(profile.getId());
            } finally {
                clearCache(profile);
            }
        }

    }

    /**
     * Clear cache.
     *
     * @param profile the profile
     */
    public void clearCache(Profile profile) {
        cache.clearCache(SecurityContextUtils.getLob(), null, "profile");
//        cache.publishCacheEvent(SecurityContextUtils.getLob(), "profile:" + profile.getType(),
//                CacheOperationsConstant.DELETE, profile);
    }

    public List<JsonNode> findAllProfileMetaData() {
        List<MetaData> metadata = metaDataService.getAllProfileMetaDat();
        ArrayNode node = null;
        List<JsonNode> profiles = new ArrayList<>();
        node = metadata.get(0).getDomainValues();
        for (JsonNode element : node) {
            profiles.add(element);
        }
        return profiles;
    }

    public JsonNode getProfileMetaData(String type) {
        List<MetaData> metadata = metaDataService.getAllProfileMetaDat();
        ArrayNode node = null;
        JsonNode response = null;

        node = metadata.get(0).getDomainValues();
        for (JsonNode element : node) {
            if (element.get("type").textValue().equalsIgnoreCase(type)) {
                response = (element);
                return response;
            }
        }
        return response;
    }

    /**
     * Remove values from profile attributes.
     * @param dbProfiles list of profile records from database
     * @return list of updated profile records
     */
    public List<Object> removeAttributeFromProfiles(List<Profile> dbProfiles){
        return dbProfiles.stream().map(this::removeAttributeValues).collect(Collectors.toList());
    }

    /**
     * Remove values from profile attributes. Set the values as empty string
     * @param profile profile record from database
     * @return updated profile object
     */
    private Object removeAttributeValues(Profile profile){
        Map<String, Object> attributesMap = JSONUtils.convert(profile.getAttributes(), Map.class);
        attributesMap.forEach((key, value) -> attributesMap.put(key, ""));
        profile.populateAttributesOnly(JSONUtils.toJsonNode(attributesMap));
        profile.setPayload(null);
        return profile;
    }

    /**
     * Update profile attributes.
     * Keep existing attributes where new attribute value is null or empty string.
     * @param attributes new attributes object
     * @param existingAttributes existing attributes object
     * @return updated attributes
     */
    public JsonNode removeEmptyValues(JsonNode attributes, JsonNode existingAttributes){
        ObjectNode updatedAttributes = JSONUtils.getObjectMapper().createObjectNode();
        Iterator<String> stringIterator = attributes.fieldNames();
        while(stringIterator.hasNext()){
            String next = stringIterator.next();
            if(JSONUtils.isNull(attributes.get(next)) || StringUtils.isEmpty(attributes.get(next).asText())){
                updatedAttributes.set(next, existingAttributes.has(next) ? existingAttributes.get(next) : attributes.get(next) );
            } else{
                updatedAttributes.set(next, attributes.get(next));
            }
        }
        return JSONUtils.toJsonNode(updatedAttributes);
    }
}
