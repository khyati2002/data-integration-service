package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.utils.CdmDiffUtil;
import com.salescode.dim.cache.CacheManager;
import com.salescode.dim.cache.Cacheable;
import com.salescode.dim.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dim.etl.enrichment.service.EnrichmentInfoRegistry;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.etl.validation.service.DataValidationService;
import com.salescode.dim.etl.validation.service.ValidationExcludeGroupRegistry;
import com.salescode.dim.etl.validation.service.ValidationInfoRegistry;
import com.salescode.dim.jooq.generated.tables.records.CkSalesDetailsRecord;
import com.salescode.dim.jooq.impl.ProductDetails;
import com.salescode.dim.jooq.impl.SalesDetails;
import com.salescode.dim.scanner.ExternalRegistryScanner;
import com.salescode.dim.PreProcessPipelineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.*;

public class SalesDetailsService extends AbstractCDMService<SalesDetails> {
    private static final Logger LOG = LoggerFactory.getLogger(SalesDetailsService.class);

    private final ProductDetailsService productDetailsService;
    private final DataValidationService dataValidationService;
    private final DataEnrichmentService dataEnrichmentService;
    private final PreProcessPipelineService preProcessPipelineService;
    private final ValidationInfoRegistry validationInfoRegistry;
    private final ValidationExcludeGroupRegistry validationExcludeGroupRegistry;
    private final EnrichmentInfoRegistry enrichmentInfoRegistry;
    private ETLRegistry etlRegistry;

    public SalesDetailsService() {
        ExternalRegistryScanner externalRegistryScanner = ExternalRegistryScanner.getInstance();
        etlRegistry = ETLRegistry.getInstance(externalRegistryScanner);
        productDetailsService = new ProductDetailsService();
        validationInfoRegistry = new ValidationInfoRegistry(getDslContext());
        validationExcludeGroupRegistry = new ValidationExcludeGroupRegistry(getDslContext());
        enrichmentInfoRegistry = new EnrichmentInfoRegistry(getDslContext());
        dataValidationService = new DataValidationService(validationInfoRegistry, validationExcludeGroupRegistry, etlRegistry);
        dataEnrichmentService = new DataEnrichmentService(enrichmentInfoRegistry, etlRegistry);
        preProcessPipelineService = new PreProcessPipelineService(dataValidationService, dataEnrichmentService);
    }

    @Cacheable(cacheName = "dataintegration-sales-details")
    public SalesDetails findById(String id) {
        SalesDetails salesDetails = getDslContext()
                .select(CK_SALES_DETAILS.asterisk())
                .from(CK_SALES_DETAILS)
                .where(CK_SALES_DETAILS.ID.eq(id))
                .fetchOneInto(SalesDetails.class);
        return salesDetails;
    }

    private List<ProductDetails> preProcessProductDetails(List<ProductDetails> productDetailsList) {
        // Add preprocessing logic if needed
        // productDetailsList.parallelStream().forEach(product -> {
        //     preProcessPipelineService.preProcessPipeline(product, null);
        // });
        return productDetailsList;
    }

    private ConcurrentHashMap<String, ProductDetails> populateProductDetails(List<SalesDetails> salesDetailsList) {
        List<ProductDetails> productDetailsList = salesDetailsList.stream()
                .map(SalesDetails::getProductDetails)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        for (int i = 0; i < productDetailsList.size(); i++) {
            productDetailsList.get(i).setReqId(salesDetailsList.get(i).getReqId());
        }

        List<ProductDetails> preProcessedProductList = preProcessProductDetails(productDetailsList);
        Collection<ProductDetails> savedProductList = productDetailsService.batchSave(preProcessedProductList);
        ConcurrentHashMap<String, ProductDetails> productMap = new ConcurrentHashMap<>();

        // Populate ConcurrentHashMap from savedProductList
        savedProductList.parallelStream()
                .forEach(product -> productMap.put(product.getProductCode(), product));

        return productMap;
    }

    private ConcurrentHashMap<String, ProductDetails> populateBatchAssociatedData(List<SalesDetails> salesDetailsList) {
        ConcurrentHashMap<String, ProductDetails> savedProductList = populateProductDetails(salesDetailsList);

        for (int i = 0; i < salesDetailsList.size(); i++) {
            String productCode = salesDetailsList.get(i).getProductCode();
            salesDetailsList.get(i).setProductDetails(savedProductList.get(productCode));
        }

        return savedProductList;
    }

