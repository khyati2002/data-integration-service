///*
// * Copyright (c) 2021. All rights reserved.
// * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
// *
// */
package com.salescode.channelkart.services;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import com.salescode.channelkart.cache.DistributedCache;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.utils.GlobalLock;
import com.salescode.channelkart.utils.JSONUtils;
import com.salescode.channelkart.utils.NullUtils;
import com.salescode.channelkart.utils.StringUtils;
//import com.salescode.channelkart.utils.TimerUtils;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.channelkart.repository.LocationRepository;
import com.salescode.jooq.generated.tables.pojos.CkLocation;
import com.salescode.jooq.generated.tables.pojos.CkMetadata;
import org.apache.commons.beanutils.PropertyUtils;
//import org.apache.http.conn.ssl.NoopHostnameVerifier;
//import org.apache.http.conn.ssl.TrustAllStrategy;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClientBuilder;
//import org.apache.http.ssl.SSLContextBuilder;
//import org.hibernate.transform.Transformers;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

//import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LocationService extends AbstractCDMService<CkLocation> {
	//
	ObjectMapper objectMapper = new ObjectMapper();
	private final DSLContext dsl;


	private MetaDataService metadataservice;

	@Value("${location.column : area,pincode,territory,city,state,region,zone,cluster,branch,country}")
	private String locationColumns;

	@Autowired
	private DistributedCache distributedCache;

	private static final String CACHE_DOMAIN= "locations";

	private static final String DOMAIN_NAME = "location";

	private static final String DOMAIN_TYPE = "level";

	protected static final String delimiter= " > ";

	private static final Object lock1 = new Object();

	@Autowired
	public LocationService(
			MetaDataService metadataservice,
			DSLContext dsl) {
		this.metadataservice = metadataservice;
		this.dsl = dsl;
	}


	public CkLocation findByLocationHierarchy(String locationHierarchy) {
		return findByLocationHierarchy(locationHierarchy,true);
	}

	public CkLocation findByLocationHierarchy(String locationHierarchy,boolean cached) {
		String lob = SecurityContextUtils.getLob();
		Function<String,CkLocation> function = (String locationHie)->{
			LocationRepository repo= SpringContext.getBean(LocationRepository.class);
			return repo.findByLocationHierarchy(locationHie);
		};

		return (cached) ? distributedCache.withCache(lob,CACHE_DOMAIN, locationHierarchy,function): function.apply(locationHierarchy);
	}

	public CkLocation createNewLocationObj(CkLocation locationObj,String[] locationColumns) {
		try {
			CkLocation finalLocation = new CkLocation();
			for(String locationName:locationColumns) {
				Object locationValue = PropertyUtils.getProperty(locationObj, locationName);
				if(NullUtils.isNotNull(locationValue)){
					PropertyUtils.setProperty(finalLocation, locationName, locationValue );
				}
				if( StringUtils.isEmpty(finalLocation.getLocationType()) && NullUtils.isNotNull(locationValue)){
					finalLocation.setLocationName(String.valueOf(locationValue));
					finalLocation.setLocationType(locationName);
				}
			}
			String[] secondaryColumns = getLocationSecondaryColumns(finalLocation.getLocationType());
			if (secondaryColumns.length > 0) {
				for (String columnKey : secondaryColumns) {
					Object columnValue = PropertyUtils.getProperty(locationObj, columnKey);
					if (NullUtils.isNotNull(columnValue)) {
						PropertyUtils.setProperty(finalLocation, columnKey, columnValue);
					}
				}
			}
			return finalLocation;
		} catch (IllegalAccessException | InvocationTargetException e1) {
//			throw new AccessException(e1.getCause(), ERROR_WHILE_FETCHING_VALUE_FROM_LOCATION_PROPERTY_EXCEPTION,e1.getLocalizedMessage());
		} catch(NoSuchMethodException e2) {
//			throw new UnknownKeyException(e2.getCause(), ASSOCIATE_GETTER_SETTER_METHOD_MISSING_FOR_ONE_OF_THE_FIELD_FROM_LIST_EXCEPTION,
//					Arrays.toString(locationColumns),e2.getLocalizedMessage());
		}
        return locationObj;
    }
