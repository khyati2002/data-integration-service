package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.utils.IdGenerator;
import com.salescode.dim.jooq.generated.tables.records.CkGenericObjectRecord;
import com.salescode.dim.jooq.impl.GenericEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_GENERIC_OBJECT;

public class GenericEntityService extends AbstractCDMService<GenericEntity> {
	private static final Logger LOG = LoggerFactory.getLogger(GenericEntityService.class);

	public List<List<GenericEntity>> getItemsToSaveList(List<GenericEntity> genericEntityList) {
		List<List<GenericEntity>> result = new ArrayList<>();
		List<String> outletCodes = genericEntityList.stream().map(GenericEntity::getId).collect(Collectors.toList());

		Map<String, GenericEntity> savedList = getDslContext().selectFrom(CK_GENERIC_OBJECT).where(CK_GENERIC_OBJECT.ID.in(outletCodes)).fetch().intoMap(CK_GENERIC_OBJECT.ID, record -> convertToGenericEntity(record));

		List<GenericEntity> itemsToInsert = new ArrayList<>();
		List<GenericEntity> itemsToUpdate = new ArrayList<>();
		for (GenericEntity loginId : genericEntityList) {
			fillAttributes(loginId, savedList.get(loginId.getId()));
			fillCommonAttributes(loginId);
			if (loginId.getId() == null)
				loginId.setId(new IdGenerator(loginId.getClass().getSimpleName()).getId(loginId));

			if (savedList.get(loginId.getId()) == null) {
				itemsToInsert.add(loginId);
				loginId.setRangeKey(0L);
				loginId.setTimestamp(new Date().toInstant().toEpochMilli());
				loginId.setOperationPerformed(ActionType.INSERT);
			} else {
				GenericEntity existingOutlet = savedList.get(loginId.getId());
				loginId.setOperationPerformed(ActionType.UPDATE);
				loginId.setRangeKey(0L);
				loginId.setChanged(1==1);
				loginId.setTimestamp(new Date().toInstant().toEpochMilli());
				itemsToUpdate.add(loginId);
			}
		}
		result.add(itemsToInsert);
		result.add(itemsToUpdate);
		return result;
	}

	private GenericEntity convertToGenericEntity(CkGenericObjectRecord record) {
		GenericEntity entity = new GenericEntity();
		entity.setId(record.getId());
		entity.setRangeKey(record.getRangeKey());
		entity.setTimestamp(record.getTimestamp());
		entity.setChanged(1==1);
		entity.setActiveStatus(record.getActiveStatus());


		return entity;
	}

	@Override
	public Collection<GenericEntity> batchSave(Collection<GenericEntity> genericEntityList) {

		LOG.info("Entering batchSave() with list size = {}",
				genericEntityList != null ? genericEntityList.size() : null);

		List<GenericEntity> genericEntity = new ArrayList<>(genericEntityList);

		LOG.info("Converting collection to list…");

		List<List<GenericEntity>> saveItemsList = getItemsToSaveList(genericEntity);

		LOG.info("Items split into: insertList size = {}, updateList size = {}",
				saveItemsList.get(0).size(), saveItemsList.get(1).size());

		// Add logs for each record before mutation
		LOG.info("Sample insert item before mutation: {}",
				saveItemsList.get(0).isEmpty() ? "NONE" : saveItemsList.get(0).get(0));

		LOG.info("Sample update item before mutation: {}",
				saveItemsList.get(1).isEmpty() ? "NONE" : saveItemsList.get(1).get(0));

		long now = new Date().toInstant().toEpochMilli();

		saveItemsList.get(0).forEach(loginId -> {
			loginId.setActiveStatus(ActiveStatus.ACTIVE);
			loginId.setRangeKey(0L);
			loginId.setTimestamp(now);
			loginId.setChanged(true);
		});

		LOG.info("Insert list mutated. First element after mutation: {}",
				saveItemsList.get(0).isEmpty() ? "NONE" : saveItemsList.get(0).get(0));

		saveItemsList.get(1).forEach(loginId -> {
			loginId.setActiveStatus(ActiveStatus.ACTIVE);
			loginId.setRangeKey(0L);
			loginId.setTimestamp(now);
			loginId.setChanged(true);
		});

		LOG.info("Update list mutated. First element after mutation: {}",
				saveItemsList.get(1).isEmpty() ? "NONE" : saveItemsList.get(1).get(0));

		// -------------------------
		// INSERT BLOCK
		// -------------------------
		if (!saveItemsList.get(0).isEmpty()) {
			LOG.info("Starting batchInsert with {} items", saveItemsList.get(0).size());
			try {
				getDslContext().batchInsert(
						saveItemsList.get(0)
								.stream()
								.map(loginId -> getDslContext().newRecord(CK_GENERIC_OBJECT, loginId))
								.collect(Collectors.toList())
				).execute();
				LOG.info("Batch insert SUCCESS");
			} catch (Exception e) {
				LOG.error("Batch insert FAILED. Error: {}", e.getMessage(), e);
				// Log all insert items for debugging
				saveItemsList.get(0).forEach(item -> LOG.error("Insert item: {}", item));
				throw e;
			}
		}

		// -------------------------
		// UPDATE BLOCK
		// -------------------------
		if (!saveItemsList.get(1).isEmpty()) {
			LOG.info("Starting batchUpdate with {} items", saveItemsList.get(1).size());
			try {
				getDslContext().batchUpdate(
						saveItemsList.get(1)
								.stream()
								.map(loginId -> getDslContext().newRecord(CK_GENERIC_OBJECT, loginId))
								.collect(Collectors.toList())
				).execute();
				LOG.info("Batch update SUCCESS");
			} catch (Exception e) {
				LOG.error("Batch update FAILED. Error: {}", e.getMessage(), e);
				// Log update items fully
				saveItemsList.get(1).forEach(item -> LOG.error("Update item: {}", item));
				throw e;
			}
		}

		LOG.info("Batch save completed successfully");
		return genericEntity;
	}


}