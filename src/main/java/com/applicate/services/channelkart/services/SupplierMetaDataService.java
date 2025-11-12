package com.applicate.services.channelkart.services;


import com.applicate.services.channelkart.utils.IdGenerator;
import com.salescode.dim.jooq.generated.tables.records.CkSupplierMetadataRecord;
import com.salescode.dim.jooq.impl.SupplierMetadata;

import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_SUPPLIER_METADATA;

public class SupplierMetaDataService extends AbstractCDMService<SupplierMetadata> {

	public void saveSupplier(String userLoginId, List<SupplierMetadata> supplierMetaDataList) {
		if (supplierMetaDataList == null || supplierMetaDataList.isEmpty()) {
			return;
		}

		// Find existing supplier metadata for this user
		List<SupplierMetadata> existingSupplierMetaData = findByUserLoginId(userLoginId);

		// Filter only new/different items that need to be saved
		List<SupplierMetadata> itemsToSave = supplierMetaDataList.stream().filter(newItem -> !hasMatchingItem(newItem, existingSupplierMetaData)).collect(Collectors.toList());


		batchSave(itemsToSave);
	}


	private boolean hasMatchingItem(SupplierMetadata newItem, List<SupplierMetadata> existingList) {
		return existingList.stream().anyMatch(existingItem -> compareBusinessFields(existingItem, newItem));
	}

	private boolean compareBusinessFields(SupplierMetadata existing, SupplierMetadata newSupplier) {
		return Objects.equals(existing.getActiveStatus(), newSupplier.getActiveStatus()) && Objects.equals(existing.getActiveStatusReason(), newSupplier.getActiveStatusReason()) && Objects.equals(existing.getExtendedAttributes(), newSupplier.getExtendedAttributes()) && Objects.equals(existing.getLob(), newSupplier.getLob()) && Objects.equals(existing.getLevel(), newSupplier.getLevel()) && Objects.equals(existing.getMax(), newSupplier.getMax()) && Objects.equals(existing.getMin(), newSupplier.getMin()) && Objects.equals(existing.getType(), newSupplier.getType()) && Objects.equals(existing.getUserLoginid(), newSupplier.getUserLoginid()) && Objects.equals(existing.getSource(), newSupplier.getSource());
	}

	public List<SupplierMetadata> findByUserLoginId(String userLoginId) {
		return getDslContext().selectFrom(CK_SUPPLIER_METADATA).where(CK_SUPPLIER_METADATA.USER_LOGINID.eq(userLoginId)).fetchInto(SupplierMetadata.class);
	}

	@Override
	public List<SupplierMetadata> batchSave(Collection<SupplierMetadata> supplierMetaDataCollection) {
		List<SupplierMetadata> supplierMetaDataList = new ArrayList<>(supplierMetaDataCollection);

		if (supplierMetaDataList.isEmpty()) {
			return supplierMetaDataList;
		}

		// All items are new since we've already filtered for differences
		getDslContext().batchInsert(supplierMetaDataList.stream().map(supplier -> {
//                                supplier.setId(UUID.randomUUID().toString());
			supplier.setId(new IdGenerator(supplier.getClass().getSimpleName()).getId(supplier));
			supplier.setChanged(true);
			CkSupplierMetadataRecord record = getDslContext().newRecord(CK_SUPPLIER_METADATA, supplier);
			return record;
		}).collect(Collectors.toList())).execute();


		return supplierMetaDataList;
	}

}