//
	public String formHierarchyUsingColumns(CkLocation location, String[] columnList,
											String delimiter) {
		StringBuilder hierarchyStr = new StringBuilder("");
		if (location == null) {
			//throw new ResourceNotFoundException("No location object found.");
		}
		try {
			for (String columnName : columnList) {
				Object locationVal = PropertyUtils.getProperty(location, columnName);
				if (NullUtils.isNotNull(locationVal)) {
					if (hierarchyStr.toString().equals("")) {
						hierarchyStr.append(locationVal.toString());
					} else {
						hierarchyStr.append(delimiter + locationVal.toString());
					}
				}
			}
		} catch (IllegalAccessException | InvocationTargetException e1) {
//			throw new AccessException(e1.getCause(),
//					ERROR_WHILE_FETCHING_VALUE_FROM_LOCATION_PROPERTY_EXCEPTION,
//					e1.getLocalizedMessage());
		} catch (NoSuchMethodException e2) {
//			throw new UnknownKeyException(e2.getCause(),
//					ASSOCIATE_GETTER_SETTER_METHOD_MISSING_FOR_ONE_OF_THE_FIELD_FROM_LIST_EXCEPTION,
//					locationColumns, e2.getLocalizedMessage());
		}
		return hierarchyStr.toString();
	}

	public CkLocation findLocationOrPersistLocation(CkLocation dataObj,Map<String,CkLocation> locationMap) {
		if (NullUtils.isNotNull(dataObj)) {
//			return TimerUtils.withTime("Time Taken to FindOrPersist Location Object", s->{
			String[] columnList = getLocationColumns();
			CkLocation tLocation = dataObj;
			String hierarchyStr = formHierarchyUsingColumns(tLocation, columnList, delimiter);
			if (StringUtils.isNotEmpty(hierarchyStr)) {
				CkLocation locationRes = findByLocationHierarchy(hierarchyStr);
				if (locationRes != null) {
					return locationRes;
				} else {
					GlobalLock.withLock(hierarchyStr, k ->
							saveRecursiveLocationHierarchies(tLocation, columnList,locationMap)
					);
					CkLocation locdata = findByLocationHierarchy(hierarchyStr, false);
					distributedCache.put(SecurityContextUtils.getLob(), CACHE_DOMAIN,
							locdata.getLocationHierarchy(), locdata, true);
					return locdata;

				}

			} else {
				//throw new ResourceNotFoundException("Location hierarchy string found null");
			}
//			});
		} else {
			//throw new ResourceNotFoundException("No location object found.");
		}
		return dataObj;
	}

	//
	public ArrayNode convertToArrayNode(JsonNode jsonNode) {
		if (jsonNode.isArray()) {
			// If it's already an ArrayNode, cast and return
			return (ArrayNode) jsonNode;
		} else {
			// Create a new ArrayNode and add the current JsonNode
			ArrayNode arrayNode = objectMapper.createArrayNode();
			arrayNode.add(jsonNode);
			return arrayNode;
		}
	}

	public String[] getLocationColumns() {
		String lob = SecurityContextUtils.getLob();
		return distributedCache.withCache(lob,CACHE_DOMAIN, "constantlocationlevel",ldata->{
		CkMetadata metadata = metadataservice.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE, true);
		if (metadata == null) {
//				logger.warn("Location level config not found in metadata. Switching to default.");
			return locationColumns.split(",");
		} else {
			List<Entry<String, JsonNode>> localdata = new ArrayList<>();
			ArrayNode arraynode = convertToArrayNode(metadata.getDomainValues());
			Iterator<JsonNode> iter = arraynode.elements();
			while (iter.hasNext()) {
				JsonNode node = iter.next();
				for (Iterator<Entry<String, JsonNode>> iterator = node.fields(); iterator.hasNext(); ) {
					localdata.add(iterator.next());
				}
			}
			Collections.sort(localdata, (Entry<String, JsonNode> o1, Entry<String, JsonNode> o2) -> Integer.compare(o1.getValue().intValue(), o2.getValue().intValue()));
			List<String> result = localdata.stream().map(Entry::getKey).collect(Collectors.toList());
			return result.toArray(new String[0]);
		}
		});
	}

//
	public String[] getLocationSecondaryColumns(String key) {
		return distributedCache.withCache(SecurityContextUtils.getLob(), CACHE_DOMAIN, "LocationType" + key, ldata -> {
			CkMetadata metaData = metadataservice.fetchByValue(DOMAIN_NAME, "secondary_columns", true);
			ArrayNode columnNode = JSONUtils.getObjectMapper().createArrayNode();
			if (metaData != null && metaData.getDomainValues().get(0).has(key)) {
				columnNode = (ArrayNode) metaData.getDomainValues().get(0).get(key);
			}
			String[] columnArr = new String[columnNode.size()];
			for (int i = 0; i < columnNode.size(); i++) {
				columnArr[i] = columnNode.get(i).asText();
			}
			return columnArr;
	   });
	}

	private CkLocation saveRecursiveLocationHierarchies(final CkLocation location, String[] columns,Map<String,CkLocation> locationMap) {
		CkLocation result = null;
		for (int i = 0; i < columns.length; i++) {
			String[] columnsList= new String[columns.length-i];
			System.arraycopy(columns, i, columnsList, 0, columnsList.length);
			String hierarchyStr =formHierarchyUsingColumns(location, columnsList, delimiter);
			if(StringUtils.isNotBlank(hierarchyStr)) {
				synchronized (lock1) {
					CkLocation locdata = findByLocationHierarchy(hierarchyStr);
					if(locdata == null) {
						CkLocation finalLocation = createNewLocationObj(location, columnsList);
						finalLocation.setLocationHierarchy(hierarchyStr);
						CkLocation tresult = refresh(finalLocation,locationMap);
						locationMap.put(tresult.getLocationHierarchy(),tresult);
						if(i == 0) {
							result= tresult;
						}
					}else {
						if(i == 0) {
							result= locdata;
						}
					}
				}
			}
		}
		return result;
	}
	public void clearCache(String lob, String locationHierarchy) {
		distributedCache.clearCache(lob,CACHE_DOMAIN,locationHierarchy);
	}
}
