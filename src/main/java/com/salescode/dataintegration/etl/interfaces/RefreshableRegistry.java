package com.salescode.dataintegration.etl.interfaces;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;

public abstract class RefreshableRegistry  {

    @PostConstruct
    public void init(){};

    public abstract void refreshRegistry();

    public String getRegistryName() {
        return RefreshableRegistry.this.getClass().getSimpleName();
    }

}
