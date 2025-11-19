package com.applicate.services.channelkart.services;

import com.salescode.dim.jooq.generated.tables.records.CkProductmetadataRecord;
import com.salescode.dim.jooq.impl.ProductMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.tables.CkProductmetadata.CK_PRODUCTMETADATA;

/**
 * Service class to handle batch operations for ProductMetaData.
 * Includes lookup of loginId and optimized batch save processing.
 */
public class ProductMetaDataService extends AbstractCDMService<ProductMetaData> {

	private static final Logger LOG = LoggerFactory.getLogger(ProductMetaDataService.class);

	/**
	 * Prepares two lists: items to insert and items to update.
	 * - Builds IDs based on skuCode-loginId-channel.
	 * - Loads existing records in bulk for comparison.
	 * - Compares hash to determine update vs no-op.
	 * - Marks changed items accordingly.
	 */
	private List<List<ProductMetaData>> getItemsToSaveList(List<ProductMetaData> productList) {

		List<List<ProductMetaData>> result = new ArrayList<>();

		for (ProductMetaData product : productList) {
			if (product.getSkuCode() == null || product.getSkuCode().isBlank()) {
				throw new IllegalStateException("SKU code cannot be null or blank.");
			}
			if (product.getLoginid() == null || product.getLoginid().isBlank()) {
				throw new IllegalStateException("Login ID cannot be null or blank before save().");
			}
			if (product.getChannel() == null || product.getChannel().isBlank()) {
				throw new IllegalStateException("Channel cannot be null or blank.");
			}
		}

		List<String> ids = productList.stream()
				.map(p -> {
					String id = String.join("-",
							p.getSkuCode(),
							p.getLoginid(),
							p.getChannel()
					);
					p.setId(id);
					return id;
				})
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

			fillCommonAttributes(product);

			var existing = savedMap.get(product.getId());

			if (existing == null) {
				product.setChanged((byte) 1);
				itemsToInsert.add(product);
			} else {
				product.setChanged((byte) 1);
				itemsToUpdate.add(product);
			}
		}

		result.add(itemsToInsert);
		result.add(itemsToUpdate);

		return result;
	}


	/**
	 * Batch saves ProductMetaData — performs insert or update based on batch_code and hash.
	 */
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
			for (ProductMetaData p : itemsToInsert) {
				applyDefaults(p);
			}
			getDslContext().batchInsert(
					itemsToInsert.stream()
							.map(p -> getDslContext().newRecord(CK_PRODUCTMETADATA, p))
							.collect(Collectors.toList())
			).execute();
		}

		if (!itemsToUpdate.isEmpty()) {
			for (ProductMetaData p : itemsToUpdate) {
				applyDefaults(p);
			}
			getDslContext().batchUpdate(
					itemsToUpdate.stream()
							.map(p -> {
								CkProductmetadataRecord rec =
										getDslContext().newRecord(CK_PRODUCTMETADATA);

								rec.from(p);
								rec.setId(p.getId());
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

	private void applyDefaults(ProductMetaData p) {
		if (p.getBasePrice() == null) p.setBasePrice(BigDecimal.ZERO);
		if (p.getCasePtr() == null) p.setCasePtr(BigDecimal.ZERO);
		if (p.getGst() == null) p.setGst(BigDecimal.ZERO);
		if (p.getTaxAmount() == null) p.setTaxAmount(BigDecimal.ZERO);
		if (p.getOtherUnitPtr() == null) p.setOtherUnitPtr(BigDecimal.ZERO);
		if (p.getMrp() == null) p.setMrp(BigDecimal.ZERO);
		if (p.getCaseMrp() == null) p.setCaseMrp(BigDecimal.ZERO);
		if (p.getOtherUnitMrp() == null) p.setOtherUnitMrp(BigDecimal.ZERO);
		if (p.getCaseToOtherUnitQuantity() == null) p.setCaseToOtherUnitQuantity(BigDecimal.ZERO);
		if (p.getCaseToPieceQuantity() == null) p.setCaseToPieceQuantity(BigDecimal.ZERO);
		if (p.getOtherUnitToPieceQuantity() == null) p.setOtherUnitToPieceQuantity(BigDecimal.ZERO);
		if (p.getPieceToOtherUnitQuantity() == null) p.setPieceToOtherUnitQuantity(BigDecimal.ZERO);
		if (p.getSsp() == null) p.setSsp(BigDecimal.ZERO);
		if (p.getPriority() == null) p.setPriority(0);
		if (p.getSchemePrice() == null) p.setSchemePrice(BigDecimal.ZERO);
	}

}
