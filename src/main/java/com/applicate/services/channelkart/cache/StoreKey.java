package com.applicate.services.channelkart.cache;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

@Setter
@Getter
public class StoreKey implements Serializable{

    private static final long serialVersionUID = 1L;


    private String key;

    private Map<String,String> attributes;

    private String lob;

    private String domain;

    private String blobKey;

    private long lastModified;

}
