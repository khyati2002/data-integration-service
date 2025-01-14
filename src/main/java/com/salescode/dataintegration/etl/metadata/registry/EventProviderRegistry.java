package com.salescode.dataintegration.etl.metadata.registry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.salescode.channelkart.abstractdatasource.DatabaseProfileRegistry;
import com.salescode.channelkart.event.EventProfile;
import com.salescode.channelkart.event.provider.DefaultEventProfile;
import com.salescode.channelkart.exceptions.EventException;
import com.salescode.channelkart.event.provider.EventProvider;
import com.salescode.channelkart.models.Profile;
import com.salescode.channelkart.profiles.ProfileRegistry;
import com.salescode.channelkart.services.SpringContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EventProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(EventProviderRegistry.class);

    public static final EventProviderRegistry INSTANCE = new EventProviderRegistry();

    private EventProvider defaultProvider;

    private boolean hasLoaded;

    private final Map<String, Map<String, EventProvider>> providers;

    private EventProviderRegistry() {
        this.providers = new HashMap<>();
    }

    public EventProvider getDefault(String lob) {
        ensureRegistryLoaded();
        return providers.getOrDefault(lob, Collections.emptyMap())
                .values()
                .stream()
                .filter(eventProvider -> eventProvider.getProfile().isDefault())
                .findFirst()
                .orElse(this.defaultProvider);
    }

    private synchronized void ensureRegistryLoaded() {
        if(!hasLoaded) {
            loadAll();
        }
    }

    public synchronized void loadAll() {
        try {
            this.defaultProvider = createDefaultProvider();
            DatabaseProfileRegistry.getDataSourceHashMap()
                    .keySet()
                    .stream()
                    .map(Object::toString)
                    .forEach(this::load);
            hasLoaded = true;
            log.info("Events providers initialized...........");
        } catch (Exception e) {
            throw new EventException("Could not initialize event provider registry...", e);
        }
    }

    private EventProvider createDefaultProvider() throws JsonProcessingException {
        var defaultEventProfile = SpringContext.getBean(DefaultEventProfile.class);
        EventProfile profile = EventProfile.createFrom(defaultEventProfile.getProfile());
        return EventProvider.create(profile);
    }

    private synchronized void load(String lob) {
        log.debug("Loading event provider for lob: {}", lob);
        try {
            this.providers.put(lob, getEventProvider(lob));
        } catch (Exception e) {
            throw new EventException("Could not initialize event provider for lob " + lob, e);
        }
        log.debug("Event provider for lob: {} has been initialized", lob);
    }

    private Map<String, EventProvider> getEventProvider(String lob) {
        Map<String, EventProfile> eventProfiles = getEventProfiles(lob);
        return eventProfiles.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> getProvider(entry.getValue())));
    }

    private Map<String, EventProfile> getEventProfiles(String lob) {
        return ProfileRegistry.INSTANCE.get(lob, DefaultEventProfile.EVENT_PROVIDER_PROFILE)
                .stream()
                .collect(Collectors.toMap(Profile::getName, EventProfile::createFrom));
    }

    private EventProvider getProvider(EventProfile profile) {
        return getProviderIfAlreadyExist(profile)
                .orElseGet(() -> EventProvider.create(profile));
    }

    private Optional<EventProvider> getProviderIfAlreadyExist(EventProfile profile) {
        if (defaultProvider.getProfile().equals(profile)) {
            return Optional.of(defaultProvider);

        }
        return this.providers
                .values()
                .stream()
                .flatMap(map -> map.values().stream())
                .filter(eventProvider -> eventProvider.getProfile().equals(profile))
                .findFirst();
    }

    public EventProvider get(String lob, String name) {
        ensureRegistryLoaded();
        Map<String, EventProvider> lobProviders = providers.getOrDefault(lob, Collections.emptyMap());
        var eventProvider = lobProviders.get(name);
        return eventProvider == null ? defaultProvider : eventProvider;
    }
}