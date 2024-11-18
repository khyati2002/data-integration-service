///*
// * Copyright (c) 2021. All rights reserved.
// * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
// *
// */
package com.salescode.dataintegration.etl.cdm.services;

//import com.applicate.analytics.exception.SystemRuntimeException;
//import com.applicate.services.channelkart.apifilter.ApiFilterNativeQueryBuilder;
//import com.applicate.services.channelkart.cache.DistributedCache;
//import com.applicate.services.channelkart.dto.LocationDTO;
//import com.applicate.services.channelkart.exceptions.AccessException;
//import com.applicate.services.channelkart.exceptions.CustomRuntimeException;
//import com.applicate.services.channelkart.exceptions.ResourceNotFoundException;
//import com.applicate.services.channelkart.exceptions.UnknownKeyException;
//import com.applicate.services.channelkart.models.Location;
//import com.applicate.services.channelkart.models.MetaData;
//import com.applicate.services.channelkart.models.OutletDetails;
//import com.applicate.services.channelkart.models.Profile;
//import com.applicate.services.channelkart.models.enums.Sequence;
//import com.applicate.services.channelkart.security.SecurityContextUtils;
//import com.applicate.services.channelkart.utils.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

//import com.salescode.channelkart.services.SpringContext;
import com.salescode.channelkart.utils.GlobalLock;
import com.salescode.channelkart.utils.JSONUtils;
import com.salescode.channelkart.utils.NullUtils;
import com.salescode.channelkart.utils.StringUtils;
//import com.salescode.channelkart.utils.TimerUtils;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.dataintegration.etl.cdm.repository.LocationRepository;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

