package com.applicate.services.channelkart.cache;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class CacheUpdateEvent implements Serializable {

    private static final long serialVersionUID = 7271540090699595148L;

    private String lob;

    private String domainName;

    private String key;

    private boolean clearAll;

    @Override
    public String toString() {
        return "CacheUpdateEvent{" +
                       "lob='" + lob + '\'' +
                       ", domainName='" + domainName + '\'' +
                       ", key='" + key + '\'' +
                       ", clearAll=" + clearAll +
                       '}';
    }
}
