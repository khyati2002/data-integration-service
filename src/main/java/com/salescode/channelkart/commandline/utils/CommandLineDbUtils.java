package com.salescode.channelkart.commandline.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.CaseFormat;
import com.salescode.channelkart.abstractdatasource.DatabaseProfileRegistry;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.models.MetaData;
import com.salescode.channelkart.querys.CustomizedJdbcTemplate;
import com.salescode.channelkart.repository.NativeCDMMapper;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.utils.JSONUtils;
import com.salescode.channelkart.utils.JdbcUtils;
import com.salescode.channelkart.utils.ReflectionUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

public class CommandLineDbUtils {

    private static final Logger log = LoggerFactory.getLogger(CommandLineDbUtils.class);

    private final DataSource dataSource;

    private final NativeCDMMapper cdmMapper = new NativeCDMMapper();

    public CommandLineDbUtils(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static HashMap<String, Object> localCache = new HashMap<>();

    public MetaData getMetaData(String domainName, String domainType) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        try {
            Map<String, Object> dbRecord = jdbcTemplate.queryForMap("select * from ck_metadata where domain_name =? and domain_type = ?", domainName, domainType);
            return map(dbRecord, MetaData.class);
        } catch (EmptyResultDataAccessException e) {
            // not need to handle this, if there is no record present then this exception will throw
            return null;
        }
    }

    public static JsonNode getClientProperty(String propertyName) {
        String key= SecurityContextUtils.getLob()+propertyName;
        if(localCache!=null && localCache.containsKey(key)){
            return (JsonNode) localCache.get(key);
        }
        String clientPropertyQuery = "SELECT domain_values FROM ck_metadata WHERE domain_name = 'client' and domain_type = 'properties'";
        JdbcTemplate jdbcTemplate= JdbcUtils.createJdbcTemplate((DataSource) DatabaseProfileRegistry.getDataSourceHashMap().get(
                SecurityContextUtils.getLob()));
        try{
            Map<String, Object> domainValues = jdbcTemplate.queryForMap(clientPropertyQuery);
            JsonNode domainValueNode = JSONUtils.getObjectMapper().readTree(domainValues.values().toArray()[0].toString());

            JsonNode clientPropertyValue = null;
            for (JsonNode node : domainValueNode) {
                Iterator<Map.Entry<String, JsonNode>> fieldIterator = node.fields();
                Map.Entry<String, JsonNode> name = fieldIterator.next();
                if(name.getValue().asText().equalsIgnoreCase(propertyName)){
                    Map.Entry<String, JsonNode> value = fieldIterator.next();
                    clientPropertyValue = value.getValue();
                }
            }
            if(localCache==null){
                localCache = new HashMap<>();
            }
            localCache.put(key, clientPropertyValue);
            return clientPropertyValue;
        } catch (Exception ex){
            log.error("Unable to read client property. Reason: ", ex);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getProperty(Object object, String propertyName) {
        try {
            return (T) PropertyUtils.getProperty(object, propertyName);
        } catch (Exception e) {
            throw new IllegalStateException("Could not get property:" + propertyName + " from object:" + object, e);
        }
    }

    public static void setProperty(Object object, String propertyName, Object value) {
        try {
            PropertyUtils.setProperty(object, propertyName, value);
        } catch (Exception e) {
            throw new IllegalStateException("Could not set property:" + propertyName + " to object:" + object + ", value:" + value, e);
        }
    }

    public static Class<?> getPropertyType(Object instance, String property) {
        try {
            return PropertyUtils.getPropertyType(instance, property);
        } catch (Exception e) {
            throw new ReflectionUtils.ReflectionException(e);
        }
    }

    public static <T> T createInstanceWithArguements(Class<T> tClass, Object... args) {
        Class<?>[] classes = Arrays.stream(args).map(Object::getClass).toArray(Class<?>[]::new);
        try {
            return tClass.getDeclaredConstructor(classes).newInstance(args);
        } catch (Exception e) {
            throw new ReflectionUtils.ReflectionException("Could not create instance of class:" + tClass + " with args:" + Arrays.toString(args), e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T extends CommonDataModel> T map(Map<String, Object> input, Class<T> clazz) {
        T instance = createInstanceWithArguements(clazz);
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String property = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, entry.getKey());
            Object value = entry.getValue();
            if (value != null) {
                Class<?> propertyType = getPropertyType(instance, property);
                if (propertyType.isAssignableFrom(ArrayNode.class)) {
                    value = JSONUtils.parse(value.toString());
                }
                if (propertyType.isEnum()) {
                    value = Enum.valueOf((Class<Enum>)propertyType, value.toString().toUpperCase());
                }
                if (propertyType == Date.class) {
                    value = parseDate(value);
                }
                setObjectProperty(instance, property, value);
            }
        }
        return instance;
    }

    private void setObjectProperty(Object instance, String property, Object value) {
        try {
            setProperty(instance, property, value);
        } catch (Exception e) {
            if(e.getCause().getClass() == NoSuchMethodException.class) {
                log.debug("Could not find setter for property:{}",property);
            } else {
                throw e;
            }
        }
    }

    private Date parseDate(Object value) {
        if (value instanceof LocalDateTime) {
            return Date.from(((LocalDateTime) value).toInstant(ZoneOffset.UTC));
        } else {
            return new Date(((Timestamp)value).getTime());
        }
    }

}