//import javax.annotation.Resource;
import javax.net.ssl.SSLContext;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LocationService extends AbstractCDMService<CkLocation> {
	//
	ObjectMapper objectMapper = new ObjectMapper();
	private final DSLContext dsl;
	//	private static final String ASSOCIATE_GETTER_SETTER_METHOD_MISSING_FOR_ONE_OF_THE_FIELD_FROM_LIST_EXCEPTION = "Associate getter/setter method missing for one of the field from list[{}]. Exception:[{}]";
//	private static final String ERROR_WHILE_FETCHING_VALUE_FROM_LOCATION_PROPERTY_EXCEPTION = "Error while fetching value from location property. Exception:[{}]";
//	private Logger logger = LoggerFactory.getLogger(this.getClass());
//
////	private final SequenceInfoService sequenceInfoService;
	private MetaDataService metadataservice;
	//
//	LocationRepository locationRepository;
//
////	@Autowired
////	LocationService locationDBService;
//
////	@Autowired
////  LocationManagementService locationManagementService;
//
////	private OutletDetailsService outletDetailsService;
//
//	@Autowired
//	private EntityManager em;
//
	@Value("${location.column}")
	private String locationColumns;


	//
//	private static final String CUSTOM_LOCATION_QUERY =
//			"SELECT " +
//				"id, " +
//				"location_hierarchy locationHierarchy, " +
//				"area, " +
//				"territory, " +
//				"city, " +
//				"state, " +
//				"region, " +
//				"zone, " +
//				"branch, " +
//				"country, " +
//				"location_type locationType, " +
//				"location_name locationName, " +
//				"pincode, " +
//				"town, " +
//				"cluster, " +
//				"areacode, " +
//				"territory_code territoryCode, " +
//				"city_code cityCode, " +
//				"state_code stateCode, " +
//				"region_code regionCode, " +
//				"zone_code zoneCode, " +
//				"cluster_code clusterCode, " +
//				"branch_code branchCode, " +
//				"country_code countryCode, " +
//				"district, " +
//				"district_code districtCode, " +
//				"town_code townCode, " +
//				"salescode_id salescodeId " +
//			"FROM ck_location " +
//			"WHERE 1=1 {DYNAMIC_FILTER} LIMIT {DYNAMIC_LIMIT}";
//
//	private static final String CACHE_DOMAIN= "locations";
//
	private static final String DOMAIN_NAME = "location";

	private static final String DOMAIN_TYPE = "level";

	//
//	private MetaDataService metadataservice;
//
	protected static final String delimiter= " > ";
//
//	private DistributedCache distributedCache;
//
//	@Autowired
//	private HttpClient httpClient;
//
//	@Resource
//	private TransactionTemplate transactionTemplate;
//
//	@Autowired
//	private ApiFilterNativeQueryBuilder<CkLocation> abstractNativeQueryBuilder;
//
//	@Autowired
//	private QueryService queryService;
//
	private static final Object lock1 = new Object();
//
	@Autowired
	public LocationService(
//			LocationRepository locationRepository,
			MetaDataService metadataservice,
			DSLContext dsl
//			DistributedCache distributedCache,
//			SequenceInfoService sequenceInfoService
	) {
//		super(locationRepository);
//		this.locationRepository = locationRepository;
		this.metadataservice = metadataservice;
		this.dsl = dsl;
//		this.distributedCache = distributedCache;
//		this.sequenceInfoService = sequenceInfoService;
	}

	//
//	public List<Map<String,Object>> getLocation(Map<String,String> requestParams){
//		String finalQuery=abstractNativeQueryBuilder.get(CkLocation.class,CUSTOM_LOCATION_QUERY,requestParams);
//		return queryService.execute(finalQuery);
//	}
//
//	public List<Location> persistLocation(List<Location> locations) {
//		List<Location> savedObj= new ArrayList<>();
//		locations.forEach(location -> savedObj.add(findLocationOrPersistLocation(location)));
//		return savedObj;
//	}
//
	public CkLocation findByLocationHierarchy(String locationHierarchy) {
		return findByLocationHierarchy(locationHierarchy,true);
	}
//
	public CkLocation findByLocationHierarchy(String locationHierarchy,boolean cached) {
		//String lob = SecurityContextUtils.getLob();
		Function<String,CkLocation> function = (String locationHie)->{
			LocationRepository repo= SpringContext.getBean(LocationRepository.class);
			return repo.findByLocationHierarchy(locationHie);
		};

//		return (cached) ? distributedCache.withCache(lob,CACHE_DOMAIN, locationHierarchy,function):
				return function.apply(locationHierarchy);
	}
//
//
//
//	public Location createLocationObj(Location locationObj,String[] locationColumns) {
//		try {
//			for(String locationName:locationColumns) {
//				Object locationValue = PropertyUtils.getProperty(locationObj, locationName);
//				if(NullUtils.isNotNull(locationValue)){
//					locationObj.setLocationName(String.valueOf(locationValue));
//					locationObj.setLocationType(locationName);
//					break;
//				}
//			}
//		} catch (IllegalAccessException | InvocationTargetException e1) {
//			throw new AccessException(e1.getCause(), ERROR_WHILE_FETCHING_VALUE_FROM_LOCATION_PROPERTY_EXCEPTION,e1.getLocalizedMessage());
//		} catch(NoSuchMethodException e2) {
//			throw new UnknownKeyException(e2.getCause(), ASSOCIATE_GETTER_SETTER_METHOD_MISSING_FOR_ONE_OF_THE_FIELD_FROM_LIST_EXCEPTION,
//					Arrays.toString(locationColumns),e2.getLocalizedMessage());
//		}
//		return locationObj;
//	}
//
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
//
//	public String formHierarchyUsingColumns(Location location) {
//		return formHierarchyUsingColumns(location,getLocationColumns(),delimiter);
//	}
//
//	public String formQueryUsingColumns(JsonNode sourceObj,String[] columnList) {
//		StringBuilder hierarchyStr = new StringBuilder("");
//		for(String columnName:columnList) {
//			if(sourceObj.has(columnName)) {
//				String nativeColumnName=entityUtils.getTableField(Location.class, columnName);
//				if(hierarchyStr.toString().equals("")) {
//					hierarchyStr.append(nativeColumnName+" = '"+sourceObj.get(columnName).asText()+"'");
//				}else {
//					hierarchyStr.append(" and "+nativeColumnName+" = '"+sourceObj.get(columnName).asText()+"'");
//				}
//			}
//		}
//		return hierarchyStr.toString();
//	}
//
	public CkLocation findLocationOrPersistLocation(CkLocation dataObj) {
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
							saveRecursiveLocationHierarchies(tLocation, columnList)
					);
					CkLocation locdata = findByLocationHierarchy(hierarchyStr, false);
//					distributedCache.put(SecurityContextUtils.getLob(), CACHE_DOMAIN,
//							locdata.getLocationHierarchy(), locdata, true);
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
		//String lob = SecurityContextUtils.getLob();
//		return distributedCache.withCache(lob,CACHE_DOMAIN, "constantlocationlevel",ldata->{
		CkMetadata metadata = metadataservice.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE, true);
		if (metadata == null) {
//				logger.warn("Location level config not found in metadata. Switching to default.");
			return locationColumns.split(",");
		} else {
			List<Entry<String, JsonNode>> localdata = new ArrayList<>();
			ArrayNode arraynode = convertToArrayNode(metadata.getDomainValues(dsl));
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
//		});
	}

