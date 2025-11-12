package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.utils.IdGenerator;
import com.salescode.dim.jooq.generated.tables.records.CkSupplierMetadataRecord;
import com.salescode.dim.jooq.generated.tables.records.CkUserMetadataRecord;
import com.salescode.dim.jooq.impl.SupplierMetadata;
import com.salescode.dim.jooq.impl.UserMetadata;


import java.util.*;
import java.util.stream.Collectors;
import static com.salescode.dim.jooq.generated.Tables.CK_SUPPLIER_METADATA;


public class SupplierMetaDataService extends AbstractCDMService<SupplierMetadata> {

	public void saveSupplier(String userLoginId, List<SupplierMetadata> supplierMetaDataList) {
		if (supplierMetaDataList == null || supplierMetaDataList.isEmpty()) {
			return;
		}
		supplierMetaDataList.forEach((supplierMetadata -> supplierMetadata.setId(userLoginId)));

		// Find existing supplier metadata for this user
		List<SupplierMetadata> existingSupplierMetaData = findByUserLoginId(userLoginId);

		// Filter only new/different items that need to be saved
		List<SupplierMetadata> itemsToSave = supplierMetaDataList.stream().filter(newItem -> !hasMatchingItem(newItem, existingSupplierMetaData)).collect(Collectors.toList());

		getItemsToSaveList(supplierMetaDataList);
		batchSave(itemsToSave);
	}

	public List<List<SupplierMetadata>> getItemsToSaveList(List<SupplierMetadata> supplierMetadataList) {
		List<List<SupplierMetadata>> result = new ArrayList<>();
		List<String> users = supplierMetadataList.stream().map(SupplierMetadata::getId).collect(Collectors.toList());

		Map<String, com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata> savedList = getDslContext().selectFrom(CK_SUPPLIER_METADATA).where(CK_SUPPLIER_METADATA.ID.in(users)).fetch().intoMap(CK_SUPPLIER_METADATA.ID, record -> record.into(com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata.class));

		List<SupplierMetadata> itemsToInsert = new ArrayList<>();
		List<SupplierMetadata> itemsToUpdate = new ArrayList<>();
		for (SupplierMetadata loginid : supplierMetadataList) {
			fillAttributes(loginid, savedList.get(loginid.getId()));
			fillCommonAttributes(loginid);
			if (loginid.getId() == null)
				loginid.setId(new IdGenerator(loginid.getClass().getSimpleName()).getId(loginid));

			if (savedList.get(loginid.getId()) == null) {
				// New record -> assign ID
				loginid.setId(new IdGenerator(loginid.getClass().getSimpleName()).getId(loginid));
				itemsToInsert.add(loginid);
				loginid.setOperationPerformed(ActionType.INSERT);
				loginid.setVersion(0);
			} else {
				// Existing record -> do NOT change ID
				com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata existingUser = savedList.get(loginid.getId());
				loginid.setId(existingUser.getId()); // Preserve existing ID
				loginid.setOperationPerformed(ActionType.UPDATE);
				loginid.setChanged(Boolean.TRUE);
				loginid.setVersion(existingUser.getVersion() + 1);
				itemsToUpdate.add(loginid);
			}
		}
		result.add(itemsToInsert);
		result.add(itemsToUpdate);
		return result;
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
	public Collection<SupplierMetadata> batchSave(Collection<SupplierMetadata> supplierMetadataList) {
//		LOG.info("Size of list is {}", supplierMetadataList.size());

		try {
			List<List<SupplierMetadata>> saveItemsList = getItemsToSaveList(new ArrayList<>(supplierMetadataList));

			if (!saveItemsList.get(0).isEmpty()) {
			//	LOG.info("Performing batch insert for {} records", saveItemsList.get(0).size());

				List<CkSupplierMetadataRecord> insertRecords = saveItemsList.get(0).stream().map(supplierMetadata -> {
					CkSupplierMetadataRecord record = getDslContext().newRecord(CK_SUPPLIER_METADATA, supplierMetadata);
					return record;
				}).collect(Collectors.toList());

				getDslContext().batchInsert(insertRecords).execute();
			}

			if (!saveItemsList.get(1).isEmpty()) {

				List<CkSupplierMetadataRecord> updateRecords = saveItemsList.get(1).stream().map(supplierMetadata -> {
					CkSupplierMetadataRecord record = getDslContext().newRecord(CK_SUPPLIER_METADATA, supplierMetadata);


					return record;
				}).collect(Collectors.toList());

				getDslContext().batchUpdate(updateRecords).execute();
			}

		} catch (Exception e) {

		}
		return supplierMetadataList;
	}
//	@Override
//	public List<SupplierMetadata> batchSave(Collection<SupplierMetadata> supplierMetaDataCollection) {
//		List<SupplierMetadata> supplierMetaDataList = new ArrayList<>(supplierMetaDataCollection);
//
//		if (supplierMetaDataList.isEmpty()) {
//			return supplierMetaDataList;
//		}
//		// All items are new since we've already filtered for differences
//		getDslContext().batchInsert(supplierMetaDataList.stream().map(supplier -> {
////			supplier.setId(new IdGenerator(supplier.getClass().getSimpleName()).getId(supplier));
//			supplier.setChanged(true);
//			CkSupplierMetadataRecord record = getDslContext().newRecord(CK_SUPPLIER_METADATA, supplier);
//			return record;
//		}).collect(Collectors.toList())).execute();
//
//
//		return supplierMetaDataList;
//	}

}
