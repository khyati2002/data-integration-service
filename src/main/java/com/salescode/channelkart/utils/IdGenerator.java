package com.salescode.channelkart.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.channelkart.models.CommonDataModel;

import com.salescode.channelkart.services.SpringContext;
import com.salescode.channelkart.services.MetaDataService;
import com.salescode.jooq.generated.tables.pojos.CkMetadata;

import java.util.UUID;

public class IdGenerator {


	private ArrayNode metadataNode;
	private EntityUtils entityUtils;

	public IdGenerator(String entityName) {
		this.metadataNode=fetchDynamicPrimaryKeys(entityName);
		this.entityUtils = SpringContext.getBean(EntityUtils.class);
	}

	/**
	 * it will check in metadata for the dynamic keys for that entityName if there is a flag "generateHash" and it is true
	 * if it is true then a hash for the dynamic_id is generated
	 *
	 * @param entityName
	 * @return
	 */
	private boolean checkGenerateMD5Hash(String entityName) {
		MetaDataService metaDataService = SpringContext.getBean(MetaDataService.class);
		CkMetadata metaData = metaDataService.fetchByValue(entityName, "DynamicUniqueKey");

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
		if (columnArr!=null && columnArr.size() > 0) {
			StringBuilder dynamicId = new StringBuilder();
			for (int i = 0; i < columnArr.size(); i++) {
				String columnName = columnArr.get(i).asText();
				if(dynamicId.length() == 0) {
					dynamicId.append(entityUtils.getBeanProperty(cdm, columnName));
				}else {
					dynamicId.append("-").append(entityUtils.getBeanProperty(cdm, columnName));
				}
			}
			if (dynamicId.length() != 0) {
				String replace = dynamicId.toString().toLowerCase().replace(" ", "-");
				return generateHash ?  EncodingUtils.getMd5(replace):replace;
			}else {
				return UUID.randomUUID().toString();
			}
		}
		return UUID.randomUUID().toString();
	}


	private ArrayNode fetchDynamicPrimaryKeys(String entityName) {
		CkMetadata metaData=  SpringContext.getBean(MetaDataService.class).fetchByValue(entityName,"DynamicUniqueKey");
		ArrayNode columnArr = JSONUtils.getObjectMapper().createArrayNode();
		if(metaData != null) {
			columnArr = (ArrayNode) metaData.getDomainValues().get(0).get("dynamicKeys");
		}
		return columnArr;
	}




}