    private ConcurrentHashMap<String, ProductDetails> preBatchSave(List<SalesDetails> salesDetailsList) {
        ConcurrentHashMap<String, ProductDetails> savedProductList = populateBatchAssociatedData(salesDetailsList);
        return savedProductList;
    }

    public List<List<SalesDetails>> getItemsToSaveList(List<SalesDetails> salesDetailsList) {
        List<List<SalesDetails>> result = new ArrayList<>();
        List<String> ids = salesDetailsList.stream()
                .map(SalesDetails::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, SalesDetails> savedList = new HashMap<>();
        if (!ids.isEmpty()) {
            savedList = getDslContext()
                    .select(CK_SALES_DETAILS.asterisk())
                    .from(CK_SALES_DETAILS)
                    .where(CK_SALES_DETAILS.ID.in(ids))
                    .fetch()
                    .intoMap(CK_SALES_DETAILS.ID,
                            record -> record.into(SalesDetails.class));
        }

        List<SalesDetails> itemsToInsert = new ArrayList<>();
        List<SalesDetails> itemsToUpdate = new ArrayList<>();

        for (SalesDetails salesDetail : salesDetailsList) {
            fillAttributes(salesDetail, savedList.get(salesDetail.getId()));
            fillCommonAttributes(salesDetail);
            new AttributeUpdateOverrideManager().overrideAttributes(salesDetail, savedList.get(salesDetail.getId()));

            super.addHash(salesDetail);

            if (salesDetail.getId() == null || savedList.get(salesDetail.getId()) == null) {
                salesDetail.setVersion(0);
                if (salesDetail.getId() == null) {
                    salesDetail.setId(UUID.randomUUID().toString());
                }
                salesDetail.setChanged(true);
                itemsToInsert.add(salesDetail);
                salesDetail.setOperationPerformed(ActionType.INSERT);
            } else {
                SalesDetails existingSalesDetail = savedList.get(salesDetail.getId());
                salesDetail.setId(existingSalesDetail.getId());
                salesDetail.setVersion(existingSalesDetail.getVersion() + 1);

                if (!Objects.equals(salesDetail.getHash(), existingSalesDetail.getHash())) {
                    salesDetail.setChanges(CdmDiffUtil.getChanges(salesDetail, existingSalesDetail));
                    salesDetail.setOperationPerformed(ActionType.UPDATE);
                    salesDetail.setChanged(true);
                    itemsToUpdate.add(salesDetail);
                }
            }
        }

        result.add(itemsToInsert);
        result.add(itemsToUpdate);
        return result;
    }

    @Override
    public Collection<SalesDetails> batchSave(Collection<SalesDetails> salesDetailsCollection) {
        LOG.info("Size of list is " + salesDetailsCollection.size());
        List<SalesDetails> salesDetailsList = new ArrayList<>(salesDetailsCollection);

        LOG.info("Pre Batch Save Called with size " + salesDetailsList.size());
        Map<String, ProductDetails> savedProductList = preBatchSave(salesDetailsList);

        List<List<SalesDetails>> saveItemsList = getItemsToSaveList(salesDetailsList);

        if (!saveItemsList.get(0).isEmpty()) {
            getDslContext().batchInsert(
                    saveItemsList.get(0).stream()
                            .map(salesDetail -> getDslContext().newRecord(CK_SALES_DETAILS, salesDetail))
                            .collect(Collectors.toList())
            ).execute();
        }

        if (!saveItemsList.get(1).isEmpty()) {
            getDslContext().batchUpdate(
                    saveItemsList.get(1).stream()
                            .map(salesDetail -> {
                                CkSalesDetailsRecord record = getDslContext().newRecord(CK_SALES_DETAILS, salesDetail);
                                // record.changed(CK_SALES_DETAILS.ID, false); // Avoid updating primary key
                                return record;
                            })
                            .collect(Collectors.toList())
            ).execute();
        }

        if (!saveItemsList.get(0).isEmpty() || !saveItemsList.get(1).isEmpty()) {
            postBatchSave(salesDetailsList);
        }

        LOG.info("Batch save successful");
        CacheManager.getInstance().evictAll("dataintegration-sales-details");
        return salesDetailsList;
    }

    public void postBatchSave(List<SalesDetails> salesDetailsList) {
        // Add any post-save operations here if needed
        LOG.info("Post batch save completed for {} sales details records", salesDetailsList.size());
    }
}