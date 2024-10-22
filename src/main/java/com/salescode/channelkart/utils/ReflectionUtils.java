package com.salescode.channelkart.utils;


import com.salescode.channelkart.scanner.ExternalRegistryScanner;
import com.salescode.channelkart.services.SpringContext;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import javax.persistence.Transient;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.reflections.ReflectionUtils.getAllFields;

/**
 * @author : Jinu
 * Date    : 6/18/2020
 **/
public class ReflectionUtils {

   private static Reflections reflections = null;

   private static final Logger log = LoggerFactory.getLogger(ReflectionUtils.class);

   static {
      try {
         reflections = new Reflections("com.applicate");
      } catch (Exception e) {
         log.error("Could not load reflections:{}", e.getMessage());
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

   public static <T> T createInstance(String fullyQualifiedClassName, ClassLoader loader) {
      Class<T> clazz = loadClass(fullyQualifiedClassName, loader);
      return createInstance(clazz);
   }


   public static <T> T createInstance(Class<T> tClass) {
      try {
         return tClass.getDeclaredConstructor()
                 .newInstance();
      } catch (Exception e) {
         throw new ReflectionException("Could not create instance of class:" + tClass, e);
      }
   }

   public static <T> T createInstance(String fullyQualifiedClassName) {
      ExternalRegistryScanner scanner = ExternalRegistryScanner.getInstance();
      if (scanner.isExternal(fullyQualifiedClassName)) {
         return scanner.createObject(fullyQualifiedClassName);
      }
      Class<T> clazz = loadClass(fullyQualifiedClassName);
      return createInstance(clazz);
   }

   @SuppressWarnings("unchecked")
   public static <T> T createInstance(String fullyQualifiedClassName, Object... args) {
      Class<T> clazz = loadClass(fullyQualifiedClassName);
      try {
         Constructor<?>[] declaredConstructors = clazz.getDeclaredConstructors();
         return (T) findMatchingConstructor(declaredConstructors, args)
                 .newInstance(args);
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
                    index ++;
                 }
                 return true;
              }).findFirst()
              .orElseThrow(() -> new ReflectionException("Could not find matching constructor"));
   }


   public static <T> Optional<T> getFromSpring(String implementationClass) {
      try {
         Class<T> tClass = loadClass(implementationClass);
         var bean = SpringContext.getBean(tClass);
         return Optional.of(bean);
      } catch (Exception e) {
         return Optional.empty();
      }
   }

   public static boolean isFieldTypeMatching(Field field, Class<?> searchType) {
      return searchType.isAssignableFrom(getFieldOrCollectionGenericType(field));
   }

   public static Class<?> getFieldOrCollectionGenericType(Field field) {
      Class<?> type = field.getType();
      if (Collection.class.isAssignableFrom(type)) {
         Type genericType = field.getGenericType();
         if (genericType instanceof ParameterizedType) {
            Type[] actualTypes = ((ParameterizedType) genericType).getActualTypeArguments();
            if (actualTypes != null && actualTypes.length == 1) {
               String className = actualTypes[0].getTypeName();
               try {
                  return Class.forName(className);
               } catch (ClassNotFoundException e) {
                  log.warn("Could not load class:{} from field type :{}", className, field);
               }
            }
         }
      }
      return type;
   }

   public static <T> Optional<Class<? extends T>> findSubClassByName(Class<T> clazz, String name) {
      return reflections.getSubTypesOf(clazz)
              .stream()
              .filter(item -> item.getSimpleName().equalsIgnoreCase(name))
              .findFirst();
   }

   @SuppressWarnings("unchecked")
   public static Set<Class<?>> searchTypeFields(Class<?> targetClass, Class<?> searchType) {
      return getAllFields(targetClass, field -> isFieldTypeMatching(field, searchType))
              .stream()
              .filter(field -> isFieldTypeMatching(field, searchType))
              .map(ReflectionUtils::getFieldOrCollectionGenericType)
              .collect(Collectors.toSet());
   }

   public static <T> Set<Class<? extends T>> findSubClasses(Class<T> clazz) {
      return reflections.getSubTypesOf(clazz);
   }

   public static boolean hasClassNameMatches(String className, Object anyObject) {
      return className.equalsIgnoreCase(anyObject.getClass().getName());
   }

   @SuppressWarnings("unchecked")
   public static <T> Class<T> loadClass(String fullyQualifiedClassName) {
      try {
         return (Class<T>) Class.forName(fullyQualifiedClassName);
      } catch (ClassNotFoundException e) {
         throw new ReflectionException("could not load class for name:" + fullyQualifiedClassName, e);
      }
   }

