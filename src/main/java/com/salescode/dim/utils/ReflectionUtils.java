package com.salescode.dim.utils;


import org.apache.commons.lang3.reflect.FieldUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.reflections.ReflectionUtils.getAllFields;

/**
 * @author : Jinu
 * Date    : 6/18/2020
 **/
public class ReflectionUtils {

    private static final Logger log = LoggerFactory.getLogger(ReflectionUtils.class);
    private static Reflections reflections = null;

    static {
        try {
            reflections = new Reflections("com.salescode.dim");
        } catch (Exception e) {
            log.error("Could not load reflections:{}", e.getMessage());
        }
    }

    private ReflectionUtils() {
        throw new IllegalStateException("Util class");
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> loadClass(String className, ClassLoader loader) {
        try {
            return (Class<T>) Class.forName(className, true, loader);
        } catch (Exception e) {
            throw new ReflectionException("Could not load class: " + className, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T createInstance(String fullyQualifiedClassName, Object... args) {
        Class<T> clazz = loadClass(fullyQualifiedClassName);
        try {
            Constructor<?>[] declaredConstructors = clazz.getDeclaredConstructors();
            return (T) findMatchingConstructor(declaredConstructors, args).newInstance(args);
        } catch (Exception e) {
            throw new ReflectionException("Could not create instance for className:" + fullyQualifiedClassName, e);
        }
    }

    private static Constructor<?> findMatchingConstructor(Constructor<?>[] constructors, Object... args) {
        return Arrays.stream(constructors).filter(c -> c.getParameterCount() == args.length).filter(constructor -> {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            int index = 0;
            for (Class<?> parameterType : parameterTypes) {
                if (!parameterType.isAssignableFrom(args[index].getClass())) {
                    return false;
                }
                index++;
            }
            return true;
        }).findFirst().orElseThrow(() -> new ReflectionException("Could not find matching constructor"));
    }

    public static <T> Set<Class<? extends T>> findSubClasses(Class<T> clazz) {
        return reflections.getSubTypesOf(clazz);
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> loadClass(String fullyQualifiedClassName) {
        try {
            return (Class<T>) Class.forName(fullyQualifiedClassName);
        } catch (ClassNotFoundException e) {
            throw new ReflectionException("could not load class for name:" + fullyQualifiedClassName, e);
        }
    }


    @SuppressWarnings({"unchecked", "java:S3011"})
    public static <T> T invokeMethod(Object object, String methodName, Object... args) {
        Class<?>[] classes = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);
        Method declaredMethod = null;
        try {
            declaredMethod = getMethod(object, methodName, classes);
            // this needs to be done so just suppressing the warnings
            declaredMethod.setAccessible(true);
            return (T) declaredMethod.invoke(object, args);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    private static Method getMethod(Object object, String methodName, Class<?>[] classes) {
        Method method = null;
        try {
            method = object.getClass().getMethod(methodName, classes);
        } catch (NoSuchMethodException e) {
            // if the method is not available then may be the method using some kind of generic arguments
            // methodName(T arg1, <D extends Serializable> arg2)
            method = Arrays.stream(object.getClass().getMethods())
                           .filter(m -> m.getName().equals(methodName))
                           .filter(m -> matchArgument(m, classes))
                           .findFirst()
                           .orElseThrow(() -> new ReflectionException("Could not find method:" + methodName));
        }
        return method;
    }

    private static boolean matchArgument(Method m, Class<?>[] classes) {
        int parameterCount = m.getParameterCount();
        if (parameterCount != classes.length) {
            // if the argument count is not matching then its not the same method
            return false;
        }
        Class<?>[] parameterTypes = m.getParameterTypes();
        // if the argument count is matching then we need to find whether the classes are
        // assigned to the method signature parameters or not
        for (int i = 0; i < parameterCount; i++) {
            if (!parameterTypes[i].isAssignableFrom(classes[i])) {
                // if it's not matching then this is not the matching method
                return false;
            }
        }
        return true;
    }

    public static List<Object> extractInstanceValues(Object bean, Set<String> excludeNames) {
        Set<Field> instanceFields = getInstanceFields(bean.getClass(), excludeNames);
        return instanceFields.stream().sorted(Comparator.comparing(Field::getName)).map(field -> readData(field, bean)).collect(Collectors.toList());
    }

    public static Set<Field> getInstanceFields(Class<?> target, Set<String> exclude) {
        return getAllFields(target, ReflectionUtils::isNonStatic, ReflectionUtils::isNonTransient, field -> (!exclude.contains(field.getName())));
    }

    public static boolean isNonStatic(Field field) {
        return !Modifier.isStatic(field.getModifiers());
    }

    public static boolean isNonTransient(Field field) {
        return !Modifier.isTransient(field.getModifiers()) ;// && !hasAnnotation(field, Transient.class);
    }

    public static Object readData(Field field, Object bean) {
        try {
            return FieldUtils.readField(field, bean, true);
        } catch (Exception e) {
            throw new ReflectionException("Could not readData for field:" + field, e);
        }
    }

    public static <T> T readData(Object target, String fieldName) {
        try {
            return (T) FieldUtils.readField(target, fieldName, true);
        } catch (IllegalAccessException e) {
            throw new ReflectionException("Could not readData for field:" + fieldName, e);
        }
    }

    public static class ReflectionException extends RuntimeException {

        private static final long serialVersionUID = -1591859483300702507L;

        public ReflectionException() {
        }

        public ReflectionException(String message) {
            super(message);
        }

        public ReflectionException(String message, Throwable cause) {
            super(message, cause);
        }

        public ReflectionException(Throwable cause) {
            super(cause);
        }

    }


}
