package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.utils.IdGenerator;
import com.salescode.dim.jooq.impl.UserMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.tables.CkUserMetadata.CK_USER_METADATA;

public class UserMetadataService extends AbstractCDMService<UserMetadata>{
	private static final Logger LOG = LoggerFactory.getLogger(UserMetadataService.class);
	public List<List<UserMetadata>> getItemsToSaveList(List<UserMetadata> userMetadataList) {
		List<List<UserMetadata>> result = new ArrayList<>();
		List<String> users = userMetadataList.stream().map(UserMetadata::getId).collect(Collectors.toList());

		Map<String, com.salescode.dim.jooq.generated.tables.pojos.UserMetadata> savedList = getDslContext().selectFrom(CK_USER_METADATA).where(CK_USER_METADATA.ID.in(users)).fetch().intoMap(CK_USER_METADATA.ID, record -> record.into(com.salescode.dim.jooq.generated.tables.pojos.UserMetadata.class));

		List<UserMetadata> itemsToInsert = new ArrayList<>();
		List<UserMetadata> itemsToUpdate = new ArrayList<>();
		for (UserMetadata loginId : userMetadataList) {
			fillAttributes(loginId, savedList.get(loginId.getId()));
			fillCommonAttributes(loginId);
			if (loginId.getId() == null)
				loginId.setId(new IdGenerator(loginId.getClass().getSimpleName()).getId(loginId));

			if (savedList.get(loginId.getId()) == null) {
				itemsToInsert.add(loginId);
				loginId.setOperationPerformed(ActionType.INSERT);
			} else {
				com.salescode.dim.jooq.generated.tables.pojos.UserMetadata existingOutlet = savedList.get(loginId.getId());
				loginId.setOperationPerformed(ActionType.UPDATE);
				loginId.setChanged(Boolean.TRUE);
				itemsToUpdate.add(loginId);
			}
		}
		result.add(itemsToInsert);
		result.add(itemsToUpdate);
		return result;
	}
	@Override
	public Collection<UserMetadata> batchSave(Collection<UserMetadata> userMetadataList) {
		LOG.info("Size of list is {}", userMetadataList.size());

		List<List<UserMetadata>> saveItemsList = getItemsToSaveList(new ArrayList<>(userMetadataList));

		if (!saveItemsList.get(0).isEmpty()) {
			getDslContext().batchInsert(saveItemsList.get(0).stream().map(outlet -> getDslContext().newRecord(CK_USER_METADATA, outlet)).collect(Collectors.toList())).execute();
		}

		if (!saveItemsList.get(1).isEmpty()) {
			getDslContext().batchUpdate(saveItemsList.get(1).stream().map(outlet -> getDslContext().newRecord(CK_USER_METADATA, outlet)).collect(Collectors.toList())).execute();
		}

		LOG.info("Batch save is successful");
		return userMetadataList;
	}
}

