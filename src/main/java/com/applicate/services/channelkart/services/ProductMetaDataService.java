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
	@Override
	public Collection<ProductMetaData> batchSave(Collection<ProductMetaData> productList) {
		if (productList == null || productList.isEmpty()) {
			return Collections.emptyList();
		}

		DSLContext dsl = getDslContext();
		List<ProductMetaData> products = new ArrayList<>(productList);

		// Build deterministic unique IDs (skuCode-loginId-channel)
		for (ProductMetaData product : products) {
			if (product == null) {
				continue;
			}

			String id = String.join("-",
					Optional.ofNullable(product.getSkuCode()).orElse("NA"),
					Optional.ofNullable(product.getLoginid()).orElse("NA"),
					Optional.ofNullable(product.getChannel()).orElse("NA")
			);
			product.setId(id);

			if (product.getVersion() == null) {
				product.setVersion(0);
			}
			super.addHash(product);
		}

		dsl.transaction(configuration -> {
			DSLContext ctx = DSL.using(configuration);

			List<CkProductmetadataRecord> records = products.stream()
					.map(p -> ctx.newRecord(CK_PRODUCTMETADATA, p))
					.collect(Collectors.toList());

			for (CkProductmetadataRecord rec : records) {
				ctx.insertInto(CK_PRODUCTMETADATA)
						.set(rec)
						.onDuplicateKeyUpdate()
						.set(rec)
						.execute();
			}
		});

		CacheManager.getInstance().evictAll(CACHE_NAME);
		LOG.info("Batch saved {} ProductMetaData records.", products.size());
		return products;
	}
}
