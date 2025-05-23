/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.enrichments.EnrichmentPhase;
import com.applicate.services.channelkart.models.enums.ActionType;

import com.applicate.services.channelkart.repository.ProductDetailsRepository;
import com.applicate.services.channelkart.utils.*;
import com.salescode.dim.cache.CacheManager;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dim.etl.enrichment.service.EnrichmentInfoRegistry;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.jooq.generated.tables.pojos.Metadata;
import com.salescode.dim.jooq.generated.tables.records.CkOutletActivityRecord;
import com.salescode.dim.jooq.impl.ProductDetails;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.scanner.ExternalRegistryScanner;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_OUTLET_ACTIVITY;
import static com.salescode.dim.jooq.generated.Tables.CK_PRODUCTDETAILS;

public class ProductDetailsService extends AbstractCDMService<ProductDetails> {

    public static final String STACKTRACE = "stacktrace";
    public static final String BATCH_CODE_SEPARATOR = "-";
    public static final String BATCH_CODES = "batchCodes";
    private static final  String BATCHCODE_DOMAIN_NAME = "batchcodekeys";
    private static final String BATCHCODE_DOMAIN_TYPE = "product";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final List<String> fileNameColumns = Arrays.asList("fileName", "fileName_a", "fileName_b", "fileName_c", "fileName_f", "fileName_l");
    private final UserService userService;
    private final MetaDataService metaDataService;
    private final DataEnrichmentService dataEnrichmentService;
    private final ProductDetailsRepository productDetailsRepository;


    public ProductDetailsService() {
        ExternalRegistryScanner externalRegistryScanner = ExternalRegistryScanner.getInstance();
        ETLRegistry etlRegistry = ETLRegistry.getInstance(externalRegistryScanner);
        userService = new UserService();
        metaDataService = new MetaDataService();
        EnrichmentInfoRegistry enrichmentInfoRegistry = new EnrichmentInfoRegistry(getDslContext());
        dataEnrichmentService = new DataEnrichmentService(enrichmentInfoRegistry, etlRegistry);
        productDetailsRepository = new ProductDetailsRepository(getDslContext());
    }

    @Override
    public Collection<ProductDetails> batchSave(Collection<ProductDetails> productDetails) {
        List<ProductDetails> entityList = preBatchSave(productDetails);
        List<List<ProductDetails>> saveItemsList = getItemsToSaveList(entityList);
        if (!saveItemsList.get(0).isEmpty()) {
            getDslContext().batchInsert(
                    saveItemsList.get(0).stream()
                            .map(target -> getDslContext().newRecord(CK_OUTLET_ACTIVITY, target)) // Convert to jOOQ Records
                            .collect(Collectors.toList())).execute();
        }
        if (!saveItemsList.get(1).isEmpty()) {
            getDslContext().batchUpdate(
                    saveItemsList.get(1).stream()
                            .map(target -> {
                                CkOutletActivityRecord targetsRecord = getDslContext().newRecord(CK_OUTLET_ACTIVITY, target);
                                targetsRecord.changed(CK_OUTLET_ACTIVITY.ID, false); // Avoid updating primary key
                                return targetsRecord;
                            })
                            .collect(Collectors.toList())
            ).execute();
        }
        CacheManager.getInstance().evictAll("dataintegration-user");
        if (!saveItemsList.get(0).isEmpty() || !saveItemsList.get(1).isEmpty()) {
            postBatchSave(entityList);
        }

        return entityList;
    }

    public List<ProductDetails> preBatchSave(Collection<ProductDetails> productDetails) {
        List<ProductDetails> productDetailsList = new ArrayList<>();
        productDetails.forEach(productEntry -> {
            prepareProductDetails(productEntry);
            if (productEntry.getId() == null) {
                String id = new IdGenerator(productEntry.getClass().getSimpleName()).getId(productEntry);
                productEntry.setId(id);
                productDetailsList.add(productEntry);
            }
        });
        return productDetailsList;
    }

    public void postBatchSave(List<ProductDetails> entityList) {
        // postBatchSave
    }

