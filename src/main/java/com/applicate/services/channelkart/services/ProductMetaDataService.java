package com.applicate.services.channelkart.services;

import com.salescode.dim.cache.CacheManager;
import com.salescode.dim.jooq.generated.tables.records.CkProductmetadataRecord;
import com.salescode.dim.jooq.impl.ProductMetaData;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.tables.CkProductmetadata.CK_PRODUCTMETADATA;

/**
 * Service class to handle batch operations for ProductMetaData.
 * Mirrors the behavior of UserService.batchSave — simple and efficient.
 */
public class ProductMetaDataService extends AbstractCDMService<ProductMetaData> {

	private static final Logger LOG = LoggerFactory.getLogger(ProductMetaDataService.class);
	private static final String CACHE_NAME = "dataintegration-productmetadata";

	/**
	 * Fetches loginId for a given batchCode.
	 */
	public String getLoginId(String batchCode) {
		if (batchCode == null || batchCode.isBlank()) {
			throw new IllegalArgumentException("Batch code cannot be null or empty");
		}

		return getDslContext()
				.select(CK_PRODUCTMETADATA.LOGINID)
				.from(CK_PRODUCTMETADATA)
				.where(CK_PRODUCTMETADATA.BATCH_CODE.eq(batchCode))
				.limit(1)
				.fetchOneInto(String.class);
	}

	/**
	 * Batch saves ProductMetaData — performs insert or update based on batch_code and hash.
	 */
	private List<List<ProductMetaData>> getItemsToSaveList(List<ProductMetaData> productList) {

		List<List<ProductMetaData>> result = new ArrayList<>();

		List<String> ids = productList.stream()
				.map(p -> String.join("-",
						Optional.ofNullable(p.getSkuCode()).orElse("NA"),
						Optional.ofNullable(p.getLoginid()).orElse("NA"),
						Optional.ofNullable(p.getChannel()).orElse("NA")
				))
				.collect(Collectors.toList());

		Map<String, com.salescode.dim.jooq.generated.tables.pojos.Productmetadata> savedMap =
				getDslContext().selectFrom(CK_PRODUCTMETADATA)
						.where(CK_PRODUCTMETADATA.ID.in(ids))
						.fetch()
						.intoMap(CK_PRODUCTMETADATA.ID,
								r -> r.into(com.salescode.dim.jooq.generated.tables.pojos.Productmetadata.class));

		List<ProductMetaData> itemsToInsert = new ArrayList<>();
		List<ProductMetaData> itemsToUpdate = new ArrayList<>();

		for (ProductMetaData product : productList) {

			String id = String.join("-",
					Optional.ofNullable(product.getSkuCode()).orElse("NA"),
					Optional.ofNullable(product.getLoginid()).orElse("NA"),
					Optional.ofNullable(product.getChannel()).orElse("NA")
			);
			product.setId(id);

			com.salescode.dim.jooq.generated.tables.pojos.Productmetadata existing = savedMap.get(id);

			fillCommonAttributes(product);

			if (existing == null) {
				product.setChanged((byte)1);
				itemsToInsert.add(product);
			} else {
				if (!Objects.equals(product.getHash(), existing.getHash())) {
					product.setChanged((byte)1);
					itemsToUpdate.add(product);
				} else {
					product.setChanged((byte)1);
				}
			}
		}

		result.add(itemsToInsert);
		result.add(itemsToUpdate);
		return result;
	}


	@Override
	public Collection<ProductMetaData> batchSave(Collection<ProductMetaData> inputList) {

		if (inputList == null || inputList.isEmpty()) {
			return Collections.emptyList();
		}
		List<ProductMetaData> productList = new ArrayList<>(inputList);
		List<List<ProductMetaData>> items = getItemsToSaveList(productList);

		List<ProductMetaData> itemsToInsert = items.get(0);
		List<ProductMetaData> itemsToUpdate = items.get(1);

		if (!itemsToInsert.isEmpty()) {
			getDslContext().batchInsert(
					itemsToInsert.stream()
							.map(p -> getDslContext().newRecord(CK_PRODUCTMETADATA, p))
							.collect(Collectors.toList())
			).execute();
		}
		if (!itemsToUpdate.isEmpty()) {
			getDslContext().batchUpdate(
					itemsToUpdate.stream()
							.map(p -> {
								CkProductmetadataRecord rec = getDslContext().newRecord(CK_PRODUCTMETADATA, p);
								rec.changed(CK_PRODUCTMETADATA.ID, false);
								return rec;
							})
							.collect(Collectors.toList())
			).execute();
		}

		LOG.info("Batch saved {} ProductMetaData records ({} inserts, {} updates).",
				productList.size(), itemsToInsert.size(), itemsToUpdate.size());

		return productList;
	}

}
