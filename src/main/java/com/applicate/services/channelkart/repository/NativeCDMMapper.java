package com.applicate.services.channelkart.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.applicate.services.channelkart.client.properties.PropertyDefinition;
import com.applicate.services.channelkart.client.properties.PropertyRegistry;
import com.applicate.services.channelkart.commandline.utils.CommandLineDbUtils;
import com.applicate.services.channelkart.converters.DateToClientTimeZoneStringConverter;
import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Transient;
import java.beans.Introspector;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class NativeCDMMapper {

	private static final Logger log = LoggerFactory.getLogger(NativeCDMMapper.class);

	private static PropertyRegistry propertyRegistry = SpringContext.getBeanSafely(PropertyRegistry.class).orElse(null);

	private static ObjectMapper mapper = new ObjectMapper();

	private static Map<Class, Map<String, Method>> cdmFieldsMap = new ConcurrentHashMap<>();

	private static Map<Class<?>,Map<String, String>> camelCaseMap = new ConcurrentHashMap<>();

	private boolean includeCommonFields = false;

	private boolean roundOff = true;

	private EntityUtils entityUtils= EntityUtils.get();

	private Set<String> listOfIncludeCommonFields = new HashSet<>();

	public NativeCDMMapper() {
		mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
		mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
	}
	public NativeCDMMapper(boolean roundOff) {
		mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
		mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
		this.roundOff = roundOff;
	}

	public <T> T mapFields(Map<String, String> dataMap, Class<T> type, Map<String, String> customMap) throws Exception {
		ObjectNode jsonode = getMappedObjectNode(dataMap, type, customMap);
		long startTimeInternal=System.currentTimeMillis();
		try {
			return mapper.treeToValue(jsonode, type);
		}finally {
			//log.info("mapFields  execution time:-> "+(System.currentTimeMillis()-startTimeInternal));
		}
	}

	public <T> Object mapAllFields(Map<String, String> dataMap, Class<T> type, Class<?> returnType, Map<String, String> customMap) throws JsonProcessingException {
		ObjectNode jsonNode = getMappedObjectNodeAllFields(dataMap, type, customMap);
		return mapper.treeToValue(jsonNode, returnType);
	}

	private void fillJsonNode(Class rt, String camelCase, Object value, ObjectNode jsonode) {
		if(value==null)
			return;
		if (rt.equals(Date.class)) {
			try {
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
				format.setTimeZone(TimeZone.getTimeZone("UTC"));
				Date date = format.parse(value.toString());
				String cdmDateformat = new DateToClientTimeZoneStringConverter().convert(date);
				jsonode.put(camelCase, cdmDateformat);
			} catch (Exception e) {
				log.error("Could not fill json", e);
			}

		} else

		if (rt.equals(Boolean.class) || rt.equals(boolean.class)) {

			jsonode.put(camelCase, Boolean.valueOf(value.toString()));

		} else if (rt.equals(Double.class) || rt.equals(double.class)) {

			jsonode.put(camelCase, Double.valueOf(value.toString()));

		} else if (rt.equals(Integer.class) || rt.equals(int.class)) {

			jsonode.put(camelCase, Integer.valueOf(value.toString()));

		} else if (rt.equals(Long.class) || rt.equals(long.class)) {

			jsonode.put(camelCase, Long.valueOf(value.toString()));

		} else if (rt.equals(Float.class) || rt.equals(float.class)) {
			jsonode.put(camelCase, Float.valueOf(value.toString()));
		}
		else if (rt.equals(ArrayNode.class) || rt.equals(JsonNode.class)) {
			try {
				jsonode.set(camelCase, JSONUtils.getObjectMapper().readTree(value.toString()));
			} catch (IOException e) {
				log.error("Could not set json", e);
			}
		} else {
			jsonode.put(camelCase, value.toString());
		}
	}

	private String toCamelCase(String s) {
		String[] parts = s.split("_");
		String camelCaseString = parts[0];
		for (int i = 1; i < parts.length; i++) {
			camelCaseString = camelCaseString + toProperCase(parts[i]);
		}
		return camelCaseString;
	}

	private String toProperCase(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}

	private Map<String, Method> generateFieldsMap(Class type) {
		Map<String, Method> fieldMap = new HashMap<String, Method>();
		for (Method m : type.getMethods()) {
			String name = m.getName();
			if ((name.startsWith("get") || name.startsWith("is")) && m.getParameterCount() == 0 && !m.isAnnotationPresent(Transient.class)) {
				fieldMap.put(methodToField(name), m);
			}
		}
		return fieldMap;
	}

	private String methodToField(String methodName) {
		return Introspector.decapitalize(methodName.substring(methodName.startsWith("is") ? 2 : 3));
	}

	public HashMap<String, String> getDataMap(ResultSet rs) throws SQLException {
		HashMap<String, String> dataMap = new LinkedHashMap<>();
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		for (int i = 1; i <= columnCount; i++) {
			try {
				var item = roundDoubleValue(rs.getObject(i));
				dataMap.put(rsmd.getColumnLabel(i), item != null ? toSafeString(item) : null);
			} catch (Exception e) {
				if(e instanceof SQLException &&
						e.getMessage().equalsIgnoreCase("Zero date value prohibited")) {
					log.warn("Found {} as 0000-00-00 00:00:00.000000 in {}. This is a unexpected value and need correction.",
							rsmd.getColumnLabel(i), rsmd.getTableName(i));
				}else {
					log.error("Error while evaluating resultset through native cdm mapper",e);
				}
			}
		}
		return dataMap;
	}

	private Object roundDoubleValue(Object item) {
		if(item instanceof Double && roundOff) {
			if (Boolean.TRUE.equals(isRoundOffDoubleValue())) {
				return BigDecimal.valueOf((Double) item).setScale(2, RoundingMode.CEILING).toPlainString();
			} else {
				return BigDecimal.valueOf((Double) item).setScale(2, RoundingMode.DOWN).stripTrailingZeros().toPlainString();
			}
		}
		return item;
	}

	private Boolean isRoundOffDoubleValue () {
		if(propertyRegistry==null) {
			JsonNode clientProperty = CommandLineDbUtils.getClientProperty(PropertyDefinition.USE_ROUNDOFF.getName());
			if (clientProperty == null) {
				clientProperty = JSONUtils.toJsonNode(PropertyDefinition.USE_ROUNDOFF.getDefaultValue());
			}
			return clientProperty != null && Boolean.parseBoolean(clientProperty.asText());
		}
		else {
			return propertyRegistry.getAsBoolean(PropertyDefinition.USE_ROUNDOFF);
		}
	}

	public HashMap<String, HashMap<String,String>> getJoinedDataMap(ResultSet rs) throws SQLException{
		HashMap<String, HashMap<String,String>> dataMap = new LinkedHashMap<>();
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		for (int i = 1; i <= columnCount; i++) {
			try {
				String nativeTableName= rs.getMetaData().getTableName(i);
				var item = rs.getObject(i);
				if(item instanceof Double) {
					item = new BigDecimal((Double)item).setScale(2, RoundingMode.CEILING).toPlainString();
				}
				HashMap<String,String> dMap= dataMap.getOrDefault(nativeTableName, new HashMap<String,String>());
				dMap.put(rsmd.getColumnLabel(i), item != null ? toSafeString(item) : null);
				dataMap.put(nativeTableName,dMap);
			} catch (Exception e) {
				log.error("stacktrace", e);
			}
		}
		return dataMap;
	}


	public <T> Map<String,Object> mapToEntityFields(Map<String, String> dataMap, Class<T> type, Map<String, String> customMap) throws Exception {
		ObjectNode jsonode= getMappedObjectNode(dataMap,type,customMap);
		long startTimeInternal=System.currentTimeMillis();
		try {
			return mapper.convertValue(jsonode, new TypeReference<Map<String, Object>>() {
			});
		}finally {
			//log.info("mapToEntityFields conversion fetch time:-> "+(System.currentTimeMillis()-startTimeInternal));
		}
	}

	private <T> ObjectNode getMappedObjectNode(Map<String, String> dataMap, Class<T> type, Map<String, String> customMap) {

		ObjectNode jsonode = mapper.createObjectNode();

		Map<String, Method> fieldsMap = cdmFieldsMap.get(type);

		if (fieldsMap == null) {
			fieldsMap = generateFieldsMap(type);
			cdmFieldsMap.put(type, fieldsMap);
		}

		Map<String, Method> commonFields = cdmFieldsMap.get(CommonDataModel.class);

		if (commonFields == null) {
			commonFields = generateFieldsMap(CommonDataModel.class);
			cdmFieldsMap.put(CommonDataModel.class, commonFields);
		}

		for (Entry<String, String> entry : dataMap.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			String camelCase = camelCaseMap.computeIfAbsent(type, a->new HashMap<>()).get(key);
			if (customMap != null && customMap.containsKey(key)) {
				camelCase = customMap.get(key);
			}
			if (camelCase == null) {
				camelCase = toCamelCase(key);
				if(fieldsMap.get(camelCase) == null) {
					try {
						Field field = entityUtils.getClassField(type, key);
						if (field != null) {
							camelCase = field.getName();
						}
					} catch (Exception e) {
						log.info("could not find field:{}", camelCase);
					}
				}
				camelCaseMap.get(type).put(key, camelCase);
			}
			Method m = fieldsMap.get(camelCase);
			if (m != null) {
				Class rt = m.getReturnType();
				boolean isToSetField = includeCommonFields || !commonFields.containsKey(camelCase) || camelCase.equals("activeStatus") || camelCase.equals("id") || camelCase.equals("accessibleBy") || camelCase.equals("extendedAttributes");
				isToSetField = checkListOfCommonFields(isToSetField, camelCase);
				if (isToSetField) {
					fillJsonNode(rt, camelCase, value, jsonode);
				}
			}
		}
		return jsonode;
	}

	private boolean checkListOfCommonFields(boolean isToSetField, String camelCase) {
		return isToSetField || listOfIncludeCommonFields.contains(camelCase);
	}

	@SuppressWarnings("java:S3776")
	private <T> ObjectNode getMappedObjectNodeAllFields(Map<String, String> dataMap, Class<T> type, Map<String, String> customMap) {

		ObjectNode jsonNode = mapper.createObjectNode();

		Map<String, Method> fieldsMap = cdmFieldsMap.get(type);

		if (fieldsMap == null) {
			fieldsMap = generateFieldsMap(type);
			cdmFieldsMap.put(type, fieldsMap);
		}

		Map<String, Method> commonFields = cdmFieldsMap.get(CommonDataModel.class);

		if (commonFields == null) {
			commonFields = generateFieldsMap(CommonDataModel.class);
			cdmFieldsMap.put(CommonDataModel.class, commonFields);
		}

		for (Entry<String, String> entry : dataMap.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			String camelCase = camelCaseMap.computeIfAbsent(type, a->new HashMap<>()).get(key);
			if (customMap != null && customMap.containsKey(key)) {
				camelCase = customMap.get(key);
			}
			if (camelCase == null) {
				camelCase = toCamelCase(key);
				if(fieldsMap.get(camelCase) == null) {
					try {
						Field field = entityUtils.getClassField(type, key);
						if (field != null) {
							camelCase = field.getName();
						}
					} catch (Exception e) {
						log.info("could not find field:{}", camelCase);
					}
				}
				camelCaseMap.get(type).put(key, camelCase);
			}
			Method m = fieldsMap.get(camelCase);
			if (m != null) {
				Class<?> rt = m.getReturnType();
				fillJsonNode(rt, camelCase, value, jsonNode);
			}
		}
		return jsonNode;
	}

	public Map<String, String> mapRow(ResultSet resultSet) throws SQLException {
		Map<String, String> dataMap = new HashMap<>();
		ResultSetMetaData rsmd = resultSet.getMetaData();
		int columnCount = rsmd.getColumnCount();
		for (int rowCount = 1; rowCount <= columnCount; rowCount++) {
			try {
				dataMap.put(rsmd.getColumnLabel(rowCount),
						resultSet.getObject(rowCount) != null ? toSafeString(resultSet.getObject(rowCount)) : null);
			} catch (Exception e) {
				log.error("Could not map row", e);
			}
		}
		return Collections.unmodifiableMap(dataMap);
	}

	public static String toSafeString(Object item) {
		if (item == null) {
			return null;
		}
		if(item instanceof LocalDateTime) {
			return Timestamp.valueOf((LocalDateTime) item).toString();
		}
		return item.toString();
	}

	public boolean isIncludeCommonFields() {
		return includeCommonFields;
	}

	public void setIncludeCommonFields(boolean includeCommonFields) {
		this.includeCommonFields = includeCommonFields;
	}

	public Set<String> getListOfIncludeCommonFields() {
		return listOfIncludeCommonFields;
	}

	public void setListOfIncludeCommonFields(Set<String> listOfIncludeCommonFields) {
		this.listOfIncludeCommonFields = listOfIncludeCommonFields;
	}

	public void addValueInListOfIncludeCommonFields(String value) {
		this.listOfIncludeCommonFields.add(value);
	}
}
