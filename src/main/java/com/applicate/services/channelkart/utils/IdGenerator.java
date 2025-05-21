package com.applicate.services.channelkart.utils;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.services.MetaDataService;
import com.salescode.dim.jooq.generated.tables.pojos.Metadata;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class IdGenerator {

	public static final String DYNAMIC_UNIQUE_KEY = "DynamicUniqueKey";
	private final ArrayNode  metadataNode;
	private static final MetaDataService metadataService=new MetaDataService();

	public IdGenerator(String entityName) {
		this.metadataNode=fetchDynamicPrimaryKeys(entityName);
	}

	/**
	 * it will check in metadata for the dynamic keys for that entityName if there is a flag "generateHash" and it is true
	 * if it is true then a hash for the dynamic_id is generated
	 *
	 * @param entityName
	 * @return
	 */
	private boolean checkGenerateMD5Hash(String entityName) {
		Metadata metaData = metadataService.fetchByValue(entityName, DYNAMIC_UNIQUE_KEY);

		boolean generateHash = false;
		if (metaData != null && metaData.getDomainValues()!=null) {
			JsonNode dynamicKeysNode = metaData.getDomainValues().get(0);
			if (dynamicKeysNode != null) {
				generateHash = dynamicKeysNode.has("generateHash") && dynamicKeysNode.get("generateHash").asBoolean();
			}
		}
		return generateHash;
	}

	public String getId(CommonDataModel cdm) {
		ArrayNode columnArr = metadataNode;
		String entityName = cdm.getClass().getSimpleName();
		boolean generateHash = checkGenerateMD5Hash(entityName);
		if (columnArr != null && !columnArr.isEmpty()) {
			String dynamicId = generateDynamicUniqueKey(cdm, columnArr);
			if (!StringUtils.isNullOrBlank(dynamicId)) {
				String replace = dynamicId.toLowerCase().replace(" ", "-");
				return generateHash ?  getMd5(replace):replace;
			}else {
				return UUID.randomUUID().toString();
			}
		}
		return UUID.randomUUID().toString();
	}

	private static String generateDynamicUniqueKey(CommonDataModel cdm, ArrayNode columnArr) {
		StringBuilder dynamicId = new StringBuilder();
		for (int i = 0; i < columnArr.size(); i++) {
			String columnName = columnArr.get(i).asText();
			try {

				if(dynamicId.length() == 0) {
				dynamicId.append(PropertyUtils.getProperty(cdm, columnName));
			}else {
				dynamicId.append("-").append(PropertyUtils.getProperty(cdm, columnName));
			}
			} catch (IllegalAccessException | InvocationTargetException e1) {
				//   throw new AccessException(e1.getCause(), ERROR_WHILE_FETCHING_VALUE_FROM_LOCATION_PROPERTY_EXCEPTION,e1.getLocalizedMessage());
			} catch(NoSuchMethodException e2) {
				//  throw new UnknownKeyException(e2.getCause(), ASSOCIATE_GETTER_SETTER_METHOD_MISSING_FOR_ONE_OF_THE_FIELD_FROM_LIST_EXCEPTION,
				//          Arrays.toString(locationColumns),e2.getLocalizedMessage());
			}
		}
		return dynamicId.toString();
	}

	public static String getMd5(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] messageDigest = md.digest(input.getBytes());
			BigInteger no = new BigInteger(1, messageDigest);
			return String.format("%032x", no);
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(" Error while generating 'MD5' hash ",e);
			//throw new CustomRuntimeException(e);
		}

	}

	private ArrayNode fetchDynamicPrimaryKeys(String entityName) {
		Metadata metaData=  metadataService.fetchByValue(entityName,DYNAMIC_UNIQUE_KEY);
		ArrayNode columnArr = JSONUtils.getObjectMapper().createArrayNode();
		if(metaData!=null && metaData.getDomainValues()!=null) {
			var dynamicKeys=metaData.getDomainValues().get(0).get("dynamicKeys");
			if(dynamicKeys!=null) {
				columnArr = JSONUtils.convertToArrayNode(dynamicKeys);
			}
		}
		return columnArr;
	}
	



}
