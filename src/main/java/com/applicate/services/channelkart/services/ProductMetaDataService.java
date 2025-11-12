package com.applicate.services.channelkart.services;

import com.salescode.dim.jooq.impl.ProductMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static com.salescode.dim.jooq.generated.tables.CkProductmetadata.CK_PRODUCTMETADATA;

/**
 * Service class to handle ProductMetaData operations for enrichment layer.
 * Provides utility to fetch loginId by batchCode and persist metadata entries.
 */
public class ProductMetaDataService extends AbstractCDMService<ProductMetaData> {

	private static final Logger LOG = LoggerFactory.getLogger(ProductMetaDataService.class);

	/**
	 * Fetches the loginId for the given batchCode from CK_PRODUCTMETADATA.
	 * @param batchCode the batch code whose loginId should be fetched
	 * @return loginId associated with the given batch code, or null if not found
	 */
	public String getLoginId(String batchCode) {
		if (batchCode == null || batchCode.isBlank()) {
			throw new IllegalArgumentException("Batch code cannot be null or empty");
		}

		try {
			return getDslContext()
					.select(CK_PRODUCTMETADATA.LOGINID)
					.from(CK_PRODUCTMETADATA)
					.where(CK_PRODUCTMETADATA.BATCH_CODE.eq(batchCode))
					.limit(1)
					.fetchOneInto(String.class);

		} catch (Exception e) {
			LOG.error("Error fetching loginId for batch code: {}", batchCode, e);
			throw new RuntimeException("Failed to fetch loginId for batch code: " + batchCode, e);
		}
	}
}