    private List<List<ProductDetails>> getItemsToSaveList(List<ProductDetails> entityList) {

        List<List<ProductDetails>> result = new ArrayList<>();

        List<String> ids = entityList.stream()
                .map(ProductDetails::getBatchCode)
                .collect(Collectors.toList());

        Map<String, ProductDetails> savedList = getDslContext().selectFrom(CK_OUTLET_ACTIVITY)
                .where(CK_PRODUCTDETAILS.BATCH_CODE.in(ids))
                .fetch()
                .intoMap(CK_PRODUCTDETAILS.BATCH_CODE, recordEntry -> recordEntry.into(ProductDetails.class));

        List<ProductDetails> itemsToInsert = new ArrayList<>();
        List<ProductDetails> itemsToUpdate = new ArrayList<>();

        for (ProductDetails entry : entityList) {

            fillAttributes(entry, ProductDetails.of(savedList.get(entry.getId())));
            fillCommonAttributes(entry);
            new AttributeUpdateOverrideManager().overrideAttributes(entry, savedList.get(entry.getId()));
            super.addHash(entry);
            preSaveEnrichment(entry);
            if (savedList.get(entry.getId()) == null) {
                entry.setVersion(0);
                entry.setOperationPerformed(ActionType.INSERT);
                itemsToInsert.add(entry);

            } else {
                ProductDetails savedEntry = ProductDetails.of(savedList.get(entry.getId()));
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

    private void preSaveEnrichment(ProductDetails targets) {
        OperationResult or = dataEnrichmentService.enrich(targets, EnrichmentPhase.PRE_SAVE);
        if (!or.getStatus().equals(OperationResult.Status.OK)) {
            throw new RuntimeException("Pre save enrichment error");
        }
    }

    public String getLocationHierarchyIfExists(OutletDetails outlet) {
        return outlet.getLocationHierarchy() == null ? null : outlet.getLocationHierarchy();
    }

    public void prepareProductDetails(ProductDetails productdetails) {

        fillBatchCode(productdetails);

        for (String column : fileNameColumns) {
            try {
                String value = (String) new PropertyDescriptor(column, ProductDetails.class).getReadMethod().invoke(productdetails);
                if (value != null && !value.isBlank()) {
                    String filesimplename = FileUtils.getSimpleFileNameWithExtenstion(value);
                    if (filesimplename != null) {
                        filesimplename = Optional.ofNullable(FilenameUtils.getBaseName(filesimplename).toLowerCase()).orElse(null);
                    }
//					PropertyAccessor accessor = PropertyAccessorFactory.forBeanPropertyAccess(productdetails);
//					accessor.setPropertyValue(column, filesimplename);
                    Field field = ProductDetails.class.getDeclaredField(column); // Get field by column name
                    // Make the field accessible if it's private
                    field.setAccessible(true);
                    // Set the value dynamically (equivalent to Spring's PropertyAccessor.setPropertyValue)
                    field.set(productdetails, filesimplename);


                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                     | IntrospectionException e) {
                logger.error(STACKTRACE, e);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
        // taxservice.preparingTaxObject(productdetails);
    }

    public void fillBatchCode(ProductDetails product) {
        try {
            if (product.getBatchCode() == null) {
                List<String> keys = getBatchKeys();
                if (keys != null && !keys.isEmpty()) {
                    StringBuffer buffer = new StringBuffer();
                    for (String key : keys) {
                        if (buffer.length() == 0) {
                            buffer.append(PropertyUtils.getProperty(product, key));
                        } else {
                            buffer.append(BATCH_CODE_SEPARATOR).append(PropertyUtils.getProperty(product, key));
                        }
                    }
                    product.setBatchCode(buffer.toString().toLowerCase().replaceAll("[ .]", ""));
                } else {
                    logger.error("Neither batchcode config found in metadata nor it's present in input data. Please check.");
                }
            }
        } catch (Throwable th) {
            throw new RuntimeException(th);
        }
    }

    public List<String> getBatchKeys() {
        try {
            String lob = SecurityContextUtils.getLob();

            Metadata metaData = metaDataService.fetchByValue(BATCHCODE_DOMAIN_NAME, BATCHCODE_DOMAIN_TYPE);
            if (metaData != null && metaData.getDomainValues() != null) {
                JsonNode values = metaData.getDomainValues();
                if (values != null && !values.isEmpty()) {
                    ArrayNode arrayNode = JSONUtils.convertToArrayNode(metaData.getDomainValues());
                    List<String> result = new ArrayList<>(arrayNode.size());
                    values.forEach(jsonNode -> result.add(jsonNode.asText()));
                    return result;
                }

            }
        } catch (Exception e) {
            logger.error(STACKTRACE, e);
        }
        return null;
    }


    public boolean checkIfBatchCodeExists(String batchCode) {
        ProductDetails result = this.productDetailsRepository.findByBacthCode(batchCode).orElse(null);
        return result != null;
    }
}


