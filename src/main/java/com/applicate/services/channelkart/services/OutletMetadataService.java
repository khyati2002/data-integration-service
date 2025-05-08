package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.jooq.generated.tables.pojos.OutletMetadata;
import com.salescode.dim.jooq.generated.tables.records.CkOutletMetadataRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_OUTLET_METADATA;

public class OutletMetadataService extends AbstractCDMService<OutletMetadata> {
	private static final Logger LOG = LoggerFactory.getLogger(OutletMetadataService.class);

	public List<List<OutletMetadata>> getItemsToSaveList(List<OutletMetadata> outletMetadataList) {
		List<List<OutletMetadata>> result = new ArrayList<>();
		List<String> outletCodes = outletMetadataList.stream().map(OutletMetadata::getId).collect(Collectors.toList());

		Map<String, OutletMetadata> savedList = getDslContext().selectFrom(CK_OUTLET_METADATA).where(CK_OUTLET_METADATA.ID.in(outletCodes)).fetch().intoMap(CK_OUTLET_METADATA.ID, record -> record.into(OutletMetadata.class));
		List<OutletMetadata> itemsToInsert = new ArrayList<>();
		List<OutletMetadata> itemsToUpdate = new ArrayList<>();
		for (OutletMetadata outlet : outletMetadataList) {
			fillAttributes(outlet, savedList.get(outlet.getId()));
			fillCommonAttributes(outlet);

			if (savedList.get(outlet.getId()) == null) {
				outlet.setVersion(0);
				itemsToInsert.add(outlet);
				outlet.setOperationPerformed(ActionType.INSERT);
			} else {
				OutletMetadata existingOutlet = savedList.get(outlet.getId());
				outlet.setVersion(existingOutlet.getVersion() + 1);


				outlet.setOperationPerformed(ActionType.UPDATE);
				itemsToUpdate.add(outlet);
			}
		}

		result.add(itemsToInsert);
		result.add(itemsToUpdate);
		return result;
	}

	@Override
	public Collection<OutletMetadata> batchSave(Collection<OutletMetadata> outletMetadataList) {
		LOG.info("Size of list is " + outletMetadataList.size());
		List<OutletMetadata> outletMetadata = new ArrayList<>(outletMetadataList);
		List<List<OutletMetadata>> saveItemsList = getItemsToSaveList(outletMetadata);
		saveItemsList.get(0).forEach(outlet -> {
			outlet.setActiveStatus(ActiveStatus.ACTIVE);
			outlet.setChanged((byte) 1);
		});

		saveItemsList.get(1).forEach(outlet -> {
			outlet.setActiveStatus(ActiveStatus.ACTIVE);
			outlet.setChanged((byte) 1);
		});
		if (!saveItemsList.get(0).isEmpty()) {
			getDslContext().batchInsert(saveItemsList.get(0).stream().map(outlet -> getDslContext().newRecord(CK_OUTLET_METADATA, outlet)).collect(Collectors.toList())).execute();
		}
		if (!saveItemsList.get(1).isEmpty()) {
			getDslContext().batchUpdate(saveItemsList.get(1).stream().map(outlet -> {
				CkOutletMetadataRecord record = getDslContext().newRecord(CK_OUTLET_METADATA, outlet);
				return record;
			}).collect(Collectors.toList())).execute();
		}

		LOG.info("Batch save successful");
		return outletMetadata;
	}
}
