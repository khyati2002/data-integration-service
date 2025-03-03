package com.applicate.services.channelkart.services;

import com.salescode.dim.utils.ReflectionUtils;
import org.jooq.DSLContext;

import java.util.Set;

public class RegisterClassesService {

    private final DSLContext dsl;

    private static RegisterClassesService instance;

    public RegisterClassesService(DSLContext dsl){
        this.dsl = dsl;
    }

    public static RegisterClassesService getInstance(DSLContext dsl){
        if(instance == null){
            instance = new RegisterClassesService(dsl);
        }
        return instance;
    }

    public void registerSubClasses(){
        Set<Class<? extends AbstractCDMService>> subClasses = ReflectionUtils.findSubClasses(AbstractCDMService.class);

        for (Class<? extends AbstractCDMService> serviceClass : subClasses) {
            try {
                // Create an instance using the default constructor
                AbstractCDMService serviceInstance = serviceClass.getDeclaredConstructor(DSLContext.class).newInstance(dsl);

                // Get the entity class it handles (assuming each service has a getPersistentClass() method)
                Class<?> persistentClass = serviceInstance.getPersistentClass();

                // Register the service
                ServiceLocator.register(persistentClass, serviceInstance);
            } catch (Exception e) {
                System.err.println("Failed to register service: " + serviceClass.getName());
                e.printStackTrace();
            }
        }
    }
}
