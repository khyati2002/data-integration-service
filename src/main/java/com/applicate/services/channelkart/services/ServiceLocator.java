package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.salescode.dim.utils.ReflectionUtils;
import org.apache.kafka.common.utils.CopyOnWriteMap;
import org.jooq.DSLContext;

import java.util.Map;
import java.util.Set;

public class ServiceLocator {

    private static final Map<Class<?>, CommonDataModelService<?>> SERVICE_REGISTRY = new CopyOnWriteMap<>();
    private static ServiceLocator instance;

    private final transient DSLContext dslContext;

    private ServiceLocator(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public static ServiceLocator getInstance(DSLContext dslContext) {
        AbstractCDMService.setDslContext(dslContext);
        if (instance == null) {
            synchronized (ServiceLocator.class) {
                if (instance == null) {
                    instance = new ServiceLocator(dslContext);
                }
            }
        }
        return instance;
    }

    public static <T extends CommonDataModel> CommonDataModelService<T> lookup(Class<T> cdmType) {
        CommonDataModelService<?> commonDataModelService = SERVICE_REGISTRY.get(cdmType);
        return (CommonDataModelService<T>) commonDataModelService;
    }

    private static <T> void register(Class<T> persistentClass, CommonDataModelService<?> abstractCDMService) {
        SERVICE_REGISTRY.put(persistentClass, abstractCDMService);
    }

    public void registerSubClasses() {
        Set<Class<? extends AbstractCDMService>> subClasses = ReflectionUtils.findSubClasses(AbstractCDMService.class);

        for (Class<? extends AbstractCDMService> serviceClass : subClasses) {
            try {
                // Create an instance using the default constructor
                AbstractCDMService serviceInstance = serviceClass.getDeclaredConstructor().newInstance();
                serviceInstance.setDslContext(dslContext);
                // Get the entity class it handles (assuming each service has a getPersistentClass() method)
                Class<?> persistentClass = serviceInstance.getPersistentClass();
                register(persistentClass, serviceInstance);
            } catch (Exception e) {
                System.err.println("Failed to register service: " + serviceClass.getName());
                e.printStackTrace();
            }
        }
    }

}