   public static String[] getNullPropertyNames(Object source) {
      final BeanWrapper src = new BeanWrapperImpl(source);
      PropertyDescriptor[] pds = src.getPropertyDescriptors();
      Set<String> emptyNames = new HashSet<>();
      for (PropertyDescriptor pd : pds) {
         Object srcValue = src.getPropertyValue(pd.getName());
         if (srcValue == null) emptyNames.add(pd.getName());
      }
      String[] result = new String[emptyNames.size()];
      return emptyNames.toArray(result);
   }

   public static void copyNonNullProperties(Object src, Object target) {
      BeanUtils.copyProperties(src, target, getNullPropertyNames(src));
   }

   public static Map<String, Object> getProperties(Object bean) {
      try {
         Map<String, Object> map = new HashMap<>();
         Arrays.stream(Introspector.getBeanInfo(bean.getClass(), Object.class)
                         .getPropertyDescriptors())
                 // filter out properties with setters only
                 .filter(pd -> Objects.nonNull(pd.getReadMethod()))
                 .forEach(pd -> { // invoke method to get value
                    try {
                       Object value = pd.getReadMethod().invoke(bean);
                       if (value != null) {
                          map.put(pd.getName(), value);
                       }
                    } catch (Exception e) {
                       // add proper error handling here
                    }
                 });
         return map;
      } catch (IntrospectionException e) {
         // and here, too
         return Collections.emptyMap();
      }
   }

   public static boolean isNonStatic(Field field) {
      return !Modifier.isStatic(field.getModifiers());
   }

   public static boolean isNonTransient(Field field) {
      return !Modifier.isTransient(field.getModifiers()) && !hasAnnotation(field, Transient.class);
   }

   public static boolean isNonStaticAndNonTransient(Field field) {
      return isNonTransient(field) && isNonStatic(field);
   }

   @SafeVarargs
   public static <T extends Annotation> boolean hasAnnotation(Field field, Class<T>... annotations) {
      return Arrays.stream(annotations).anyMatch(annotation -> field.getAnnotation(annotation) != null);
   }

   @SuppressWarnings("unchecked")
   public static Set<Field> getInstanceFields(Class<?> target) {
      return getAllFields(target, ReflectionUtils::isNonStatic, ReflectionUtils::isNonTransient);
   }

   @SuppressWarnings("unchecked")
   public static Set<Field> getInstanceFields(Class<?> target, Set<String> exclude) {
      return getAllFields(target, ReflectionUtils::isNonStatic, ReflectionUtils::isNonTransient, field -> (!exclude.contains(field.getName())));
   }

   public static List<Object> getInstanceValuesSafely(Object bean) {
      Set<Field> instanceFields = getInstanceFields(bean.getClass());
      return instanceFields.stream().sorted(Comparator.comparing(Field::getName)).map(field -> readDataSafely(field, bean)).collect(Collectors.toList());
   }

   public static List<Object> extractInstanceValues(Object bean, Set<String> excludeNames) {
      Set<Field> instanceFields = getInstanceFields(bean.getClass(), excludeNames);
      return instanceFields.stream().sorted(Comparator.comparing(Field::getName)).map(field -> readData(field, bean)).collect(Collectors.toList());
   }

   public static Map<String, Object> extractProperties(Object bean, Set<String> excludeNames) {
      Set<Field> instanceFields = getInstanceFields(bean.getClass(), excludeNames);
      return instanceFields.stream().sorted(Comparator.comparing(Field::getName)).collect(Collectors.toMap(Field::getName, field -> readData(field, bean), (i, j) -> i, LinkedHashMap::new));
   }

   @SuppressWarnings("unchecked")
   public static <T> T readData(Object target, String fieldName) {
      try {
         return (T) FieldUtils.readField(target, fieldName, true);
      } catch (IllegalAccessException e) {
         throw new ReflectionException("Could not readData for field:" + fieldName, e);
      }
   }

   private static Object readDataSafely(Field field, Object bean) {
      try {
         return readData(field, bean);
      } catch (Exception e) {
         log.debug("Could not read field:{} form bean:{}", field, bean);
      }
      return null;
   }

   public static Object readData(Field field, Object bean) {
      try {
         return FieldUtils.readField(field, bean, true);
      } catch (Exception e) {
         throw new ReflectionException("Could not readData for field:" + field, e);
      }
   }

   @SuppressWarnings({"unchecked", "java:S3011"})
   public static <T> T  invokeMethod(Object object, String methodName, Object... args) {
      Class<?>[] classes = Arrays.stream(args)
              .map(Object::getClass)
              .toArray(Class[]::new);
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


}
