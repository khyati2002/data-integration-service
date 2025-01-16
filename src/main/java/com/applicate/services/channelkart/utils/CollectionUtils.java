/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.applicate.services.channelkart.utils;


import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.stream.StreamSupport;

/**
 * The class CollectionUtils.
 *
 * @author Manish Srivastava
 * @since  May 2020
 */
public class CollectionUtils {

	/** The Constant logger. */
	private static final Logger logger = LoggerFactory.getLogger(CollectionUtils.class);

	/**
	 * Merge map.
	 *
	 * @param from the from
	 * @param to the to
	 */
	public static void mergeMap(Map from, Map to) {
		if(from == null)
			return;
		if(to == null)
			to = new HashMap();
		for(Object key : from.keySet()) {
			to.put(key, from.get(key));
		}
	}


	/**
	 * Null safe to list.
	 *
	 * @param array the array
	 * @return the list
	 * @throws JSONException the JSON exception
	 */
	public static List nullSafeToList(JSONArray array) throws JSONException {
		if(array == null)
			return null;
		List items = new ArrayList(array.length());
		for(int i = 0; i < array.length(); i++) {
			items.add(array.get(i));
		}
		return items;
	}


	/**
	 * Close.
	 *
	 * @param items the items
	 */
	public static void close(Closeable... items) {
		for(Closeable item : items) {
			if(item != null) {
				try {
					item.close();
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
			}
		}
	}


	/**
	 * Inter change key value.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param map the map
	 * @return the map
	 */
	public static <K, V> Map<V,K> interChangeKeyValue(Map<K,V> map) {
		Map<V,K> newMap = new LinkedHashMap<>();
		for(K key : map.keySet()) {
			V value = map.get(key);
			newMap.put(value, key);
		}
		return newMap;
	}


	/**
	 * Adds the items to list.
	 *
	 * @param jsonArray the json array
	 * @param list the list
	 * @throws JSONException the JSON exception
	 */
	public static void addItemsToList(JSONArray jsonArray, List list) throws JSONException {
		for(int i = 0; i < jsonArray.length(); i++) {
			list.add(jsonArray.get(i));
		}
	}



	/**
	 * Map key to list.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param map the map
	 * @return the list
	 */
	public static <K,V> List<K> MapKeyToList(Map<K, V> map) {
		List<K> list = new ArrayList<>();
		for(K item : map.keySet()) {
			list.add(item);
		}
		return list;
	}


	/**
	 * Map value to list.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param map the map
	 * @return the list
	 */
	public static <K, V> List<V> MapValueToList(Map<K, V> map) {
		List<V> list = new ArrayList<>();
		for(K key : map.keySet()) {
			list.add(map.get(key));
		}
		return list;
	}


	/**
	 * Clone properties.
	 *
	 * @param source the source
	 * @return the properties
	 */
	public static Properties cloneProperties(Properties source) {
		Properties properties = new Properties();
		for (Map.Entry<?, ?> entry: source.entrySet()) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			properties.put(key, value);
		}
		return properties;
	}


	/**
	 * To list.
	 *
	 * @param <T> the generic type
	 * @param items the items
	 * @return the list
	 */
	public static <T>  List<T> toList(T[] items) {
		List<T> list = null;
		if(items != null) {
			list = new ArrayList<>();
			for (T value : items) {
				list.add(value);
			}
		}
		return list;
	}


	/**
	 * Clone array.
	 *
	 * @param <T> the generic type
	 * @param items the items
	 * @return the t[]
	 */
	public static <T> T[] cloneArray(T[] items) {
		return Arrays.copyOf(items, items.length);
	}


	/**
	 * Clone bytes.
	 *
	 * @param array the array
	 * @return the byte[]
	 */
	public static byte[] cloneBytes(byte[] array) {
		return Arrays.copyOf(array, array.length);
	}


	/**
	 * Checks if is equal ignore order.
	 *
	 * @param array1 the array 1
	 * @param array2 the array 2
	 * @return true, if is equal ignore order
	 */
	public static boolean isEqualIgnoreOrder(Object[] array1, Object[] array2) {
		array1 = Arrays.copyOf(array1, array1.length);
		array2 = Arrays.copyOf(array2, array2.length);
		Arrays.sort(array1);
		Arrays.sort(array2);
		return Arrays.equals(array1, array2);
	}


