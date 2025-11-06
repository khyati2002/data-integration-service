package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.utils.BatchInsertUtil;
import com.applicate.services.channelkart.utils.CdmDiffUtil;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.DataStreamJob;
import com.salescode.dim.PreProcessOperationResult;
import com.salescode.dim.PreProcessPipelineService;
import com.salescode.dim.cache.CacheManager;
import com.salescode.dim.cache.Cacheable;
import com.salescode.dim.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dim.etl.enrichment.service.EnrichmentInfoRegistry;
import com.salescode.dim.jooq.generated.tables.records.CkSalesRecord;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.etl.validation.service.DataValidationService;
import com.salescode.dim.etl.validation.service.ValidationExcludeGroupRegistry;
import com.salescode.dim.etl.validation.service.ValidationInfoRegistry;
import com.salescode.dim.jooq.impl.Sales;
import com.salescode.dim.jooq.impl.SalesDetails;
import com.salescode.dim.jooq.impl.SalesHistory;
import com.salescode.dim.scanner.ExternalRegistryScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.*;

public class SalesService extends AbstractCDMService<Sales> {
    private static final Logger LOG = LoggerFactory.getLogger(SalesService.class);

    private final SalesDetailsService salesDetailsService;
    private final SalesHistoryService salesHistoryService;
    private final UserService userService;
    private final OutletDetailsService outletDetailsService;
    private final DataValidationService dataValidationService;
    private final DataEnrichmentService dataEnrichmentService;
    private final PreProcessPipelineService preProcessPipelineService;
    private final ValidationInfoRegistry validationInfoRegistry;
    private final ValidationExcludeGroupRegistry validationExcludeGroupRegistry;
    private final EnrichmentInfoRegistry enrichmentInfoRegistry;
    private ETLRegistry etlRegistry;

    public SalesService() {
        ExternalRegistryScanner externalRegistryScanner = ExternalRegistryScanner.getInstance();
        etlRegistry = ETLRegistry.getInstance(externalRegistryScanner);
        userService = new UserService();
        outletDetailsService = new OutletDetailsService();
        salesDetailsService = new SalesDetailsService();
        salesHistoryService = new SalesHistoryService();
        validationInfoRegistry = new ValidationInfoRegistry(getDslContext());
        validationExcludeGroupRegistry = new ValidationExcludeGroupRegistry(getDslContext());
        enrichmentInfoRegistry = new EnrichmentInfoRegistry(getDslContext());
        dataValidationService = new DataValidationService(validationInfoRegistry, validationExcludeGroupRegistry, etlRegistry);
        dataEnrichmentService = new DataEnrichmentService(enrichmentInfoRegistry, etlRegistry);
        preProcessPipelineService = new PreProcessPipelineService(dataValidationService, dataEnrichmentService);
    }

    @Cacheable(cacheName = "dataintegration-sales")
    public Sales findByInvoiceNumber(String invoiceNumber) {
        // Assuming you have a CK_SALES table in your jooq generated tables
       Sales sales = getDslContext()
                .select(CK_SALES.asterisk())
                .from(CK_SALES)
                .where(CK_SALES.INVOICE_NUMBER.eq(invoiceNumber))
                .fetchOneInto(Sales.class);
        return sales;
    }

    private List<SalesDetails> preProcessSalesDetails(List<SalesDetails> salesDetailsList) {
         salesDetailsList.parallelStream().forEach(detail -> {
             preProcessPipelineService.preProcessPipeline(detail, null);
         });
        return salesDetailsList;
    }

    private List<SalesHistory> preProcessSalesHistory(List<SalesHistory> salesHistoryList) {
         salesHistoryList.parallelStream().forEach(history -> {
             preProcessPipelineService.preProcessPipeline(history, null);
         });
        return salesHistoryList;
    }

    private ConcurrentHashMap<String, List<SalesDetails>> populateSalesDetails(List<Sales> salesList) {
        List<SalesDetails> allSalesDetails = salesList.stream()
                .filter(sales -> sales.getSalesDetails() != null && !sales.getSalesDetails().isEmpty())
                .flatMap(sales -> {
                    sales.getSalesDetails().forEach(detail -> {
                        detail.setInvoiceNumber(sales.getInvoiceNumber());
                        detail.setReqId(sales.getReqId());
                    });
                    return sales.getSalesDetails().stream();
                })
                .collect(Collectors.toList());

        if (!allSalesDetails.isEmpty()) {
            List<SalesDetails> preProcessedList = preProcessSalesDetails(allSalesDetails);
            Collection<SalesDetails> savedDetailsList = salesDetailsService.batchSave(preProcessedList);

            ConcurrentHashMap<String, List<SalesDetails>> detailsMap = new ConcurrentHashMap<>();
            savedDetailsList.stream()
                    .collect(Collectors.groupingBy(SalesDetails::getInvoiceNumber))
                    .forEach(detailsMap::put);

            return detailsMap;
        }

        return new ConcurrentHashMap<>();
    }

