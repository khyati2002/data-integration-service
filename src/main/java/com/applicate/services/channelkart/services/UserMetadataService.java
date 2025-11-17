package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.utils.IdGenerator;
import com.salescode.dim.jooq.generated.tables.records.CkUserMetadataRecord;
import com.salescode.dim.jooq.impl.UserMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.tables.CkUserMetadata.CK_USER_METADATA;
import static org.jooq.impl.DSL.*;

public class UserMetadataService extends AbstractCDMService<UserMetadata> {
	private static final Logger LOG = LoggerFactory.getLogger(UserMetadataService.class);

	public List<List<UserMetadata>> getItemsToSaveList(List<UserMetadata> userMetadataList) {
		List<List<UserMetadata>> result = new ArrayList<>();
		List<String> users = userMetadataList.stream().map(UserMetadata::getLoginid).collect(Collectors.toList());

		Map<String, com.salescode.dim.jooq.generated.tables.pojos.UserMetadata> savedList = getDslContext().selectFrom(CK_USER_METADATA).where(CK_USER_METADATA.LOGINID.in(users)).fetch().intoMap(CK_USER_METADATA.LOGINID, record -> record.into(com.salescode.dim.jooq.generated.tables.pojos.UserMetadata.class));

		List<UserMetadata> itemsToInsert = new ArrayList<>();
		List<UserMetadata> itemsToUpdate = new ArrayList<>();
		for (UserMetadata loginid : userMetadataList) {
			fillAttributes(loginid, savedList.get(loginid.getId()));
			fillCommonAttributes(loginid);
			loginid.setId(new IdGenerator(loginid.getClass().getSimpleName()).getId(loginid));

			if (loginid.getId() == null)
				loginid.setId(new IdGenerator(loginid.getClass().getSimpleName()).getId(loginid));

			if (savedList.get(loginid.getLoginid()) == null) {
				// New record -> assign ID
				loginid.setId(new IdGenerator(loginid.getClass().getSimpleName()).getId(loginid));
				itemsToInsert.add(loginid);
				loginid.setOperationPerformed(ActionType.INSERT);
				loginid.setVersion(0);
			} else {
				// Existing record -> do NOT change ID
				com.salescode.dim.jooq.generated.tables.pojos.UserMetadata existingUser = savedList.get(loginid.getLoginid());
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

	@Override
	public Collection<UserMetadata> batchSave(Collection<UserMetadata> userMetadataList) {
		LOG.info("Size of list is {}", userMetadataList.size());

		try {
			List<List<UserMetadata>> saveItemsList = getItemsToSaveList(new ArrayList<>(userMetadataList));

			if (!saveItemsList.get(0).isEmpty()) {
				LOG.info("Performing batch insert for {} records", saveItemsList.get(0).size());

				List<CkUserMetadataRecord> insertRecords = saveItemsList.get(0).stream().map(userMetadata -> {
					CkUserMetadataRecord record = getDslContext().newRecord(CK_USER_METADATA, userMetadata);

					// Handle location field with SRID 4326
					if (userMetadata.getLatitude() != null && userMetadata.getLongitude() != null) {
						Object pointValue = getDslContext().select(field("ST_GeomFromText({0}, 4326)", String.format("POINT(%s %s)", userMetadata.getLongitude(), userMetadata.getLatitude()))).fetchOne(0);
						record.set(CK_USER_METADATA.LOCATION, pointValue);
					}

					return record;
				}).collect(Collectors.toList());

				getDslContext().batchInsert(insertRecords).execute();
			}

			if (!saveItemsList.get(1).isEmpty()) {
				LOG.info("Performing batch update for {} records", saveItemsList.get(1).size());

				List<CkUserMetadataRecord> updateRecords = saveItemsList.get(1).stream().map(userMetadata -> {
					CkUserMetadataRecord record = getDslContext().newRecord(CK_USER_METADATA, userMetadata);

					// Handle location field with SRID 4326
					if (userMetadata.getLatitude() != null && userMetadata.getLongitude() != null) {
						Object pointValue = getDslContext().select(field("ST_GeomFromText({0}, 4326)", String.format("POINT(%s %s)", userMetadata.getLongitude(), userMetadata.getLatitude()))).fetchOne(0);
						record.set(CK_USER_METADATA.LOCATION, pointValue);
					}

					return record;
				}).collect(Collectors.toList());

				getDslContext().batchUpdate(updateRecords).execute();
			}

			LOG.info("Batch save completed successfully for {} records", userMetadataList.size());

		} catch (Exception e) {
			LOG.error("Error occurred during batch save of UserMetadata. Message: {}", e.getMessage(), e);
		}

		return userMetadataList;
	}
}


