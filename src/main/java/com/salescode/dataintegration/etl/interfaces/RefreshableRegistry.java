package com.salescode.dataintegration.etl.interfaces;

public interface RefreshableRegistry {

    void refreshRegistry();

    default String getRegistryName() {
        return RefreshableRegistry.this.getClass().getSimpleName();
    }

}
