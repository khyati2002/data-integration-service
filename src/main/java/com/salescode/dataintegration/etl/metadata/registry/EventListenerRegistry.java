package com.salescode.dataintegration.etl.metadata.registry;

import com.salescode.channelkart.cache.AllLOBRouter;
import com.salescode.channelkart.cache.DistributedCache;
import com.salescode.channelkart.models.EventListenerInfo;
import com.salescode.channelkart.registry.AbstractRegistry;
import com.salescode.channelkart.services.SpringContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class EventListenerRegistry extends AbstractRegistry<EventListenerInfo>  {

    public static final EventListenerRegistry INSTANCE = new EventListenerRegistry();

    private DistributedCache distributedCache = SpringContext.getBean(DistributedCache.class);

    private static final String CACHE_DOMAIN = "eventlistener";

    @SuppressWarnings("unchecked")
    public void add(EventListenerInfo eventListenerInfo) {
        synchronized (EventListenerInfo.class) {
            List<EventListenerInfo> listenerInfos = (List<EventListenerInfo>) distributedCache.get(eventListenerInfo.getLob(), null, CACHE_DOMAIN, false);
            if (listenerInfos == null) {
                listenerInfos = new ArrayList<>();
            }
            listenerInfos.add(eventListenerInfo);
            clear(eventListenerInfo.getLob());
            distributedCache.put(eventListenerInfo.getLob(), null, CACHE_DOMAIN, listenerInfos, false);
        }
    }

    public void addAll(String lob, List<EventListenerInfo> eventListener) {
        synchronized (EventListenerInfo.class) {
            clear(lob);
            distributedCache.put(lob, null, CACHE_DOMAIN, eventListener, false);
        }
    }

    public void loadAll(Predicate<String> predicate, boolean overrideOld) {
        AllLOBRouter.loadAll(EventListenerInfo.class,predicate).stream().map(e->(EventListenerInfo)e).collect(Collectors.groupingBy(a->a.getLob())).
                entrySet().stream().forEach(e->{
                    if(distributedCache.get(e.getKey(), null, CACHE_DOMAIN, false)==null || overrideOld)
                        addAll(e.getKey(),e.getValue());
                });
    }

    public List<EventListenerInfo> get(String lob) {
        return get(lob, false);
    }


    public List<EventListenerInfo> get(String lob, boolean isEnabledOnly) {
        List<EventListenerInfo> listeners = distributedCache.withCache(lob, null, CACHE_DOMAIN, s -> AllLOBRouter.loadAll(EventListenerInfo.class,
                k -> k.equalsIgnoreCase(lob)).stream().map(e -> (EventListenerInfo) e).collect(Collectors.toList()));
        if (lob != null && listeners != null && isEnabledOnly) {
            listeners = listeners.stream().filter(EventListenerInfo::isEnabled).collect(Collectors.toList());
        }
        return lob != null && listeners != null ?
                new ArrayList<>(listeners) : Collections.emptyList();
    }

    public List<EventListenerInfo> get(String lob, String topic) {
        return get(lob, eventListenerInfo -> eventListenerInfo.getTopic().equalsIgnoreCase(topic));
    }

    public void clear(String lob) {
        distributedCache.clearCache(lob, null, CACHE_DOMAIN);
    }
}