package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.enrichments.EnrichmentPhase;
import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.utils.CdmDiffUtil;
import com.applicate.services.channelkart.utils.IdGenerator;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.cache.CacheManager;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dim.etl.enrichment.service.EnrichmentInfoRegistry;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.jooq.generated.tables.records.CkOutletActivityRecord;
import com.salescode.dim.jooq.impl.OutletActivity;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.scanner.ExternalRegistryScanner;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_OUTLET_ACTIVITY;

public class OutletActivityService extends AbstractCDMService<OutletActivity> {
	private final DataEnrichmentService dataEnrichmentService;
	private final EnrichmentInfoRegistry enrichmentInfoRegistry;
	private OutletDetailsService outletDetailsService;
	private ETLRegistry etlRegistry;

	public OutletActivityService() {
		ExternalRegistryScanner externalRegistryScanner = ExternalRegistryScanner.getInstance();
		etlRegistry = ETLRegistry.getInstance(externalRegistryScanner);
		outletDetailsService = new OutletDetailsService();
		enrichmentInfoRegistry = new EnrichmentInfoRegistry(getDslContext());
		dataEnrichmentService = new DataEnrichmentService(enrichmentInfoRegistry, etlRegistry);
	}

	@Override
	public Collection<OutletActivity> batchSave(Collection<OutletActivity> outletActivity) {
		List<OutletActivity> entityList = preBatchSave(outletActivity);
		List<List<OutletActivity>> saveItemsList = getItemsToSaveList(entityList);
		if (!saveItemsList.get(0).isEmpty()) {
			getDslContext().batchInsert(saveItemsList.get(0).stream().map(target -> getDslContext().newRecord(CK_OUTLET_ACTIVITY, target)) // Convert to jOOQ Records
					.collect(Collectors.toList())).execute();
		}
		if (!saveItemsList.get(1).isEmpty()) {
			getDslContext().batchUpdate(saveItemsList.get(1).stream().map(target -> {
				CkOutletActivityRecord targetsRecord = getDslContext().newRecord(CK_OUTLET_ACTIVITY, target);
				targetsRecord.changed(CK_OUTLET_ACTIVITY.ID, false); // Avoid updating primary key
				return targetsRecord;
			}).collect(Collectors.toList())).execute();
		}
		CacheManager.getInstance().evictAll("dataintegration-user");
		if (!saveItemsList.get(0).isEmpty() || !saveItemsList.get(1).isEmpty()) {
			postBatchSave(entityList);
		}
		return entityList;
	}

	public List<OutletActivity> preBatchSave(Collection<OutletActivity> outletActivity) {
		List<OutletActivity> outletActivityList = new ArrayList<>();
		outletActivity.forEach(activity -> {
			if (activity.getId() == null) {
				String id = new IdGenerator(activity.getClass().getSimpleName()).getId(activity);
				activity.setId(id);
			}
			if (StringUtils.isNotEmpty(activity.getOutletcode())) {
				OutletDetails activityOutlet = outletDetailsService.findByOutletCode(activity.getOutletcode());
				if (activity.getHierarchy() == null) {
					activity.setHierarchy(activityOutlet.getHierarchy());
				}
				activity.setLocationHierarchy(getLocationHierarchyIfExists(activityOutlet));
				activity.setOutletName(activityOutlet.getOutletName());
				outletActivityList.add(activity);
			}
		});
		return outletActivityList;
	}

	public void postBatchSave(List<OutletActivity> entityList) {
		// postBatchSave
	}

	private List<List<OutletActivity>> getItemsToSaveList(List<OutletActivity> entityList) {
		List<List<OutletActivity>> result = new ArrayList<>();
		List<String> ids = entityList.stream().map(OutletActivity::getId).collect(Collectors.toList());
		Map<String, OutletActivity> savedList = getDslContext().selectFrom(CK_OUTLET_ACTIVITY).where(CK_OUTLET_ACTIVITY.ID.in(ids)).fetch().intoMap(CK_OUTLET_ACTIVITY.ID, recordEntry -> recordEntry.into(OutletActivity.class));
		List<OutletActivity> itemsToInsert = new ArrayList<>();
		List<OutletActivity> itemsToUpdate = new ArrayList<>();
		for (OutletActivity entry : entityList) {
			fillAttributes(entry, OutletActivity.of(savedList.get(entry.getId())));
			fillCommonAttributes(entry);
			new AttributeUpdateOverrideManager().overrideAttributes(entry, savedList.get(entry.getId()));
			preSaveEnrichment(entry);
			if (savedList.get(entry.getId()) == null) {
				entry.setVersion(0);
				entry.setChanged((byte)1);
				entry.setActiveStatus(ActiveStatus.ACTIVE);
				entry.setSystemTime(LocalDateTime.now(ZoneOffset.UTC));
				entry.setSubmissionTime(LocalDateTime.now(ZoneOffset.UTC));
				entry.setOperationPerformed(ActionType.INSERT);
				itemsToInsert.add(entry);
			} else {
				OutletActivity savedEntry = OutletActivity.of(savedList.get(entry.getId()));
				entry.setVersion(savedList.get(entry.getId()).getVersion() + 1);
				entry.setChanges(CdmDiffUtil.getChanges(entry, savedEntry));
				entry.setOperationPerformed(ActionType.UPDATE);
				itemsToUpdate.add(entry);
			}
		}
		result.add(itemsToInsert);
		result.add(itemsToUpdate);
		return result;
	}

	private void preSaveEnrichment(OutletActivity targets) {
		OperationResult or = dataEnrichmentService.enrich(targets, EnrichmentPhase.PRE_SAVE);
		if (!or.getStatus().equals(OperationResult.Status.OK)) {
			throw new RuntimeException("Pre save enrichment error");
		}
	}

	public String getLocationHierarchyIfExists(OutletDetails outlet) {
		return outlet.getLocationHierarchy() == null ? null : outlet.getLocationHierarchy();
	}
}
