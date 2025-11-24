package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.utils.IdGenerator;

import com.salescode.dim.jooq.generated.tables.pojos.Productmetadata;
import com.salescode.dim.jooq.impl.ProductMetaData;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_PRODUCTMETADATA;

public class ProductMetaDataService extends AbstractCDMService<ProductMetaData> {

	private static final Logger LOG = LoggerFactory.getLogger(ProductMetaDataService.class);
	private final LocationService locationService;

	public ProductMetaDataService() {
		this.locationService = new LocationService();
	}

	public List<List<ProductMetaData>> getDataToSaveList(List<ProductMetaData> productMetadataList) {
		List<List<ProductMetaData>> result = new ArrayList<>();
		List<ProductMetaData> itemsToInsert = new ArrayList<>();
		List<ProductMetaData> itemsToUpdate = new ArrayList<>();

		if (productMetadataList == null || productMetadataList.isEmpty()) {
			result.add(itemsToInsert);
			result.add(itemsToUpdate);
			return result;
		}

		for (ProductMetaData product : productMetadataList) {
			product.setId(new IdGenerator(product.getClass().getSimpleName()).getId(product));
			product.setActiveStatus(ActiveStatus.ACTIVE);
			product.setChanged((byte)1);
		}

		List<String> batchCodes = productMetadataList.stream().map(ProductMetaData::getId).filter(Objects::nonNull).collect(Collectors.toList());

		if (batchCodes.isEmpty()) {
			result.add(itemsToInsert);
			result.add(itemsToUpdate);
			return result;
		}
		
		Map<String, Productmetadata> existingBatchCodeMap = getDslContext().selectFrom(CK_PRODUCTMETADATA).where(CK_PRODUCTMETADATA.ID.in(batchCodes)).fetch().stream().collect(Collectors.toMap(rec -> rec.get(CK_PRODUCTMETADATA.ID), rec -> rec.into(Productmetadata.class), (a, b) -> a));

		for (ProductMetaData product : productMetadataList) {
			Productmetadata existing = existingBatchCodeMap.get(product.getId());
			ProductMetaData existingDomain = null;
			if (existing != null) {
				existingDomain = new ProductMetaData(existing);
			}
			fillAttributes(product, existingDomain);
			fillCommonAttributes(product);

			if (existingBatchCodeMap.get(product.getId())==null) {
				product.setVersion(0);
				product.setOperationPerformed(ActionType.INSERT);
				itemsToInsert.add(product);
			} else {
				product.setVersion((existing != null ? existing.getVersion() : 0) + 1);
				product.setOperationPerformed(ActionType.UPDATE);

				itemsToUpdate.add(product);
			}
		}

		result.add(itemsToInsert);
		result.add(itemsToUpdate);
		return result;
	}

	@Override
	public Collection<ProductMetaData> batchSave(Collection<ProductMetaData> productMetadataList) {
		LOG.info("Size of list is {}", productMetadataList.size());

		List<List<ProductMetaData>> saveItemsList = getDataToSaveList(new ArrayList<>(productMetadataList));
		DSLContext dsl = getDslContext();

		// Inserts
		if (!saveItemsList.get(0).isEmpty()) {
			dsl.batchInsert(saveItemsList.get(0).stream().map(prod -> dsl.newRecord(CK_PRODUCTMETADATA, prod)).collect(Collectors.toList())).execute();
		}

		// Updates
		if (!saveItemsList.get(1).isEmpty()) {
			dsl.batchUpdate(saveItemsList.get(1).stream().map(prod -> dsl.newRecord(CK_PRODUCTMETADATA, prod)).collect(Collectors.toList())).execute();
		}

		LOG.info("Batch save successful for ProductMetadata");
		return productMetadataList;
	}
}