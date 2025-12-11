package com.applicate.services.channelkart.utils;

import java.util.Collection;
import java.util.Map;

public class CollectionUtils {

	public static boolean isEmptyOrNull(Collection collection) {
		return collection == null || collection.isEmpty();
	}

	public static boolean isEmptyOrNull(Map<?, ?> map) {
		return map == null || map.isEmpty();
	}

}
