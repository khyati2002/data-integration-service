package com.salescode.channelkart.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class SpringContext implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(SpringContext.class);
     
    private static ApplicationContext context;

    private static final Map<Class<?>, Object> secondaryContext = new HashMap<>();

    public static boolean isContextInitialized() {
        return context!=null;
    }

    /**
     * Returns the Spring managed bean instance of the given class type if it exists.
     * Returns null otherwise.
     * @param beanClass
     * @return
     */
    public static <T> T getBean(Class<T> beanClass) {
        try {
            return context.getBean(beanClass);
        } catch (Exception e) {
            T instance = getFromSecondaryContext(beanClass);
            if (instance != null) {
                return instance;
            }
            throw e;
        }

    }

    public static <T> Optional<T> getBeanSafely(Class<T> clazz) {
        try {
            return Optional.of(getBean(clazz));
        } catch (Exception e) {
            log.info("Could not find class {} from spring context", clazz);
        }
        return Optional.empty();
    }

    public static void register(Class<?> clazz, Object instance) {
        secondaryContext.put(clazz, instance);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getFromSecondaryContext(Class<T> theClass) {
        return (T) secondaryContext.get(theClass);
    }
     
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        // store ApplicationContext reference to access required beans later on
        setContext(context);
    }

    private static synchronized void setContext(ApplicationContext c){
        context = c;
    }

}