package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.client.properties.PropertyDefinition;
import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.utils.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
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
import com.salescode.dim.jooq.impl.*;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
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
    private final DataValidationService dataValidationService;
    private final DataEnrichmentService dataEnrichmentService;
    private final PreProcessPipelineService preProcessPipelineService;
    private final ValidationInfoRegistry validationInfoRegistry;
    private final ValidationExcludeGroupRegistry validationExcludeGroupRegistry;
    private final EnrichmentInfoRegistry enrichmentInfoRegistry;
    private final OutletDetailsService outletDetailsService;
    private static final String CONTACT_NO = "0000000000";
    private final UserService userService;
    private final EntityUtils entityUtils;
    private ETLRegistry etlRegistry;
    private final OrderService orderService;

    public SalesService() {
        ExternalRegistryScanner externalRegistryScanner = ExternalRegistryScanner.getInstance();
        etlRegistry = ETLRegistry.getInstance(externalRegistryScanner);
        salesDetailsService = new SalesDetailsService();
        salesHistoryService = new SalesHistoryService();
        validationInfoRegistry = new ValidationInfoRegistry(getDslContext());
        validationExcludeGroupRegistry = new ValidationExcludeGroupRegistry(getDslContext());
        enrichmentInfoRegistry = new EnrichmentInfoRegistry(getDslContext());
        dataValidationService = new DataValidationService(validationInfoRegistry, validationExcludeGroupRegistry, etlRegistry);
        dataEnrichmentService = new DataEnrichmentService(enrichmentInfoRegistry, etlRegistry);
        preProcessPipelineService = new PreProcessPipelineService(dataValidationService, dataEnrichmentService);
        outletDetailsService = new OutletDetailsService();
        userService = new UserService();
        entityUtils =  EntityUtils.getInstance(getDslContext());
    }

    @Cacheable(cacheName = "dataintegration-sales")
    public Sales findByInvoiceNumber(String invoiceNumber) {
       return getDslContext()
                .select(CK_SALES.asterisk())
                .from(CK_SALES)
                .where(CK_SALES.INVOICE_NUMBER.eq(invoiceNumber))
                .fetchOneInto(Sales.class);
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


    private OutletDetails getOrSaveOutlet(String outletCode) {
        OutletDetails out;
        OutletDetails od = outletDetailsService
                .findByOutletCode(outletCode);
        if (od == null) {
            OutletDetails microOutletDetails = new OutletDetails();
            microOutletDetails.setOutletCode(outletCode);
            microOutletDetails.setActiveStatus(ActiveStatus.INACTIVE);
            microOutletDetails.setContactno(CONTACT_NO);
            od = outletDetailsService.save(microOutletDetails);
            OutletDetails outletDetails = outletDetailsService
                    .findByOutletCode(outletCode);
            if (outletDetails != null) {
                out = outletDetails;
            } else {
                out = od;
            }
        } else {
            out = od;
        }
        return out;
    }

    private User getOrSetUser(String user) {
        User out;
        User od = userService.findByLoginId(user);
        if (od == null) {
            User user2 = new User();
            user2.setActiveStatus(ActiveStatus.INACTIVE);
            user2.setUserAccountId(user);
            user2.setLoginId(user);
            user2.setMobile(CONTACT_NO);
            user2.setPassword(user);
            user2.setName(user);
            user2.setLocationHierarchy(userService.findByLoginId(SecurityContextUtils.getPrincipal()).getLocationHierarchy());
            od = userService.save(user2);
            User u = userService.findByLoginId(user);
            if (u != null) {
                out = u;
            } else {
                out = od;
            }
        } else {
            out = od;
        }
        return out;
    }

    private void createAssociatedData(Sales sales) {
        LOG.info(sales.getOutletcode());
        if (sales.getOutletcode() == null) {
            OutletDetails findByOutletCode = outletDetailsService.findByOutletCode(sales.getOutletcode());
            if (sales.getOutletcode() != null && findByOutletCode == null) {
                synchronized (sales.getOutletcode().intern()) {
                    OutletDetails outlet = getOrSaveOutlet(sales.getOutletcode());
                    sales.setOutletcode(outlet.getOutletcode());
                    findByOutletCode = outlet;
                }
            }
            if (findByOutletCode.getActiveStatus() == null) {
                sales.setActiveStatus(ActiveStatus.INACTIVE);
            }
        }

        User findByLoginId = userService.findByLoginId(sales.getLoginid());
        if (findByLoginId!=null) {
            if (sales.getLoginid() != null && findByLoginId == null) {
                synchronized (sales.getLoginid().intern()) {
                    User user = getOrSetUser(sales.getLoginid());
                    sales.setLoginid(user.getLoginid());
                    findByLoginId = user;
                }
            }
            if (findByLoginId.getActiveStatus() == null) {
                sales.setActiveStatus(ActiveStatus.INACTIVE);
            }
        }
    }


    public String getBeanProperty(Object cdm, String property) {
        try {
            if (cdm == null || property == null || property.isBlank()) {
                return null;
            }

            String[] parts = property.split("\\.");

            java.util.function.BiFunction<Object, String, Object> getProp = (obj, propName) -> {
                if (obj == null) return null;
                Class<?> cls = obj.getClass();
                String capitalized = propName.substring(0, 1).toUpperCase() + propName.substring(1);
                String[] getterNames = new String[] { "get" + capitalized, "is" + capitalized, propName };
                for (String gName : getterNames) {
                    try {
                        java.lang.reflect.Method m = cls.getMethod(gName);
                        if (m != null) {
                            return m.invoke(obj);
                        }
                    } catch (NoSuchMethodException ignored) {
                        // try next
                    } catch (Exception ex) {
                        break;
                    }
                }
                try {
                    java.lang.reflect.Field f = null;
                    Class<?> search = cls;
                    while (search != null) {
                        try {
                            f = search.getDeclaredField(propName);
                            break;
                        } catch (NoSuchFieldException e) {
                            search = search.getSuperclass();
                        }
                    }
                    if (f != null) {
                        f.setAccessible(true);
                        return f.get(obj);
                    }
                } catch (Exception ex) {
                    // ignore and return null
                }
                return null;
            };

            Object current = cdm;
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];

                if (current != null && com.fasterxml.jackson.databind.JsonNode.class.isAssignableFrom(current.getClass())) {
                    com.fasterxml.jackson.databind.JsonNode node = (com.fasterxml.jackson.databind.JsonNode) current;
                    if (parts.length == 1) {
                        return node.isTextual() ? node.asText() : node.toString();
                    } else if (i == parts.length - 1) {
                        return (node.has(part) && !node.get(part).isNull()) ? node.get(part).asText() : null;
                    } else {
                        current = node.has(part) ? node.get(part) : null;
                        continue;
                    }
                }

                Object next = getProp.apply(current, part);
                if (next == null) {
                    if (i == parts.length - 1) return null;
                    return null;
                }
                current = next;
            }

            if (current == null) return null;

            if (current instanceof com.fasterxml.jackson.databind.JsonNode) {
                com.fasterxml.jackson.databind.JsonNode node = (com.fasterxml.jackson.databind.JsonNode) current;
                return node.isTextual() ? node.asText() : node.toString();
            }

            return String.valueOf(current);

        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("could not find property {} from cdm object {}, class {}", property, cdm, cdm != null ? cdm.getClass() : null);
            }
            LOG.error("Error while reading property '{}': {}", property, e.getMessage(), e);
            return "";
        }
    }

    private Sales findByDynamicIdUsingJooq(Sales sales) {

        // 1) Try dynamic-primary-key lookup (same logic as your JPA findUniqueRecord)
        ArrayNode dynamicPrimaryKeys = entityUtils.fetchDynamicPrimaryKeys(Sales.class.getSimpleName());
        if (dynamicPrimaryKeys != null && dynamicPrimaryKeys.size() > 0) {
            StringBuilder value = new StringBuilder();

            for (int i = 0; i < dynamicPrimaryKeys.size(); i++) {
                String key = dynamicPrimaryKeys.get(i).asText();
                Object prop = getBeanProperty(sales, key);
                String tempval = prop == null ? null : String.valueOf(prop);
                if (tempval != null && !tempval.isBlank()) {
                    tempval = tempval.toLowerCase().replace(" ", "-");
                    if (value.length() == 0) {
                        value.append(tempval);
                    } else {
                        value.append("-").append(tempval);
                    }
                }
            }

            if (value.length() > 0) {
                String idValue;
                if (entityUtils.checkGenerateMD5Hash(Sales.class.getSimpleName())) {
                    idValue = entityUtils.getMd5(value.toString());
                } else {
                    idValue = value.toString();
                }
                // note: jOOQ will handle quoting; escapeSql kept only if you rely on that elsewhere.
                // Query by id
                return getDslContext().selectFrom(CK_SALES)
                        .where(CK_SALES.ID.eq(idValue))
                        .fetchOptionalInto(Sales.class)
                        .orElse(null);
            }
        }

        // 2) Fallbacks similar to your previous jOOQ helper:
        if (sales.getId() != null) {
            return getDslContext().selectFrom(CK_SALES)
                    .where(CK_SALES.ID.eq(sales.getId()))
                    .fetchOptionalInto(Sales.class)
                    .orElse(null);
        }

        if (sales.getInvoiceNumber() != null && !sales.getInvoiceNumber().isEmpty()) {
            return getDslContext().selectFrom(CK_SALES)
                    .where(CK_SALES.INVOICE_NUMBER.eq(sales.getInvoiceNumber()))
                    .fetchOptionalInto(Sales.class)
                    .orElse(null);
        }

        // 3) Final fallback: composite lookup (adjust fields if needed)
//        if (sales.getOutletCode() != null && sales.getLoginId() != null && sales.getTxnTime() != null) {
//            return getDslContext().selectFrom(CK_SALES)
//                    .where(CK_SALES.OUTLET_CODE.eq(sales.getOutletCode()))
//                    .and(CK_SALES.LOGIN_ID.eq(sales.getLoginId()))
//                    .and(CK_SALES.TXN_TIME.eq(sales.getTxnTime()))
//                    .fetchOptionalInto(Sales.class)
//                    .orElse(null);
//        }

        // nothing to lookup
        return null;
    }

    private void populateBatchAssociatedData(List<Sales> salesList) {
        ConcurrentHashMap<String, List<SalesDetails>> savedDetailsMap = populateSalesDetails(salesList);
        ConcurrentHashMap<String, List<SalesHistory>> savedHistoryMap = populateSalesHistory(salesList);
        salesList.forEach(sales -> {
            String invoiceNumber = sales.getInvoiceNumber();
            if (savedDetailsMap.containsKey(invoiceNumber)) {
                sales.setSalesDetails(savedDetailsMap.get(invoiceNumber));
            }
            if (savedHistoryMap.containsKey(invoiceNumber)) {
                sales.setSalesHistory(savedHistoryMap.get(invoiceNumber));
            }
            var salesDb = findByDynamicIdUsingJooq(sales);
            createAssociatedData(sales);
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


    public Sales cdmSave(Sales sales) {
        addReturnParameters(sales);
        handleTallyIntegrationIfApplicable(sales, sagaOrchestrator);

//        SagaStep<?> saveSales = Saga.step(
//                context -> super.save(sales),
//                context -> super.delete(context.get(SAVESALESSTEP, Sales.class)),
//                SAVESALESSTEP
//        );
        Sales saved = super.save(sales);
//        sagaOrchestrator.addStep(saveSales);

        if(propertyRegistry.getAsBoolean(PropertyDefinition.CREATE_GRN_FOR_INVOICE) && isPrimaryInvoice(sales)) {
            JsonNode extendedAttributes = sales.getExtendedAttributes();

            String status = "IntegrationGrnStatus";
            String statusReason = "IntegrationGrnStatusReason";
            if(!extendedAttributes.has(status) && sales.isCreate()) {
                SagaStep<?> createGRNInfo = Saga.nonReversibleStep(context -> {
                    Sales finalSales = context.get(SAVESALESSTEP, Sales.class);
                    GRNInfo grnInfo = new GRNInfo(
                            finalSales.getInvoiceNumber(),
                            finalSales.getOrderNumber(),
                            finalSales.getLoginId(),
                            GRNStatus.OPEN.name()
                    );
                    salesGrnService.addNewEntry(grnInfo);
                }, "SaveGRNInfoStep");
                OrderStatusUpdateStep orderStatusUpdateStep = new OrderStatusUpdateStep(orderService, Optional.ofNullable(sales.getOrderNumber()).orElse(""), INVOICED, "");
                sagaOrchestrator.addStep(createGRNInfo);
                sagaOrchestrator.addStep(orderStatusUpdateStep);
            } else if(extendedAttributes.has(status)) {
                String grnStatus = Objects.requireNonNull(extendedAttributes.get(status)).asText();
                String grnStatusReason = extendedAttributes.has(statusReason) ? extendedAttributes.get(statusReason).asText() : "";

                Map<String, Object> runtimeParams = Map.of(
                        "0", SpringContext.getBeanSafely(GRNInfoRepository.class),
                        "1", orderService,
                        "2", GRNStatus.PARTIALLY_REJECTED.name().equalsIgnoreCase(grnStatus) ? saved.getReferenceNumber() : saved.getInvoiceNumber(),
                        "3", grnStatus,
                        "4", orderStockHelperService,
                        "5", entityUtils,
                        "6", grnStatusReason,
                        "8", findOutletCodeForInvoiceNumber(GRNStatus.PARTIALLY_REJECTED.name().equalsIgnoreCase(grnStatus) ? saved.getReferenceNumber() : saved.getInvoiceNumber())
                );

                // Execute the GRN-status update flow synchronously
                updateGRNStatusFlow(runtimeParams);
            }
        }
        return saved;
    }

    private String findOutletCodeForInvoiceNumber(String invoiceNumber){
        Sales salesData = findByInvoiceNumber(invoiceNumber);
        if(salesData!=null){
            return salesData.getOutletcode();
        }
        return "";
    }



    @Override
    public Collection<Sales> batchSave(Collection<Sales> salesCollection) {
        LOG.info("Size of list is " + salesCollection.size());
        List<Sales> salesList = new ArrayList<>(salesCollection);

        LOG.info("Pre Batch Save Called with size " + salesList.size());
        preBatchSave(salesList);

        List<List<Sales>> saveItemsList = getItemsToSaveList(salesList);

        salesList.stream().forEach(sales -> cdmSave(sales));


//        if (!saveItemsList.get(0).isEmpty()) {
//            getDslContext().batchInsert(
//                    saveItemsList.get(0).stream()
//                            .map(sale -> getDslContext().newRecord(CK_SALES, sale))
//                            .collect(Collectors.toList())
//            ).execute();
//        }
//
//        if (!saveItemsList.get(1).isEmpty()) {
//            getDslContext().batchUpdate(
//                    saveItemsList.get(1).stream()
//                            .map(sale -> {
//                                CkSalesRecord record = getDslContext().newRecord(CK_SALES, sale);
//                                return record;
//                            })
//                            .collect(Collectors.toList())
//            ).execute();
//        }

        if (!saveItemsList.get(0).isEmpty() || !saveItemsList.get(1).isEmpty()) {
            postBatchSave(salesList);
        }

        LOG.info("Batch save successful");
        CacheManager.getInstance().evictAll("dataintegration-sales");
        return salesList;
    }

    public void postBatchSave(List<Sales> salesList) {
        LOG.info("Post batch save completed for {} sales records", salesList.size());
    }

    private void addReturnParameters(Sales sales) {
        boolean flag = sales.getExtendedAttributes() != null ;
        if (flag) {
            var exAttrNode = (ObjectNode) sales.getExtendedAttributes();
            exAttrNode.put("postProcess", "true");
            sales.setExtendedAttributes(exAttrNode);
            if (exAttrNode.get("return") != null && exAttrNode.get("return").asBoolean()) {
                salesReturn(sales);
            }
        } else {
            var extendedAttribute = JSONUtils.getObjectMapper().createObjectNode().put("postProcess", "true");
            sales.setExtendedAttributes(extendedAttribute);
        }
    }
}