    private ConcurrentHashMap<String, List<SalesHistory>> populateSalesHistory(List<Sales> salesList) {
        List<SalesHistory> allSalesHistory = salesList.stream()
                .filter(sales -> sales.getSalesHistory() != null && !sales.getSalesHistory().isEmpty())
                .flatMap(sales -> {
                    sales.getSalesHistory().forEach(history -> {
                        history.setInvoiceNumber(sales.getInvoiceNumber());
                        history.setReqId(sales.getReqId());
                    });
                    return sales.getSalesHistory().stream();
                })
                .collect(Collectors.toList());

        if (!allSalesHistory.isEmpty()) {
            List<SalesHistory> preProcessedList = preProcessSalesHistory(allSalesHistory);
            Collection<SalesHistory> savedHistoryList = salesHistoryService.batchSave(preProcessedList);

            ConcurrentHashMap<String, List<SalesHistory>> historyMap = new ConcurrentHashMap<>();
            savedHistoryList.stream()
                    .collect(Collectors.groupingBy(SalesHistory::getInvoiceNumber))
                    .forEach(historyMap::put);

            return historyMap;
        }

        return new ConcurrentHashMap<>();
    }

    private void populateBatchAssociatedData(List<Sales> salesList) {
        // Populate SalesDetails
        ConcurrentHashMap<String, List<SalesDetails>> savedDetailsMap = populateSalesDetails(salesList);

        // Populate SalesHistory
        ConcurrentHashMap<String, List<SalesHistory>> savedHistoryMap = populateSalesHistory(salesList);

        // Update each sales record with saved details and history
        salesList.forEach(sales -> {
            String invoiceNumber = sales.getInvoiceNumber();

            if (savedDetailsMap.containsKey(invoiceNumber)) {
                sales.setSalesDetails(savedDetailsMap.get(invoiceNumber));
            }

            if (savedHistoryMap.containsKey(invoiceNumber)) {
                sales.setSalesHistory(savedHistoryMap.get(invoiceNumber));
            }
        });
    }

    private void preBatchSave(List<Sales> salesList) {
        populateBatchAssociatedData(salesList);
    }

    public List<List<Sales>> getItemsToSaveList(List<Sales> salesList) {
        List<List<Sales>> result = new ArrayList<>();
        List<String> invoiceNumbers = salesList.stream()
                .map(Sales::getInvoiceNumber)
                .collect(Collectors.toList());

        Map<String, Sales> savedList = getDslContext()
                .select(CK_SALES.asterisk())
                .from(CK_SALES)
                .where(CK_SALES.INVOICE_NUMBER.in(invoiceNumbers))
                .fetch()
                .intoMap(CK_SALES.INVOICE_NUMBER,
                        record -> record.into(Sales.class));

        List<Sales> itemsToInsert = new ArrayList<>();
        List<Sales> itemsToUpdate = new ArrayList<>();

        for (Sales sale : salesList) {
            fillAttributes(sale, (savedList.get(sale.getInvoiceNumber())));
            fillCommonAttributes(sale);
            new AttributeUpdateOverrideManager().overrideAttributes(sale, savedList.get(sale.getInvoiceNumber()));


            if (savedList.get(sale.getInvoiceNumber()) == null) {
                sale.setId(UUID.randomUUID().toString());
                sale.setChanged(true);
                itemsToInsert.add(sale);
                sale.setOperationPerformed(ActionType.INSERT);
            } else {
                Sales existingSale = (savedList.get(sale.getInvoiceNumber()));
                sale.setId(existingSale.getId());
                sale.setVersion(existingSale.getVersion() + 1);

                if (!Objects.equals(sale.getHash(), existingSale.getHash())) {
                    sale.setChanges(CdmDiffUtil.getChanges(sale, existingSale));
                    sale.setOperationPerformed(ActionType.UPDATE);
                    sale.setChanged(true);
                    itemsToUpdate.add(sale);
                }
            }
        }

        result.add(itemsToInsert);
        result.add(itemsToUpdate);
        return result;
    }

    @Override
    public Collection<Sales> batchSave(Collection<Sales> salesCollection) {
        LOG.info("Size of list is " + salesCollection.size());
        List<Sales> salesList = new ArrayList<>(salesCollection);

        LOG.info("Pre Batch Save Called with size " + salesList.size());
        preBatchSave(salesList);

        List<List<Sales>> saveItemsList = getItemsToSaveList(salesList);

        if (!saveItemsList.get(0).isEmpty()) {
            getDslContext().batchInsert(
                    saveItemsList.get(0).stream()
                            .map(sale -> getDslContext().newRecord(CK_SALES, sale))
                            .collect(Collectors.toList())
            ).execute();
        }

        if (!saveItemsList.get(1).isEmpty()) {
            getDslContext().batchUpdate(
                    saveItemsList.get(1).stream()
                            .map(sale -> {
                                CkSalesRecord record = getDslContext().newRecord(CK_SALES, sale);
                                // record.changed(CK_SALES.ID, false); // Avoid updating primary key
                                return record;
                            })
                            .collect(Collectors.toList())
            ).execute();
        }

        if (!saveItemsList.get(0).isEmpty() || !saveItemsList.get(1).isEmpty()) {
            postBatchSave(salesList);
        }

        LOG.info("Batch save successful");
        CacheManager.getInstance().evictAll("dataintegration-sales");
        return salesList;
    }

    public void postBatchSave(List<Sales> salesList) {
        // Add any post-save operations here if needed
        // For example, updating related entities or triggering events
        LOG.info("Post batch save completed for {} sales records", salesList.size());
    }
}