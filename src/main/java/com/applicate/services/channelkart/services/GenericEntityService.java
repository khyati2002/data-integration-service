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



	public List<GenericEntity> findByNameAndKey1(String name,String key1){
		return getDslContext().selectFrom(CK_GENERIC_OBJECT).where(CK_GENERIC_OBJECT.KEY1.eq(key1)).and(CK_GENERIC_OBJECT.NAME.eq(name)).fetchInto(GenericEntity.class);
	}

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
				loginId.setChanged((byte) 1);
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
		entity.setChanged((byte) 1);
		entity.setActiveStatus(record.getActiveStatus());


		return entity;
	}


	@Override
	public Collection<GenericEntity> batchSave(Collection<GenericEntity> genericEntityList) {
		LOG.info("Size of list is " + genericEntityList.size());
		List<GenericEntity> genericEntity = new ArrayList<>(genericEntityList);
		List<List<GenericEntity>> saveItemsList = getItemsToSaveList(genericEntity);
		saveItemsList.get(0).forEach(loginId -> {
			loginId.setActiveStatus(ActiveStatus.ACTIVE);
			loginId.setRangeKey(0L);
			loginId.setTimestamp(new Date().toInstant().toEpochMilli());
			loginId.setChanged((byte) 1);
		});

		saveItemsList.get(1).forEach(loginId -> {
			loginId.setActiveStatus(ActiveStatus.ACTIVE);
			loginId.setRangeKey(0L);
			loginId.setTimestamp(new Date().toInstant().toEpochMilli());
			loginId.setChanged((byte) 1);

		});
		if (!saveItemsList.get(0).isEmpty()) {
			getDslContext().batchInsert(saveItemsList.get(0).stream().map(loginId -> getDslContext().newRecord(CK_GENERIC_OBJECT, loginId)).collect(Collectors.toList())).execute();
		}
		if (!saveItemsList.get(1).isEmpty()) {
			getDslContext().batchUpdate(saveItemsList.get(1).stream().map(loginId -> {
				CkGenericObjectRecord record = getDslContext().newRecord(CK_GENERIC_OBJECT, loginId);
				return record;
			}).collect(Collectors.toList())).execute();
		}

		LOG.info("Batch save successful");
		return genericEntity;
	}

}