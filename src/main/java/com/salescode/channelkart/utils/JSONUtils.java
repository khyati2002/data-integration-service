/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.utils;

//import com.applicate.services.channelkart.exceptions.TransformationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.salescode.channelkart.exceptions.CustomRuntimeException;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The class JSONUtils.
 *
 * @author Manish Srivastava
 * @since May 2020
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class JSONUtils {

   /*
    * Private constructor to resolve sonar issue
    */
   private JSONUtils(){}

   private static final String EXCEPTION = "Exception : ";

   public static final TypeReference<Map<String, String>> STRING_VALUE_MAP_REFERENCE = new TypeReference<>() {
   };

   private static final TypeReference<List<String>> LIST_STRING_REFERENCE = new TypeReference<>() { };


   public static final TypeReference<Map<String, Object>> OBJECT_VALUE_MAP_REFERENCE = new TypeReference<>() {
   };

   /**
    * The Constant OBJECT_MAPPER.
    */
   private static ObjectMapper OBJECT_MAPPER;

   /**
    * The Constant LOGGER.
    */
   private static final Logger LOGGER = LoggerFactory.getLogger(JSONUtils.class);

   private static ObjectMapper get() {
      if (OBJECT_MAPPER == null) {
         OBJECT_MAPPER = new ObjectMapper();
         OBJECT_MAPPER.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
         OBJECT_MAPPER.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
         OBJECT_MAPPER.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
      }
      return OBJECT_MAPPER;
   }

   static {
      get();
   }

   /**
    * Gets the object mapper.
    *
    * @return the object mapper
    */
   public static ObjectMapper getObjectMapper() {
      return get();
   }

   /**
    * Convert to pojo.
    *
    * @param <T>        the generic type
    * @param jsonString the json string
    * @param tClass     the t class
    * @return the t
    * @throws IOException Signals that an I/O exception has occurred.
    */
   public static <T> T convertToPojo(JSONObject jsonString, Class<T> tClass) {
      try {
		return OBJECT_MAPPER.readValue(jsonString.toString(), tClass);
	} catch (JsonProcessingException e) {
//		throw new TransformationException(e,"Error while converting JSONObject to Pojo {}",tClass.toString());
		throw new CustomRuntimeException(e,"Error while converting JSONObject to Pojo {}",tClass.toString());
	}
   }

   /**
    * Gets the value.
    *
    * @param jsonObj the json obj
    * @param keys    the keys
    * @return the value
    */
   public static Object getValue(JSONObject jsonObj, String keys) {
      try {
         String keysArr[] = keys.split(Pattern.quote("||"));
         for (String string : keysArr) {
            if (jsonObj.has(string)) {
               return jsonObj.get(string);
            }
         }
      } catch (Exception ex) {
         LOGGER.error(EXCEPTION, ex);
      }
      return null;
   }

   /**
    * To list.
    *
    * @param jsonArr the json arr
    * @return the list
    * @throws JSONException the JSON exception
    */
   public static List toList(JSONArray jsonArr) throws JSONException {
      List arrList = new ArrayList(jsonArr.length());
      for (int i = 0; i < jsonArr.length(); i++) {
         Object element = jsonArr.get(i);
         arrList.add(element);
      }
      return arrList;
   }

   /**
    * To JSON array.
    *
    * @param list the list
    * @return the JSON array
    * @throws JSONException the JSON exception
    */
   public static JSONArray toJSONArray(List<?> list) throws JSONException {
      JSONArray arr = new JSONArray();
      for (int i = 0; i < list.size(); i++) {
         arr.put(list.get(i));
      }
      return arr;
   }

   /**
    * To string array.
    *
    * @param jsonArr the json arr
    * @return the string[]
    * @throws JSONException the JSON exception
    */
   public static String[] toStringArray(JSONArray jsonArr) throws JSONException {
      String[] strArr = new String[jsonArr.length()];
      for (int i = 0; i < jsonArr.length(); i++) {
         strArr[i] = jsonArr.getString(i);
      }
      return strArr;
   }


   /**
    * To string.
    *
    * @param jsonArr the json arr
    * @return the string
    * @throws JSONException the JSON exception
    */
   public static String toString(JSONArray jsonArr) throws JSONException {
      StringBuilder strArr = new StringBuilder();
      for (int i = 0; i < jsonArr.length(); i++) {
         if (i > 0) {
            strArr.append(",");
         }
         strArr.append(jsonArr.getString(i));
      }
      return new String(strArr);
   }

   /**
    * To string.
    *
    * @param list the list
    * @return the string
    * @throws JSONException the JSON exception
    */
   public static String toString(List<?> list) throws JSONException {
      return toString(toJSONArray(list));
   }

   /**
    * To JSON array.
    *
    * @param coll the coll
    * @return the JSON array
    * @throws JSONException the JSON exception
    */
   public static JSONArray toJSONArray(Collection coll) throws JSONException {
      return fillJSONArray(new JSONArray(), coll);
   }

   /**
    * Fill JSON array.
    *
    * @param jsonArr the json arr
    * @param coll    the coll
    * @return the JSON array
    */
   public static JSONArray fillJSONArray(JSONArray jsonArr, Collection coll) {
      for (Object obj : coll) {
         jsonArr.put(obj);
      }
      return jsonArr;
   }

   /**
    * Safe to JSON array.
    *
    * @param collection the collection
    * @return the JSON array
    */
   public static JSONArray safeToJSONArray(Collection collection) {
      try {
         return toJSONArray(collection);
      } catch (JSONException e) {
         LOGGER.error("Exception happened while converting collection to JSONArray", e);
      }
      return null;
   }

   /**
    * To JSON array.
    *
    * @param arr the arr
    * @return the JSON array
    * @throws JSONException the JSON exception
    */
   public static JSONArray toJSONArray(Object[] arr) throws JSONException {
      return fillJSONArray(new JSONArray(), arr);
   }

   /**
    * Safe to JSON array.
    *
    * @param arr the arr
    * @return the JSON array
    */
   public static JSONArray safeToJSONArray(Object[] arr) {
      try {
         return toJSONArray(arr);
      } catch (JSONException e) {
         LOGGER.error(e.getMessage(), e);
      }
      return null;
   }

   /**
    * Fill JSON array.
    *
    * @param jsonArr the json arr
    * @param arr     the arr
    * @return the JSON array
    * @throws JSONException the JSON exception
    */
   public static JSONArray fillJSONArray(JSONArray jsonArr, Object[] arr) throws JSONException {
      for (Object obj : arr) {
         jsonArr.put(obj);
      }
      return jsonArr;
   }

   /**
    * Merge JSON.
    *
    * @param objects the objects
    * @return the JSON object
    * @throws JSONException the JSON exception
    */
   public static JSONObject mergeJSON(JSONObject... objects) throws JSONException {
      JSONObject mergeJson = objects[0];
      for (int i = 1; i < objects.length; ++i) {
         JSONObject json = objects[i];
         if (json != null && json.length() > 0) {
            String[] keys = JSONObject.getNames(json);
            for (String key : keys) {
               mergeJson.put(key, json.get(key));
            }
         }
      }
      return mergeJson;
   }

   public static ObjectNode mergeObejctNode(ObjectNode... objects) throws JSONException {
      ObjectNode mergeJson = objects[0];
      for (int i = 1; i < objects.length; ++i) {
         ObjectNode json = objects[i];
         if (json != null && json.size() > 0) {
            Iterator<String> keys = json.fieldNames();
            while (keys.hasNext()) {
               String key = keys.next();
               mergeJson.put(key, json.get(key));
            }
         }
      }
      return mergeJson;
   }
   
   	/**
   	 * Merge two JsonNodes.
   	 *
   	 * @param source the JsonNode
   	 * @param destination the JsonNode
   	 * @return mainNode the JsonNode
   	 * @throws JSONException
   	 * @throws IOException
   	 */
   	    public static JsonNode mergeJsonNodes(JsonNode source, JsonNode destination) throws  JSONException, IOException{
   
   	    	ObjectNode destinationNode = destination.deepCopy();
   	    	
   	        Iterator<String> fieldNames = source.fieldNames();
   	        while (fieldNames.hasNext()) {
  
   	            String fieldName = fieldNames.next();
   	            JsonNode jsonNode = destinationNode.get(fieldName);
   
   	            if (jsonNode != null && jsonNode.isObject()) {
                   JsonNode value=mergeJsonNodes(source.get(fieldName),jsonNode);
                   destinationNode.set(fieldName, value);
   	            }
   	            else {
   	                if (destinationNode instanceof ObjectNode) {
   
   	                    JsonNode value = source.get(fieldName);
                           destinationNode.set(fieldName, value);

   	                }
   	            }
   
   	        }
   
   	        return destinationNode;
   	    }

   /**
    * Merge JSO nspecified keys.
    *
    * @param objects the objects
    * @return the JSON object
    * @throws JSONException the JSON exception
    */
   public static JSONObject mergeJSONspecifiedKeys(Object... objects) throws JSONException {
      JSONObject mergeJson = (JSONObject) objects[0];
      JSONObject json = (JSONObject) objects[1];
      String[] specifiedKeys = objects[2].toString().split(",");
      if (json != null) {
         for (String key : specifiedKeys) {
            if (json.has(key)) {
               mergeJson.put(key, json.get(key));
            }
         }
      }
      return mergeJson;
   }

   /**
    * Merge JSON extra keys.
    *
    * @param objects the objects
    * @return the JSON object
    * @throws JSONException the JSON exception
    */
   public static JSONObject mergeJSONExtraKeys(Object... objects) throws JSONException {
      JSONObject mergeJson = (JSONObject) objects[0];
      Iterator<String> keys = null;
      String key = null;

      for (int i = 1; i < objects.length; i++) {
         JSONObject json = (JSONObject) objects[i];
         keys = json.keys();
         while (keys.hasNext()) {
            key = keys.next();
            if (!mergeJson.has(key)) {
               mergeJson.put(key, json.get(key));
            }
         }
      }
      return mergeJson;
   }

   /**
    * Merge JSON array.
    *
    * @param mergeJSONArr the merge JSON arr
    * @param JSONArrs     the JSON arrs
    * @return the JSON array
    * @throws JSONException the JSON exception
    */
   public static JSONArray mergeJSONArray(JSONArray mergeJSONArr, JSONArray... JSONArrs) throws JSONException {
      for (JSONArray jsonArr : JSONArrs) {
         for (int i = 0; i < jsonArr.length(); i++) {
            mergeJSONArr.put(jsonArr.get(i));
         }
      }
      return mergeJSONArr;
   }

   /**
    * Write pretty json.
    *
    * @param filePath   the file path
    * @param jsonObject the json object
    */
   public static void writePrettyJson(String filePath, JSONObject jsonObject) {
      File file = new File(filePath);
      file.getParentFile().mkdirs();
      try (FileOutputStream out = new FileOutputStream(file)) {
         try {
            IOUtils.write(toPrettyString(jsonObject), out, Charset.defaultCharset().name());
         } catch (JSONException e) {
            LOGGER.error(EXCEPTION, e);
         }
      } catch (FileNotFoundException e) {
         LOGGER.error(EXCEPTION, e);
      } catch (IOException e) {
         LOGGER.error(EXCEPTION, e);
      }
   }

   /**
    * Adds the extra details.
    *
    * @param dataObj                 the data obj
    * @param addExtraValueDetailsArr the add extra value details arr
    * @return the JSON object
    * @throws JSONException the JSON exception
    */
   public static JSONObject addExtraDetails(JSONObject dataObj, JSONArray addExtraValueDetailsArr) throws JSONException {
      //reference : [{ "key" : "code","value" : "supplierCompanyWiseSum"},{ "key" : "itemType","value" : "?","refKey":"code"}]
      for (int i = 0; i < addExtraValueDetailsArr.length(); i++) {
         boolean isValueExist = true;
         JSONObject detailObj = addExtraValueDetailsArr.getJSONObject(i);
         String key = detailObj.getString("key");
         Object value = detailObj.get("value");
         if ("?".equals(value.toString())) {
            if (detailObj.optBoolean("mergeKeys", false)) {
               value = "";
               String[] refKeys = detailObj.getString("refKey").split(",");
               for (int j = 0; j < refKeys.length; j++) {
                  if (isValueExist && dataObj.has(refKeys[j])) {
                     value += dataObj.get(refKeys[j]).toString();
                  } else {
                     isValueExist = false;
                  }
               }
            } else {
               if (dataObj.has(detailObj.getString("refKey"))) {
                  value = dataObj.get(detailObj.getString("refKey"));
               } else {
                  isValueExist = false;
               }
            }
         }
         if (isValueExist) {
            dataObj.put(key, value);
         }
      }
      return dataObj;
   }


   /**
    * Generate JSON details.
    *
    * @param dataObj            the data obj
    * @param keyValueDetailsArr the key value details arr
    * @return the JSON object
    * @throws JSONException the JSON exception
    */
   public static JSONObject generateJSONDetails(JSONObject dataObj, JSONArray keyValueDetailsArr) throws JSONException {
      JSONObject finalObj = new JSONObject();
      for (int i = 0; i < keyValueDetailsArr.length(); i++) {
         Object detail = keyValueDetailsArr.get(i);
         if (detail instanceof String) {
            if (dataObj.has(detail.toString())) {
               finalObj.put(detail.toString(), dataObj.get(detail.toString()));
            }
         } else {
            JSONObject detailObj = (JSONObject) detail;
            String key = detailObj.getString("key");
            String value = detailObj.getString("value");
            if (dataObj.has(key)) {
               finalObj.put(key, dataObj.get(value));
            }
         }
      }
      return finalObj;
   }

   /**
    * Merge JSON object values to JSON array.
    *
    * @param mainJsonArr the main json arr
    * @param jsonObjects the json objects
    * @return the JSON array
    * @throws JSONException the JSON exception
    */
   public static JSONArray mergeJSONObjectValuesToJSONArray(JSONArray mainJsonArr, JSONObject... jsonObjects) throws JSONException {
      JSONArray fnlArr = new JSONArray();
      for (int i = 0; i < jsonObjects.length; i++) {
         JSONObject jsonObj = jsonObjects[i];
         String[] keys = jsonObj.getNames(jsonObj);
         for (int j = 0; j < mainJsonArr.length(); j++) {
            JSONObject arrObj = mainJsonArr.getJSONObject(j);
            for (String key : keys) {
               arrObj.put(key, jsonObj.get(key));
            }
            fnlArr.put(arrObj);
         }
      }
      return fnlArr;
   }

   /**
    * Removes the unwanted keys.
    *
    * @param jsonObj    the json obj
    * @param removekeys the removekeys
    * @return the JSON object
    * @throws JSONException the JSON exception
    */
   public static JSONObject removeUnwantedKeys(JSONObject jsonObj, JSONArray removekeys) throws JSONException {

      for (int i = 0; i < removekeys.length(); i++) {
         String key = removekeys.getString(i);
         jsonObj.remove(key);
      }
      return jsonObj;

   }

   /**
    * JSON object to JSON array.
    *
    * @param dataObj the data obj
    * @return the JSON array
    */
   public static JSONArray JSONObjectToJSONArray(JSONObject dataObj) {
      JSONArray finalArray = new JSONArray();
      String keys[] = JSONObject.getNames(dataObj);
      for (String string : keys) {
         finalArray.put(dataObj.opt(string));
      }
      return finalArray;
   }

   /**
    * Convert to JSO narry.
    *
    * @param data    the data
    * @param splitBy the split by
    * @return the JSON array
    */
   public static JSONArray convertToJSONarry(Object data, String splitBy) {
      JSONArray jsonArray = new JSONArray();
      if (data instanceof String) {
         if (splitBy == null) {
            splitBy = ",";
         }
         String[] dataArr = ((String) data).split(Pattern.quote(splitBy));
         for (String string : dataArr) {
            jsonArray.put(string);
         }
      } else if (data instanceof JSONArray) {
         jsonArray = (JSONArray) data;
      } else {
         jsonArray.put(data);
      }
      return jsonArray;
   }

   /**
    * Parses the json from file.
    *
    * @param fileName the file name
    * @return the JSON object
    * @throws IOException   Signals that an I/O exception has occurred.
    * @throws JSONException the JSON exception
    */
   public static JSONObject parseJsonFromFile(String fileName) throws IOException, JSONException {
      File file = new File(fileName);
      return parseJsonFromFile(file);
   }

   /**
    * Parses the json from file.
    *
    * @param file the file
    * @return the JSON object
    * @throws IOException   Signals that an I/O exception has occurred.
    * @throws JSONException the JSON exception
    */
   public static JSONObject parseJsonFromFile(File file) throws IOException, JSONException {
      String content = readFile(new FileInputStream(file));
      return new JSONObject(content);
   }

   /**
    * Safely read.
    *
    * @param file the file
    * @return the JSON object
    */
   public static JSONObject safelyRead(File file) {
      try {
         return parseJsonFromFile(file);
      } catch (Exception e) {
         LOGGER.error(e.getMessage(), e);
      }
      return null;
   }

   /**
    * Read file.
    *
    * @param inputStream the input stream
    * @return the string
    * @throws IOException Signals that an I/O exception has occurred.
    */
   public static String readFile(InputStream inputStream) throws IOException {
      try {
         return IOUtils.toString(inputStream, "UTF-8");
      } finally {
         IOUtils.closeQuietly(inputStream);
      }
   }

   /**
    * Parses the json from stream.
    *
    * @param stream the stream
    * @return the JSON object
    * @throws IOException   Signals that an I/O exception has occurred.
    * @throws JSONException the JSON exception
    */
   public static JSONObject parseJsonFromStream(InputStream stream) throws IOException, JSONException {
      String content = readFile(stream);
      return new JSONObject(content);
   }

   /**
    * Parses the json object or array.
    *
    * @param stream the stream
    * @return the object
    * @throws IOException   Signals that an I/O exception has occurred.
    * @throws JSONException the JSON exception
    */
   public static Object parseJsonObjectOrArray(InputStream stream) throws IOException, JSONException {
      String content = readFile(stream).trim();
      if (content.startsWith("{")) {
         return new JSONObject(content);
      }
      if (content.startsWith("[")) {
         return new JSONArray(content);
      }
      throw new JSONException("Extracted String" + content + " is not a valid json structure");
   }


   /**
    * To pretty string.
    *
    * @param jsonObject the json object
    * @return the string
    * @throws JSONException the JSON exception
    */
   public static String toPrettyString(JSONObject jsonObject) throws JSONException {
      final int spacesToIndentEachLevel = 4;
      return jsonObject.toString(spacesToIndentEachLevel);
   }

   /**
    * Pretty json.
    *
    * @param json the json
    * @return the string
    */
   public static String prettyJson(JSONObject json) {
      try {
         return json == null ? null : json.toString(4);
      } catch (JSONException e) {
         return null;
      }
   }


   /**
    * Checks if is empty.
    *
    * @param jsonObject the json object
    * @return true, if is empty
    */
   public static boolean isEmpty(JSONObject jsonObject) {
      return (jsonObject == null) || (jsonObject.length() < 1);
   }

   /**
    * Checks if is empty.
    *
    * @param jsonArray the json array
    * @return true, if is empty
    */
   public static boolean isEmpty(JSONArray jsonArray) {
      return (jsonArray == null) || (jsonArray.length() < 1);
   }

   /**
    * Copy contents.
    *
    * @param source      the source
    * @param destination the destination
    * @throws JSONException the JSON exception
    */
   public static void copyContents(JSONObject source, JSONObject destination) throws JSONException {
      if (isEmpty(source)) {
         return;
      }
      for (String key : JSONObject.getNames(source)) {
         destination.put(key, source.get(key));
      }
   }


   /**
    * To array.
    *
    * @param jsonArray the json array
    * @return the object[]
    */
   public static Object[] toArray(JSONArray jsonArray) {
      Object[] array = new Object[jsonArray.length()];
      for (int i = 0; i < jsonArray.length(); i++) {
         try {
            array[i] = jsonArray.get(i);
         } catch (JSONException e) {
            LOGGER.error(e.getMessage(), e);
         }
      }
      return array;
   }

   /**
    * Checks if is equal.
    *
    * @param array     the array
    * @param jsonArray the json array
    * @return true, if is equal
    * @throws JSONException the JSON exception
    */
   public static boolean isEqual(String[] array, JSONArray jsonArray) throws JSONException {
      if (array == null || jsonArray == null) {
         return true;
      }
      List list = toList(jsonArray);
      List arrayList = Arrays.asList(array);
      return list.containsAll(arrayList) && arrayList.containsAll(list);
   }

   /**
    * Put value safetly.
    *
    * @param data  the data
    * @param key   the key
    * @param value the value
    */
   public static void putValueSafetly(JSONObject data, String key, Object value) {
      try {
         data.put(key, value);
      } catch (JSONException e) {
         LOGGER.error(e.getMessage());
      }
   }

   /**
    * Safe to string.
    *
    * @param data the data
    * @return the string
    */
   public static String safeToString(JSONObject data) {
      if (data == null) {
         return new JSONObject().toString();
      }
      return data.toString();
   }

   public static String toSafeJsonString(Object data) {
      try {
         return toJsonString(data);
      } catch (Exception e) {
         return "{}";
      }
   }

   /**
    * Contains.
    *
    * @param array the array
    * @param data  the data
    * @return true, if successful
    */
   public static boolean contains(JSONArray array, Object data) {
      for (int i = 0; i < array.length(); i++) {
         if (array.opt(i).equals(data)) {
            return true;
         }
      }
      return false;
   }

   /**
    * Checks if is json object.
    *
    * @param item the item
    * @return true, if is json object
    */
   public static boolean isJsonObject(Object item) {
      return item instanceof JSONObject;
   }

   /**
    * Checks if is json array.
    *
    * @param item the item
    * @return true, if is json array
    */
   public static boolean isJsonArray(Object item) {
      return item instanceof JSONArray;
   }

   /**
    * Extract keys.
    *
    * @param object the object
    * @return the JSON array
    */
   public static JSONArray extractKeys(JSONObject object) {
      JSONArray keys = new JSONArray();
      if (!isEmpty(object)) {
         for (String key : JSONObject.getNames(object)) {
            keys.put(key);
         }
      }
      return keys;
   }

   /**
    * Extract keys and make SPA array.
    *
    * @param data    the data
    * @param keyName the key name
    * @return the JSON array
    */
   public static JSONArray extractKeysAndMakeSPAArray(JSONObject data, String keyName) {
      JSONArray list = new JSONArray();
      if (!isEmpty(data)) {
         for (String key : JSONObject.getNames(data)) {
            JSONObject item = new JSONObject();
            list.put(item);
            try {
               item.put(keyName, key);
            } catch (JSONException e) {
               LOGGER.error(e.getMessage());
            }
         }
      }
      return list;
   }

   private static void checkInstanceAndCopy(JSONObject source, JSONObject destination, String key){
      Object value = source.opt(key);
      if (value instanceof String && !StringUtils.isEmpty((String) value)) {
         destination.put(key, value);
      }
      if (value instanceof JSONObject && (!isEmpty((JSONObject) value))) {
         destination.put(key, value);
      }
      if (value instanceof JSONArray && (!isEmpty((JSONArray) value))) {
         destination.put(key, value);
      }
      if (value instanceof Number) {
         destination.put(key, value);
      }
      if (value instanceof Boolean && (boolean) value) {
         destination.put(key, value);
      }
   }
   /**
    * Copy value if present.
    *
    * @param source      the source
    * @param destination the destination
    * @param key         the key
    */
   public static void copyValueIfPresent(JSONObject source, JSONObject destination, String key) {
      if (source.has(key)) {
         try {
            checkInstanceAndCopy(source, destination, key);
         } catch (JSONException e) {
            LOGGER.error(e.getMessage());
         }
      }
   }

   /**
    * Copy inner values if present.
    *
    * @param source      the source
    * @param destination the destination
    */
   public static void copyInnerValuesIfPresent(JSONObject source, JSONObject destination) {
      if (!isEmpty(source)) {
         for (String key : JSONObject.getNames(source)) {
            copyValueIfPresent(source, destination, key);
         }
      }
   }


   /**
    * Clone JSON object.
    *
    * @param source the source
    * @return the JSON object
    */
   public static JSONObject cloneJSONObject(JSONObject source) {
      try {
         return new JSONObject(source.toString());
      } catch (JSONException e) {
         LOGGER.error(e.getMessage());
      }
      return null;
   }

   /**
    * Extract value.
    *
    * @param source     the source
    * @param properties the properties
    * @return the object
    */
   public static Object extractValue(JSONObject source, List<String> properties) {
      if (properties.isEmpty()) {
         return source;
      }
      String property = properties.get(0);
      if (source.has(property)) {
         JSONObject data = source.optJSONObject(property);
         if (data != null) {
            properties.remove(0);
            return extractValue(data, properties);
         }
      }
      return null;
   }


   /**
    * Deep merge.
    *
    * @param source the source
    * @param target the target
    * @return the JSON object
    * @throws JSONException the JSON exception
    */
   public static JSONObject deepMerge(JSONObject source, JSONObject target) throws JSONException {
      if (containsData(source)) {
         for (String key : JSONObject.getNames(source)) {
            Object value = source.get(key);
            if (!target.has(key)) {
               // new value for "key":
               target.put(key, value);
            } else {
               // existing value for "key" - recursively deep merge:
               if (value instanceof JSONObject) {
                  JSONObject valueJson = (JSONObject) value;

                  deepMerge(valueJson, target.getJSONObject(key));
               } else {
                  target.put(key, value);
               }
            }
         }
      }
      return target;
   }

   /**
    * Array to JSON array.
    *
    * @param arr the arr
    * @return the JSON array
    */
   public static JSONArray arrayToJSONArray(Object[] arr) {
      JSONArray jsonArr = new JSONArray();
      for (int i = 0; i < arr.length; i++) {
         jsonArr.put(arr[i]);
      }
      return jsonArr;
   }


   /**
    * Gets the first key.
    *
    * @param jsonObject the json object
    * @return the first key
    */
   public static String getFirstKey(JSONObject jsonObject) {
      if (!isEmpty(jsonObject)) {
         return JSONObject.getNames(jsonObject)[0];
      }
      return null;
   }


   /**
    * Gets the first value.
    *
    * @param jsonObject the json object
    * @return the first value
    */
   public static Object getFirstValue(JSONObject jsonObject) {
      if (!isEmpty(jsonObject)) {
         for (String key : JSONObject.getNames(jsonObject)) {
            return jsonObject.opt(key);
         }
      }
      return null;
   }

   /**
    * Gets the first value.
    *
    * @param array the array
    * @return the first value
    */
   public static Object getFirstValue(JSONArray array) {
      if (containsData(array)) {
         return array.opt(0);
      }
      return null;
   }


   /**
    * Checks if is valid JSON object.
    *
    * @param value the value
    * @return true, if is valid JSON object
    */
   public static boolean isValidJSONObject(String value) {
      try {
         new JSONObject(value);
         return true;
      } catch (JSONException e) {
         return false;
      }
   }

   /**
    * Checks if is valid JSON array.
    *
    * @param value the value
    * @return true, if is valid JSON array
    */
   public static boolean isValidJSONArray(String value) {
      try {
         new JSONArray(value);
         return true;
      } catch (JSONException e) {
         return false;
      }

   }

   /**
    * Contains data.
    *
    * @param jsonObject the json object
    * @return true, if successful
    */
   public static boolean containsData(JSONObject jsonObject) {
      return !isEmpty(jsonObject);
   }


   /**
    * Gets the value from key.
    *
    * @param data the data
    * @param key  the key
    * @return the value from key
    */
   public static Object getValueFromKey(JSONObject data, String key) {
      final String delimiter = "\\.";
      return getValueFromKey(delimiter, data, key);
   }

   /**
    * Gets the value from key.
    *
    * @param delimiter the delimiter
    * @param data      the data
    * @param key       the key
    * @return the value from key
    */
   public static Object getValueFromKey(String delimiter, JSONObject data, String key) {
      try {
         String[] keyArray = key.split(delimiter);
         List<String> keys = CollectionUtils.toList(keyArray);
         return extractValueFromKey(data, keys);
      } catch (Exception e) {
         LOGGER.warn("{} key is not found from {}", key, data);
         return null;
      }
   }

   /**
    * Gets the value from key.
    *
    * @param delimiter the delimiter
    * @param data      the data
    * @param keys      the keys
    * @return the value from key
    */
   public static Object getValueFromKey(char delimiter, JSONObject data, String... keys) {
      String mergedKey = org.apache.commons.lang.StringUtils.join(keys, delimiter);
      return getValueFromKey(String.valueOf(delimiter), data, mergedKey);
   }


   /**
    * Gets the value from key.
    *
    * @param data the data
    * @param keys the keys
    * @return the value from key
    */
   public static Object getValueFromKey(JSONObject data, String... keys) {
      StringBuilder mergedKey = new StringBuilder();
      for (String key : keys) {
         mergedKey.append(key).append(".");
      }
      mergedKey = new StringBuilder(mergedKey.substring(0, mergedKey.length() - 1));
      return getValueFromKey(data, mergedKey.toString());
   }

   /**
    * Extract value from key.
    *
    * @param data the data
    * @param keys the keys
    * @return the object
    * @throws JSONException the JSON exception
    */
   private static Object extractValueFromKey(JSONObject data, List<String> keys) throws JSONException {
      String key = keys.remove(0);
      Object value = data.get(key);
      if (value instanceof JSONObject && !keys.isEmpty()) {
         return extractValueFromKey((JSONObject) value, keys);
      } else {
         return value;
      }
   }


   /**
    * Contains data.
    *
    * @param array the array
    * @return true, if successful
    */
   public static boolean containsData(JSONArray array) {
      return !isEmpty(array);
   }


   /**
    * Replace dynamic values.
    *
    * @param template   the template
    * @param dataObject the data object
    * @throws JSONException the JSON exception
    */
   public static void replaceDynamicValues(JSONObject template, JSONObject dataObject) throws JSONException {
      if (containsData(template)) {
         for (String key : JSONObject.getNames(template)) {
            Object value = template.opt(key);
            if (value instanceof String) {
               template.put(key, getReplacedValue((String) value, dataObject));
            }
            if (value instanceof JSONObject) {
               replaceDynamicValues((JSONObject) value, dataObject);
            }
         }
      }
   }

   /**
    * Gets the replaced value.
    *
    * @param value      the value
    * @param dataObject the data object
    * @return the replaced value
    * @throws JSONException the JSON exception
    */
   private static Object getReplacedValue(String value, JSONObject dataObject) throws JSONException {
      if (value.startsWith("{{") && value.endsWith("}}")) {
         String key = StringUtils.getCurlyBracketsMatcherKey(value);
         return dataObject.get(key);
      } else {
         return StringUtils.replaceDynamicValues(value, dataObject);
      }
   }


   /**
    * Shallow merge.
    *
    * @param source      the source
    * @param destination the destination
    * @return the JSON object
    * @throws JSONException the JSON exception
    */
   public static JSONObject shallowMerge(JSONObject source, JSONObject destination) throws JSONException {
      if (containsData(source)) {
         for (String key : JSONObject.getNames(source)) {
            putValueSafetly(destination, key, source.opt(key));
         }
      }
      return destination;
   }

   /**
    * Safe shallow merge.
    *
    * @param source      the source
    * @param destination the destination
    * @return the JSON object
    */
   public static JSONObject safeShallowMerge(JSONObject source, JSONObject destination) {
      try {
         shallowMerge(source, destination);
      } catch (JSONException e) {
         LOGGER.error(e.getMessage());
      }
      return destination;
   }


   /**
    * Are equal.
    *
    * @param ob1 the ob 1
    * @param ob2 the ob 2
    * @return true, if successful
    * @throws JSONException the JSON exception
    */
   public static boolean areEqual(Object ob1, Object ob2) throws JSONException {
      Object obj1Converted = convertJsonElement(ob1);
      Object obj2Converted = convertJsonElement(ob2);
      return obj1Converted.equals(obj2Converted);
   }

   /**
    * Convert json element.
    *
    * @param elem the elem
    * @return the object
    * @throws JSONException the JSON exception
    */
   private static Object convertJsonElement(Object elem) throws JSONException {
      if (elem instanceof JSONObject) {
         JSONObject obj = (JSONObject) elem;
         Iterator<String> keys = obj.keys();
         Map<String, Object> jsonMap = new HashMap<>();
         while (keys.hasNext()) {
            String key = keys.next();
            jsonMap.put(key, convertJsonElement(obj.get(key)));
         }
         return jsonMap;
      } else if (elem instanceof JSONArray) {
         JSONArray arr = (JSONArray) elem;
         Set<Object> jsonSet = new HashSet<>();
         for (int i = 0; i < arr.length(); i++) {
            jsonSet.add(convertJsonElement(arr.get(i)));
         }
         return jsonSet;
      } else {
         return elem;
      }
   }


   /**
    * Checks for keys.
    *
    * @param jsonObject the json object
    * @param keys       the keys
    * @return true, if successful
    */
   public static boolean hasKeys(JSONObject jsonObject, String... keys) {
      for (String key : keys) {
         if (!jsonObject.has(key)) {
            return false;
         }
      }
      return true;
   }


   /**
    * Gets the empty object.
    *
    * @return the empty object
    */
   public static JSONObject getEmptyObject() {
      return new JSONObject();
   }

   /**
    * Json to map.
    *
    * @param json the json
    * @return the map
    * @throws JSONException the JSON exception
    */
   public static Map<String, Object> jsonToMap(JSONObject json) throws JSONException {
      Map<String, Object> retMap = new HashMap<String, Object>();

      if (json != JSONObject.NULL) {
         retMap = toMap(json);
      }
      return retMap;
   }

   /**
    * To map.
    *
    * @param object the object
    * @return the map
    * @throws JSONException the JSON exception
    */
   public static Map<String, Object> toMap(JSONObject object) throws JSONException {
      Map<String, Object> map = new LinkedHashMap<>();

      Iterator<String> keysItr = object.keys();
      while (keysItr.hasNext()) {
         String key = keysItr.next();
         Object value = object.get(key);

         if (value instanceof JSONArray) {
            value = toJsonToArrayList((JSONArray) value);
         } else if (value instanceof JSONObject) {
            value = toMap((JSONObject) value);
         }
         map.put(key, value);
      }
      return map;
   }

   public static Map<String, String> toStringMap(JsonNode jsonNode) {
      return OBJECT_MAPPER.convertValue(jsonNode, STRING_VALUE_MAP_REFERENCE);
   }

   public static Map<String, Object> toObjectMap(JsonNode jsonNode) {
      return OBJECT_MAPPER.convertValue(jsonNode, OBJECT_VALUE_MAP_REFERENCE);
   }

   public static Map<String, Object> toMap(Object input) {
      return getObjectMapper().convertValue(input, OBJECT_VALUE_MAP_REFERENCE);
   }

   /**
    * To map generic.
    *
    * @param json the json
    * @return the map
    * @throws JSONException the JSON exception
    */
   public static Map<String, ?> toMapGeneric(JSONObject json) throws JSONException {
      Map<String, ?> retMap = new HashMap<>();

      if (json != JSONObject.NULL) {
         retMap = toMap(json);
      }
      return retMap;
   }

   /**
    * To json to array list.
    *
    * @param array the array
    * @return the list
    * @throws JSONException the JSON exception
    */
   public static List<Object> toJsonToArrayList(JSONArray array) throws JSONException {
      List<Object> list = new ArrayList<Object>();
      for (int i = 0; i < array.length(); i++) {
         Object value = array.get(i);
         if (value instanceof JSONArray) {
            value = toJsonToArrayList((JSONArray) value);
         } else if (value instanceof JSONObject) {
            value = toMap((JSONObject) value);
         }
         list.add(value);
      }
      return list;
   }


   /**
    * Safe clone.
    *
    * @param itemToClone the item to clone
    * @return the JSON object
    */
   public static JSONObject safeClone(JSONObject itemToClone) {
      if (itemToClone != null) {
         try {
            return new JSONObject(itemToClone.toString());
         } catch (JSONException e) {
            //this will eat this exception
            //We don't want to print this exception
         }
      }
      return null;
   }

   /**
    * Clone.
    *
    * @param array the array
    * @return the JSON array
    */
   public static JSONArray clone(JSONArray array) {
      try {
         return new JSONArray(array.toString());
      } catch (JSONException e) {
         LOGGER.error(e.getMessage(), e);
      }
      return null;
   }

   /**
    * Checks if is json file.
    *
    * @param file the file
    * @return true, if is json file
    */
   public static boolean isJsonFile(File file) {
      return file != null && file.exists() && "json".equalsIgnoreCase(FilenameUtils.getExtension(file.toString()));
   }

   /**
    * Checks if is eligible for put.
    *
    * @param item the item
    * @return true, if is eligible for put
    */
   public static boolean isEligibleForPut(Object item) {
      Class[] eligibleClasses = {
              JSONObject.class, JSONArray.class, String.class, Integer.class, Double.class
      };
      for (Class clazz : eligibleClasses) {
         if (clazz.isInstance(item)) {
            return true;
         }
      }
      return false;
   }


   /**
    * Checks if is array string.
    *
    * @param data the data
    * @return true, if is array string
    */
   public static boolean isArrayString(String data) {
      try {
         new JSONArray(data);
         return true;
      } catch (JSONException e) {
         return false;
      }
   }

   /**
    * Creates the array saftly.
    *
    * @param data the data
    * @return the JSON array
    */
   public static JSONArray createArraySaftly(String data) {
      try {
         return new JSONArray(data);
      } catch (JSONException e) {
         return null;
      }
   }

   /**
    * Creates the object saftly.
    *
    * @param data the data
    * @return the JSON object
    */
   public static JSONObject createObjectSaftly(String data) {
      try {
         return new JSONObject(data);
      } catch (JSONException e) {
         return null;
      }
   }

   /**
    * Join array as string.
    *
    * @param array     the array
    * @param separator the separator
    * @return the string
    */
   public static String joinArrayAsString(JSONArray array, String separator) {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < array.length(); i++) {
         builder.append(array.opt(i));
         builder.append(separator);
      }
      if (!separator.isEmpty()) {
         builder.deleteCharAt(separator.length() - 1);
      }
      return builder.toString();
   }


   /**
    * Checks if is keys value empty or null.
    *
    * @param key     the key
    * @param dataObj the data obj
    * @return true, if is keys value empty or null
    * @throws JSONException the JSON exception
    */
   public static boolean isKeysValueEmptyOrNull(String key, JSONObject dataObj) throws JSONException {
      boolean result = true;
      if (dataObj.has(key) && !dataObj.get(key).equals("")) {
         result = false;
      }
      return result;

   }

   /**
    * Reverse JSON array.
    *
    * @param jsonArr the json arr
    * @return the JSON array
    * @throws JSONException the JSON exception
    */
   public static JSONArray reverseJSONArray(JSONArray jsonArr) throws JSONException {
      JSONArray reversedJsonArr = new JSONArray();
      for (int i = jsonArr.length() - 1; i >= 0; i--) {
         reversedJsonArr.put(jsonArr.get(i));
      }
      return reversedJsonArr;
   }

   /**
    * Cast to JSON object.
    *
    * @param data the data
    * @return the JSON object
    * @throws JSONException the JSON exception
    */
   public static JSONObject castToJSONObject(Object data) throws JSONException {
      if (data != null) {
         return (JSONObject) data;
      } else {
         throw new JSONException("<<<<<<<<<< Data should not be null >>>>>>>>>>>>");
      }
   }

   /**
    * Cast to JSON array.
    *
    * @param data the data
    * @return the JSON array
    * @throws JSONException the JSON exception
    */
   public static JSONArray castToJSONArray(Object data) throws JSONException {
      if (data != null) {
         return (JSONArray) data;
      } else {
         throw new JSONException("<<<<<<<<<< Data should not be null >>>>>>>>>>>>");
      }
   }

   /**
    * Copy keys.
    *
    * @param <T>        the generic type
    * @param dataObject the data object
    * @param collection the collection
    * @return the t
    */
   public static <T extends Collection> T copyKeys(JSONObject dataObject, T collection) {
      if (containsData(dataObject)) {
         collection.addAll(Arrays.asList(JSONObject.getNames(dataObject)));
      }
      return collection;
   }

   /**
    * Gets the object by specified path.
    *
    * @param source       the source
    * @param keyDepth     the key depth
    * @param defaultValue the default value
    * @return the object by specified path
    */
   public static Object getObjectBySpecifiedPath(JSONObject source, String keyDepth, Object defaultValue) {
      Object result = defaultValue;
      if (keyDepth != null) {
         try {
            String[] keyDepthArr = keyDepth.split("/");
            for (int i = 0, len = keyDepthArr.length; i < len; i++) {
               result = source.get(keyDepthArr[i]);
            }
            return result;
         } catch (JSONException e) {
            LOGGER.error(EXCEPTION, e);
         }
      }
      return result;
   }

   /**
    * To properties.
    *
    * @param from       the from
    * @param properties the properties
    * @return the properties
    */
   public static Properties toProperties(JSONObject from, Properties properties) {
      if (containsData(from)) {
         for (String key : JSONObject.getNames(from)) {
            properties.setProperty(key, from.optString(key));
         }
      }
      return properties;
   }

   /**
    * Put to collection.
    *
    * @param <T>        the generic type
    * @param array      the array
    * @param collection the collection
    */
   public static <T> void putToCollection(JSONArray array, Collection<T> collection) {
      if (containsData(array)) {
         for (int i = 0; i < array.length(); i++) {
            collection.add((T) array.opt(i));
         }
      }
   }

   /**
    * Removes the duplicate jsons from json array.
    *
    * @param array the array
    * @param key   the key
    * @return the JSON array
    * @throws JSONException the JSON exception
    */
   public static JSONArray removeDuplicateJsonsFromJsonArray(JSONArray array, String key) throws JSONException {
      for (int i = 0; i < array.length(); i++) {
         JSONObject elem = array.getJSONObject(i);
         for (int k = i + 1; k < array.length(); k++) {
            while (k < array.length() && elem.optString(key).equals(array.getJSONObject(k).optString(key))) {
               array.remove(k);
            }
         }
      }
      return array;
   }


   /**
    * Removes the value if specific val contains.
    *
    * @param jsonArr       the json arr
    * @param compareString the compare string
    * @return the JSON array
    * @throws JSONException the JSON exception
    */
   public static JSONArray removeValueIfSpecificValContains(JSONArray jsonArr, String compareString) throws JSONException {
      JSONArray newArr = new JSONArray();
      for (int i = 0; i < jsonArr.length(); i++) {
         String curVal = jsonArr.getString(i);
         if (!curVal.contains(compareString)) {
            newArr.put(curVal);
         }
      }

      return newArr;
   }

   /**
    * To set.
    *
    * @param <T>     the generic type
    * @param jsonArr the json arr
    * @return the sets the
    * @throws JSONException the JSON exception
    */
   public static <T> Set<T> toSet(JSONArray jsonArr) throws JSONException {
      List<T> arrList = toList(jsonArr);
      return new HashSet<>(arrList);
   }

   public static JsonNode toJsonNode(Map<?, ?> input) {
      return OBJECT_MAPPER.convertValue(input, JsonNode.class);
   }

   public static JsonNode toJsonNode(Object item) {
      return get().convertValue(item, JsonNode.class);
   }

   public static <T> T convert(JsonNode node, Class<T> clazz) {
      return OBJECT_MAPPER.convertValue(node, clazz);
   }

   public static <T> T convert(Object node, Class<T> tClass) {
      return OBJECT_MAPPER.convertValue(node, tClass);
   }

   public static <T> T convert(Object node, TypeReference<T> typeReference) {
      return OBJECT_MAPPER.convertValue(node, typeReference);
   }

   public static <T> T parse(String data, TypeReference<T> typeReference) {
      try {
         return OBJECT_MAPPER.readValue(data, typeReference);
      } catch (JsonProcessingException e) {
         throw new ConversionException(e);
      }
   }
   
   /**
    * Convert json string to given model.
    *
    * @param <T> the generic type
    * @param body the body
    * @param clazz the clazz
    * @return the t
    */
   public static <T> T convert(String body, Class<T> clazz) {
	   try {
		   return JSONUtils.getObjectMapper().readValue(body, clazz);
	   } catch (JsonProcessingException e) {
//		   throw new TransformationException(e,"Exception happened while converting string to given class body : {}, type : {}", body, String.valueOf(clazz));
		   throw new CustomRuntimeException(e,"Exception happened while converting string to given class body : {}, type : {}", body, String.valueOf(clazz));
	   }
   }

   public static Collector<JsonNode, ArrayNode, ArrayNode> toArrayNode() {
      return new ArrayNodeCollector();
   }

   public static Stream<JsonNode> stream(JsonNode nodes) {
      return StreamSupport.stream(nodes.spliterator(), false);
   }

   private static class ArrayNodeCollector implements Collector<JsonNode, ArrayNode, ArrayNode> {

      @Override
      public Supplier<ArrayNode> supplier() {
         return OBJECT_MAPPER::createArrayNode;
      }

      @Override
      public BiConsumer<ArrayNode, JsonNode> accumulator() {
         return ArrayNode::add;
      }

      @Override
      public BinaryOperator<ArrayNode> combiner() {
         return (x, y) -> {
            x.addAll(y);
            return x;
         };
      }

      @Override
      public Function<ArrayNode, ArrayNode> finisher() {
         return accumulator -> accumulator;
      }

      @Override
      public Set<Characteristics> characteristics() {
         return EnumSet.of(Characteristics.UNORDERED);
      }
   }

   public static boolean containsData(ArrayNode array) {
      return !isEmptyOrNull(array);
   }

   public static boolean isEmptyOrNull(ArrayNode array) {
      return array == null || array.isNull() || array.isEmpty();
   }



   public static String toJsonString(Collection<?> collection) {
      try {
         return OBJECT_MAPPER.writeValueAsString(collection);
      } catch (JsonProcessingException e) {
         throw new ConversionException("Could not convert collection to json. input = " + collection, e);
      }
   }

   public static String toJsonString(Object object) {
      try {
         return OBJECT_MAPPER.writeValueAsString(object);
      } catch (JsonProcessingException e) {
         throw new ConversionException("Could not convert collection to json. input= " + object, e);
      }
   }

   public static JsonNode parse(String data) {
      try {
         return OBJECT_MAPPER.readTree(data);
      } catch (JsonProcessingException e) {
         throw new ConversionException("Could not convert to json node. input" + data, e);
      }
   }

   public static <T> T parse(String data, Class<T> tClass) {
      try {
         return OBJECT_MAPPER.readValue(data, tClass);
      } catch (JsonProcessingException e) {
         throw new ConversionException("Could not parse data to " + tClass + " input:" + data);
      }
   }

   public static <T>List<T> toList(String input, Class<T> type) {
	   try {
		   CollectionType typeReference =
				   TypeFactory.defaultInstance().constructCollectionType(List.class, type);
		   return OBJECT_MAPPER.readValue(input, typeReference);
	   } catch (JsonProcessingException e) {
		   throw new ConversionException("Could not convert to list of type " + type + ". input=" + input, e);
	   }
   }


   public static List<String> toList(ArrayNode node) {
      return OBJECT_MAPPER.convertValue(node, LIST_STRING_REFERENCE);
   }

   public static String stringify(Object object){
      try {
         return getObjectMapper().writeValueAsString(object);
      } catch (JsonProcessingException e) {
         LOGGER.error("stacktrace", e);
      }
      return null;
   }

   public static boolean isNull(JsonNode node) {
      return node == null || node.isNull();
   }

   public static void assertNull(JsonNode node, Supplier<RuntimeException> exceptionSupplier) {
      if (isNull(node)) {
         throw exceptionSupplier.get();
      }
   }

   /**
    * this is for getting value from json node or default value
    * @param jsonNode object from which data need to get
    * @param key key for which data have to get
    * @param defaultValue default value
    * @return
    */
   public static String getStringData(JsonNode jsonNode, String key,String defaultValue){
      if(jsonNode!=null && !jsonNode.isEmpty())
         return jsonNode.has(key)?jsonNode.get(key).asText():defaultValue;
      return defaultValue;
   }

}