//
	public String[] getLocationSecondaryColumns(String key) {
	//	return distributedCache.withCache(SecurityContextUtils.getLob(), CACHE_DOMAIN, "LocationType" + key, ldata -> {
			CkMetadata metaData = metadataservice.fetchByValue(DOMAIN_NAME, "secondary_columns", true);
			ArrayNode columnNode = JSONUtils.getObjectMapper().createArrayNode();
			if (metaData != null && metaData.getDomainValues(dsl).get(0).has(key)) {
				columnNode = (ArrayNode) metaData.getDomainValues(dsl).get(0).get(key);
			}
			String[] columnArr = new String[columnNode.size()];
			for (int i = 0; i < columnNode.size(); i++) {
				columnArr[i] = columnNode.get(i).asText();
			}
			return columnArr;
	//	});
	}
//	@SuppressWarnings("unchecked")
//	public List<Location> findByQuery(String query,boolean isNative){
//		if(NullUtils.isNotNull(query)) {
//			List<Location> location= null;
//			if(isNative) {
//				Query q = em.createNativeQuery(query, Location.class);
//				location = q.getResultList();
//			}
//			else {
//				TypedQuery<Location> q = em.createQuery(query, Location.class);
//				location = q.getResultList();
//			}
//			return location;
//		}
//		return null;
//	}
//
//	public Location findLocation(Location dataObj) {
//		try {
//			String[] columnList = getLocationColumns();
//			String hierarchyStr =formHierarchyUsingColumns(dataObj, columnList, delimiter);
//			if (!"".equals(hierarchyStr)) {
//				Location locationRes = findByLocationHierarchy(hierarchyStr);
//				if (locationRes != null) {
//					return locationRes;
//				}
//			}
//		} catch (Exception e) {
//			logger.error("Exception: ",e);
//		}
//		return null;
//	}
//
//	@SuppressWarnings("unchecked")
//	public List<Location> findLocationsByFields(Location location,String type) {
//		if(location !=null) {
//			try {
//				String[] columnList = getLocationColumns();
//				JsonNode locObj = JSONUtils.getObjectMapper().convertValue(location,JsonNode.class);
//				if(type == null) {
//					for(String colname: columnList) {
//						if(locObj.has(colname)) {
//							type= colname;
//							break;
//						}
//					}
//				}
//				if(type == null) {
//					throw new com.applicate.services.channelkart.exceptions.IllegalArgumentException("Passed location type found null and system not able to find suitable type from given hierarchy metadata. Please check the data.");
//				}
//				String queryByField =formQueryUsingColumns(locObj, columnList);
//				return findByQuery("select * from ck_location where "+queryByField+" and location_type ='"+type+"'" , true);
//			} catch (Exception e) {
//				logger.error("Exception: ",e);
//			}
//		}
//		return null;
//	}
//
//	public List<Map<String,Object>> findLocationUsingNativeQuery(Location loc) {
//		try {
//
//			String query = "select * from ck_location where ";
//			StringBuilder whereCondition = new StringBuilder("");
//
//			String[] columnList = getLocationColumns();
//			ObjectNode locObj = JSONUtils.getObjectMapper().convertValue(loc, ObjectNode.class);
//			boolean foundValue = false;
//			for(String columnName:columnList) {
//				if(locObj.has(columnName)) {
//					String value = locObj.get(columnName).asText();
//					if(StringUtils.isValidString(value)) {
//						foundValue = true;
//						if(!"".equals(whereCondition.toString())) {
//							whereCondition.append(" and ");
//						}
//						whereCondition.append(columnName+" = '"+value+"'");
//					}
//				}
//			}
//			if(foundValue) {
//				Query locQry =  em.createNativeQuery(query+whereCondition);
//				locQry.unwrap(org.hibernate.Query.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
//				return locQry.getResultList();
//			}
//			return null;
//		}catch(Exception e) {
//			throw new CustomRuntimeException(e,"Exception occured while fetching location data "+e.getMessage());
//		}
//	}
//
//	public Location reloadCache(String locationHierarchy) {
//		String lob = SecurityContextUtils.getLob();
//		Function<String,Location> function = (String locationHie)->{
//			LocationRepository repo= SpringContext.getBean(LocationRepository.class);
//			return repo.findByLocationHierarchy(locationHie);
//		};
//		distributedCache.clearCache(lob,CACHE_DOMAIN,locationHierarchy);
//		return  distributedCache.withCache(lob,CACHE_DOMAIN, locationHierarchy,function);
//	}
//
//	/**
//	 * Location api profile config.
//	 *
//	 * @return the profile
//	 */
//	public Profile locationApiProfileConfig() {
//
//		ProfileService profileService= (ProfileService) ServiceLocator.lookup(Profile.class);
//
//		Profile profile= profileService.findByNameAndType(DOMAIN_NAME,"external");
//
//		if(profile == null) {
//			throw new SystemRuntimeException("External location profile not found. Expecting Profile[ name : location, type : external ]");
//		}
//		if(profile.getAttributes() == null || profile.getAttributes().isNull()) {
//			throw new SystemRuntimeException("External location profile found but missconfigured. Profile[ name : location, type : external ]");
//		}
//
//		JsonNode node= profile.getAttributes();
//		Assert.isTrue(StringUtils.isNotBlank(node.get("countryUrl").asText()), "Misconfigured Profile[ name : location, type : external ]. countryUrl found null or blank");
//		Assert.isTrue(StringUtils.isNotBlank(node.get("hierarchyUrl").asText()), "Misconfigured Profile[ name : location, external : type ]. hierarchyUrl found null or blank");
//		Assert.isTrue(StringUtils.isNotBlank(node.get("apiKey").asText()), "Misconfigured Profile[ name : location, type : external ]. apiKey found null or blank");
//		Assert.isTrue(StringUtils.isNotBlank(node.get("userId").asText()), "Misconfigured Profile[ name : location, type : external ]. userId found null or blank");
//
//		return profile;
//	}
//
//	/**
//	 * Gets the external location.
//	 *
//	 * @param url the url
//	 * @param function the function
//	 * @return the external location
//	 * @throws Exception the exception
//	 */
//	@SuppressWarnings("unchecked")
//	public List<LocationDTO> getExternalLocation(String url, Function<Map<String,Object>,LocationDTO> function) {
//
//		return distributedCache.withCache(SecurityContextUtils.getLob(), CACHE_DOMAIN, url, s -> {
//			SSLContext sslContext;
//			try {
//				sslContext = new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();
//			} catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
//				throw new CustomRuntimeException((String.format("Failed to create SSL context : %s", e.getMessage())));
//			}
//			CloseableHttpClient httpClientRequest = HttpClientBuilder.create().setSSLContext(sslContext).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();
//			HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClientRequest);
//			RestTemplate restTemplate = new RestTemplate(requestFactory);
//			ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
//			if(responseEntity.getStatusCode() != HttpStatus.OK) {
//				throw new CustomRuntimeException(String.format("Got %s response for getting countries through external api. Please check the request. Body : %s",
//						responseEntity.getStatusCode(),responseEntity.getBody()));
//			}
//
//			String body= responseEntity.getBody();
//			if(org.apache.commons.lang3.StringUtils.isBlank(body)) {
//				throw new CustomRuntimeException(String.format("Recieved empty response for getting countries through external api. Please check the request. Body : %s",
//						responseEntity.getBody()));
//			}
//
//			Map<String,Object> data= JSONUtils.convert(body, Map.class);
//			if(!Boolean.parseBoolean(String.valueOf(data.get("success")))){
//				throw new CustomRuntimeException(String.format("Getting invalid response from api server : %s",body));
//			}
//
//			data.remove("success");
//			data.remove("remaining_lookups");
//			return data.values().stream()
//					.map(m->function.apply((Map<String,Object>)m))
//					.collect(Collectors.toList());
//		});
//
//	}
//
//	public List<Location> findByLocationHierarchyWithParent(String hierarchy){
//		Set<String> locationHierarchies = new HashSet<>();
//		locationHierarchies.add(hierarchy);
//		while(hierarchy.contains(delimiter)){
//			hierarchy = hierarchy.substring(hierarchy.indexOf(delimiter) + delimiter.length(), hierarchy.length());
//			locationHierarchies.add(hierarchy);
//		}
//		return findByLocationHierarchy(locationHierarchies);
//	}
//	public List<Location> findByLocationHierarchy(Set<String> hierarhcies){
//		return locationRepository.findByLocationHierarchyIn(hierarhcies);
//	}
//
//	public Location findBySalescodeId (String salescodeId) {
//		return findBySalescodeId(salescodeId, true);
//	}
//
//	public Location findBySalescodeId (String salescodeId, boolean cached){
//		String lob = SecurityContextUtils.getLob();
//		Function<String,Location> function = (String salescodeIdStr) -> locationRepository.findBySalescodeId(salescodeIdStr);
//		return (cached) ?
//				distributedCache.withCache(lob, CACHE_DOMAIN, salescodeId, function) :
//				function.apply(salescodeId);
//	}
//
//	/**
//	 * Save recursive location hierarchies.
//	 *
//	 * @param location the location
//	 * @param columns the columns
//	 * @return the location
//	 */
	private CkLocation saveRecursiveLocationHierarchies(final CkLocation location, String[] columns) {
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
						CkLocation tresult = this.save(refresh(finalLocation));
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
//
//	public Location findByOutletCode(String outletCode) {
//		OutletDetails outletDetails = getOutletDetails(outletCode);
//		return outletDetails.getLocation();
//	}
//
//	public String findSalescodeIdByOutletCode(String outletCode) {
//		return findByOutletCode(outletCode).getSalescodeId();
//	}
//
//	public List<Location> findByOutletCodeWithParent(String outletCode) {
//		OutletDetails outletDetails = getOutletDetails(outletCode);
//		return findByLocationHierarchyWithParent(outletDetails.getLocationHierarchy().getLocationHierarchy());
//	}
//
//	public Set<String> findSalescodeIdByOutletCodeWithParent(String outletCode) {
//		Set<String> salescodeIds = new HashSet<>();
//		List<Location> locations = findByOutletCodeWithParent(outletCode);
//		for(Location location:locations){
//			salescodeIds.add(location.getSalescodeId());
//		}
//		return salescodeIds;
//	}
//
//	private OutletDetails getOutletDetails(String outletCode) {
//		if(outletDetailsService == null) {
//			outletDetailsService = SpringContext.getBean(OutletDetailsService.class);
//		}
//		return outletDetailsService.findByOutletCode(outletCode);
//	}
//
//	@Override
//	public Location refresh(Location cdmObject){
//		String[] columnList=getLocationColumns();
//		String hierarchy=formHierarchyUsingColumns(cdmObject, columnList, delimiter);
//		cdmObject = createLocationObj(cdmObject, columnList);
//		cdmObject.setLocationHierarchy(hierarchy);
//		cdmObject = setSalescodeId(cdmObject);
//		return super.refresh(cdmObject);
//	}
//
//	public Location setSalescodeId(Location locationObj){
//		if(StringUtils.isNullOrBlank(locationObj.getSalescodeId())) {
//			locationObj.setSalescodeId(sequenceInfoService.generateSalescodeId(Sequence.LOCATION.getSequenceName()));
//		}
//		return locationObj;
//	}
//
//	public void clearCache(String lob, String locationHierarchy) {
//		distributedCache.clearCache(lob,CACHE_DOMAIN,locationHierarchy);
//	}
}
