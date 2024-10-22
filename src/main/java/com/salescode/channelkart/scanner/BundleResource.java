package com.salescode.channelkart.scanner;

import lombok.Getter;
import lombok.Setter;

/***
 * Dhaneesh
 */
@SuppressWarnings("rawtypes")
@Getter
@Setter
public class BundleResource {

    private String type;

    private String implementation;

    private String status = "Active";

    private ClassLoader loader;

    private Class<Object> clazz;

    private String lob;

    public BundleResource setClazz(Class<Object> clazz) {
        this.clazz = clazz;
        return this;
    }

    public BundleResource setLoader(ClassLoader loader) {
        this.loader = loader;
        return this;
    }

}
