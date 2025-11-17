package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.utils.CdmDiffUtil;
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
		supplierMetaDataList.forEach(supplierMetadata -> supplierMetadata.setId(userLoginId.toLowerCase()));

		//	supplierMetaDataList.forEach((supplierMetadata -> supplierMetadata.setId(userLoginId)));
		List<List<SupplierMetadata>> saveItemsList = getItemsToSaveList(supplierMetaDataList);

		if (!saveItemsList.get(0).isEmpty()) {
			getDslContext().batchInsert(saveItemsList.get(0).stream().map(outlet -> getDslContext().newRecord(CK_SUPPLIER_METADATA, outlet)).collect(Collectors.toList())).execute();
		}
		if (!saveItemsList.get(1).isEmpty()) {
			getDslContext().batchUpdate(saveItemsList.get(1).stream().map(outlet -> {
				CkSupplierMetadataRecord record = getDslContext().newRecord(CK_SUPPLIER_METADATA, outlet);
				//                          record.changed(CK_USER.ID, false); // Avoid updating primary key
				return record;
			}).collect(Collectors.toList())).execute();
		}
		System.out.println("Save successful");
	}

	public List<List<SupplierMetadata>> getItemsToSaveList(List<SupplierMetadata> supplierMetadataList) {
		List<List<SupplierMetadata>> result = new ArrayList<>();
		List<String> outletCodes = supplierMetadataList.stream().map(SupplierMetadata::getId).collect(Collectors.toList());

		Map<String, com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata> savedList = getDslContext().selectFrom(CK_SUPPLIER_METADATA).where(CK_SUPPLIER_METADATA.ID.in(outletCodes)).fetch().intoMap(CK_SUPPLIER_METADATA.ID, record -> record.into(com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata.class));
		List<SupplierMetadata> itemsToInsert = new ArrayList<>();
		List<SupplierMetadata> itemsToUpdate = new ArrayList<>();
		for (SupplierMetadata outlet : supplierMetadataList) {
			fillCommonAttributes(outlet);
			if (savedList.get(outlet.getId()) == null) {
				outlet.setVersion(0);
				outlet.setChanged((byte) 1);
				itemsToInsert.add(outlet);
				outlet.setOperationPerformed(ActionType.INSERT);
			} else {
				SupplierMetadata existingOutlet = SupplierMetadata.of(savedList.get(outlet.getId()));
				outlet.setId(existingOutlet.getId());
				outlet.setVersion(existingOutlet.getVersion() + 1);
				outlet.setOperationPerformed(ActionType.UPDATE);
				outlet.setChanged((byte) 1);
				itemsToUpdate.add(outlet);

				String outlethash = outlet.getHash();
				String existingHash = existingOutlet.getHash();

				if (!Objects.equals(outlet.getHash(), existingOutlet.getHash())) {
					outlet.setChanges(CdmDiffUtil.getChanges(outlet, existingOutlet));
					outlet.setOperationPerformed(ActionType.UPDATE);
					outlet.setChanged((byte) 1);
					itemsToUpdate.add(outlet);
				}
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


}
