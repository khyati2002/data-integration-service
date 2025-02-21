package com.salescode.dim.interfaces;

public interface RefreshableRegistry { //extends BeanPostProcessor {

    default void init(){};

    void refreshRegistry();

    default String getRegistryName() {
        return RefreshableRegistry.this.getClass().getSimpleName();
    }

}