	/**
	 * Contains data.
	 *
	 * @param collection the collection
	 * @return true, if successful
	 */
	public static boolean containsData(Collection collection) {
		return collection != null && collection.size() > 0;
	}

	/**
	 * Contains data.
	 *
	 * @param map the map
	 * @return true, if successful
	 */
	public static boolean containsData(Map map) {
		return map != null && map.size() > 0;
	}

	/**
	 * Gets the first key.
	 *
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param map the map
	 * @return the first key
	 */
	public static  <K,V> K getFirstKey(Map<K, V> map) {
		for (K key : map.keySet()) {
			return key;
		}
		return null;
	}

	/**
	 * Checks if is empty or null.
	 *
	 * @param collection the collection
	 * @return true, if is empty or null
	 */
	public static boolean isEmptyOrNull(Collection collection) {
		return collection == null || collection.isEmpty();
	}

	public static boolean isEmptyOrNull(Map<?, ?> map) {
		return map == null || map.isEmpty();
	}

	public static boolean isNotEmpty(Collection<?> collection) {
		return !isEmptyOrNull(collection);
	}



	/**
	 * Removes the all.
	 *
	 * @param collection the collection
	 * @param objectToRemove the object to remove
	 */
	public static void removeAll(Collection collection, Object objectToRemove) {
		boolean hasRemoved = collection.remove(objectToRemove);
		while (hasRemoved) {
			hasRemoved = collection.remove(objectToRemove);
		}
	}

	/**
	 * Creates the collection.
	 *
	 * @param collectionClass the collection class
	 * @return the collection
	 * @throws IllegalAccessException the illegal access exception
	 * @throws InstantiationException the instantiation exception
	 */
	public static Collection createCollection(Class<? extends Collection> collectionClass) throws IllegalAccessException, InstantiationException {
		if (collectionClass == List.class) {
			return new ArrayList();
		}
		return collectionClass.newInstance();
	}

	/**
	 * Immutable list.
	 *
	 * @param <T> the generic type
	 * @param items the items
	 * @return the list
	 */
	public static <T> List<T> immutableList(T... items) {
		return Collections.unmodifiableList(Arrays.asList(items));
	}


	public static <T> Optional<T> findFirst(Iterable<T> items) {
		if (items != null) {
			return StreamSupport.stream(items.spliterator(), false).findFirst();
		}
		return Optional.empty();
	}


//	public static <T> Optional<T> getFromList(List<T> from, SellinaQueryAssociation object) {
//		int index = from.indexOf(object);
//		if (index < 0) {
//			return Optional.empty();
//		}
//		return Optional.ofNullable(from.get(index));
//	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <K, V> Map<K, V> deepMapOverride(Map<K, V> defaultMap, Map<K, V> mapToOverride) {
		Map<K, V> response = new HashMap<>(defaultMap);
		for (Map.Entry<K, V> entry : mapToOverride.entrySet()) {
			K key = entry.getKey();
			V value = entry.getValue();
			V existingValue = response.get(key);
			if (existingValue instanceof Map && value instanceof Map) {
				response.put(key, (V) deepMapOverride((Map)existingValue, (Map)value));
			} else {
				response.put(key, value);
			}
		}
		return response;
	}

	public static <T> Set<T> mergeSet(Set<T> set, T... itemsToMerge) {
		Set<T> output = new HashSet<>(set);
		Collections.addAll(output, itemsToMerge);
		return output;
	}

	public static <T> Optional<T> getValueByIgnoreKeyCase(Map<String, T> map, String key) {
		T t = map.get(key);
		if (t == null) {
			Optional<String> optionalKey = map.keySet().stream().filter(key::equalsIgnoreCase)
					.findFirst();
			return optionalKey.map(map::get);
		}
		return Optional.of(t);
	}

	public static Object getValue(Map<String, Object> map, String key, Object defaultValue) {
		Object value = map.get(key);
		return value == null ? defaultValue : value;
	}

	public static <T> List<T> mergeList(List<T> list1, List<T> list2) {
		List<T> items = new ArrayList<>();
		items.addAll(list1);
		items.addAll(list2);
		return items;
	}
}
