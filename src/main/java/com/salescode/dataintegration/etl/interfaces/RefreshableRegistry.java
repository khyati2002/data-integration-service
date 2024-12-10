package com.salescode.dataintegration.etl.interfaces;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public interface RefreshableRegistry extends BeanPostProcessor {

    @Override
    default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        init();
        return bean;
    }

    default void init(){};

    void refreshRegistry();

    default String getRegistryName() {
        return RefreshableRegistry.this.getClass().getSimpleName();
    }

}
