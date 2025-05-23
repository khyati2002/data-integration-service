package com.applicate.services.channelkart.services;


import com.applicate.services.channelkart.enrichments.EnrichmentPhase;
import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.utils.CdmDiffUtil;
import com.applicate.services.channelkart.utils.IdGenerator;
import com.salescode.dim.cache.CacheManager;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dim.etl.enrichment.service.EnrichmentInfoRegistry;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.jooq.generated.tables.records.CkRecommendedOrderRecord;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.RecommendedOrder;
import com.salescode.dim.scanner.ExternalRegistryScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.*;


public class RecommendedOrderService extends AbstractCDMService<RecommendedOrder>{

	private OutletDetailsService outletDetailsService;
	private final DataEnrichmentService dataEnrichmentService;
	private final EnrichmentInfoRegistry enrichmentInfoRegistry;
	private ETLRegistry etlRegistry;


	private static final Logger logger = LoggerFactory.getLogger(RecommendedOrderService.class);

	public RecommendedOrderService() {
		ExternalRegistryScanner externalRegistryScanner = ExternalRegistryScanner.getInstance();
		etlRegistry = ETLRegistry.getInstance(externalRegistryScanner);
		outletDetailsService= new OutletDetailsService();;
		enrichmentInfoRegistry = new EnrichmentInfoRegistry(getDslContext());
		dataEnrichmentService = new DataEnrichmentService(enrichmentInfoRegistry, etlRegistry);
	}

	@Override
	public RecommendedOrder save(RecommendedOrder recommendedOrder) {
		if(recommendedOrder.getChannel() !=null) {
			return super.save(recommendedOrder);
		}
		if(recommendedOrder.getOutletcode() != null) {
			String outletCode = recommendedOrder.getOutletcode();
			OutletDetails outletDetails = outletDetailsService.findByOutletCode(outletCode);
			if(outletDetails == null) {
				throw new IllegalArgumentException("No outlet found with outletCode " + outletCode);
			}
		}
		return super.save(recommendedOrder);
	}



	public List<RecommendedOrder> batchSave(List<RecommendedOrder> recommendedOrders) {
		logger.info("Saving {} records",recommendedOrders.size());
		List<RecommendedOrder> entryList = preBatchSave(recommendedOrders);
		List<List<RecommendedOrder>> saveItemsList = getItemsToSaveList(entryList);
		if (!saveItemsList.get(0).isEmpty()) {
			getDslContext().batchInsert(
					saveItemsList.get(0).stream()
							.map(target -> getDslContext().newRecord(CK_RECOMMENDED_ORDER, target)) // Convert to jOOQ Records
							.collect(Collectors.toList())).execute();
		}
		if (!saveItemsList.get(1).isEmpty()) {
			getDslContext().batchUpdate(
					saveItemsList.get(1).stream()
							.map(target -> {
								CkRecommendedOrderRecord targetsRecord = getDslContext().newRecord(CK_RECOMMENDED_ORDER, target);
								targetsRecord.changed(CK_RECOMMENDED_ORDER.ID, false); // Avoid updating primary key
								return targetsRecord;
							})
							.collect(Collectors.toList())
			).execute();
		}
		CacheManager.getInstance().evictAll("dataintegration-user");
		if (!saveItemsList.get(0).isEmpty() || !saveItemsList.get(1).isEmpty()) {
			postBatchSave(entryList);
		}

		return entryList;
	}

	public List<RecommendedOrder> preBatchSave(Collection<RecommendedOrder> recommendedOrder) {
		List<RecommendedOrder>  recommendedOrderList= new ArrayList<>();
		recommendedOrder.forEach(order -> {
			if (order.getId() == null) {
				String id = new IdGenerator(order.getClass().getSimpleName()).getId(order);
				order.setId(id);
			}
			recommendedOrderList.add(order);
		});

		return new ArrayList<>(recommendedOrder);
	}
	public void postBatchSave(List<RecommendedOrder> entityList) {
		// postBatchSave
	}

	private List<List<RecommendedOrder>> getItemsToSaveList(List<RecommendedOrder> entityList) {

		List<List<RecommendedOrder>> result = new ArrayList<>();

		List<String> ids = entityList.stream()
				.map(RecommendedOrder::getId)
				.collect(Collectors.toList());

		Map<String, RecommendedOrder> savedList = getDslContext().selectFrom(CK_RECOMMENDED_ORDER)
				.where(CK_RECOMMENDED_ORDER.ID.in(ids))
				.fetch()
				.intoMap(CK_RECOMMENDED_ORDER.ID, recordEntry -> recordEntry.into(RecommendedOrder.class));

		List<RecommendedOrder> itemsToInsert = new ArrayList<>();
		List<RecommendedOrder> itemsToUpdate = new ArrayList<>();

		for (RecommendedOrder entry : entityList) {
			fillAttributes(entry,   RecommendedOrder.of(savedList.get(entry.getId())));
			fillCommonAttributes(entry);
			new AttributeUpdateOverrideManager().overrideAttributes(entry, savedList.get(entry.getId()));
			super.addHash(entry);
			preSaveEnrichment(entry);
			if (savedList.get(entry.getId()) == null) {
				entry.setVersion(0);
				entry.setOperationPerformed(ActionType.INSERT);
				itemsToInsert.add(entry);

			} else {
				RecommendedOrder savedEntry =  RecommendedOrder.of(savedList.get(entry.getId()));
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

	private void preSaveEnrichment(RecommendedOrder targets) {
		OperationResult or = dataEnrichmentService.enrich(targets, EnrichmentPhase.PRE_SAVE);
		if (!or.getStatus().equals(OperationResult.Status.OK)) {
			throw new RuntimeException("Pre save enrichment error");
		}
	}

}
