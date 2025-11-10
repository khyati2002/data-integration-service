package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.client.properties.PropertyDefinition;
import com.applicate.services.channelkart.client.properties.PropertyRegistry;
import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.GRNStatus;
import com.applicate.services.channelkart.utils.CdmDiffUtil;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.salescode.dim.PreProcessPipelineService;
import com.salescode.dim.cache.CacheManager;
import com.salescode.dim.cache.Cacheable;
import com.salescode.dim.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dim.etl.enrichment.service.EnrichmentInfoRegistry;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.etl.validation.service.DataValidationService;
import com.salescode.dim.etl.validation.service.ValidationExcludeGroupRegistry;
import com.salescode.dim.etl.validation.service.ValidationInfoRegistry;
import com.salescode.dim.jooq.impl.*;
import com.salescode.dim.scanner.ExternalRegistryScanner;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.*;

public class SalesService extends AbstractCDMService<Sales> {

    public static final String TALLY = "tally";
    private static final String RETURN = "return";
    private static final String DOMAIN_NAME = "stock-deduction-enable";
    private static final String DOMAIN_TYPE = "sale";
    public static final String SYSTEM_TIME = "systemTime";
    private static final String VERSION = "version";
    public static final String INCREASED_AMOUNT = "increasedAmount";
    private static final String INCREASED_QUANTITY = "increasedQuantity";
    private static final String POST_PROCESS = "postProcess";
    private static final String SALES_DETAILS = "salesDetails";
    private static final String TAX_INFO = "taxInfo";
    public static final String TAX_AMOUNT = "taxAmount";
    private static final Logger LOG = LoggerFactory.getLogger(SalesService.class);

    private final SalesDetailsService salesDetailsService;
    private final SalesHistoryService salesHistoryService;
    private final DataValidationService dataValidationService;
    private final DataEnrichmentService dataEnrichmentService;
    private final PreProcessPipelineService preProcessPipelineService;
    private final ValidationInfoRegistry validationInfoRegistry;
    private final ValidationExcludeGroupRegistry validationExcludeGroupRegistry;
    private final EnrichmentInfoRegistry enrichmentInfoRegistry;
    private final ETLRegistry etlRegistry;
    private final StockService stockService;
    private final UserService userService;
    private final DivisionService divisionService;
    private final MetaDataService metaDataService;
    private final SalesGRNService salesGRNService;

    public SalesService() {
        salesGRNService = new SalesGRNService();
        divisionService = new DivisionService();
        userService = new UserService();
        metaDataService =  new MetaDataService();
        stockService =   new StockService();
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
            return new ConcurrentHashMap<>(savedDetailsList.stream().collect(Collectors.groupingBy(SalesDetails::getInvoiceNumber)));
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

            return new ConcurrentHashMap<>(savedHistoryList.stream().collect(Collectors.groupingBy(SalesHistory::getInvoiceNumber)));
        }

        return new ConcurrentHashMap<>();
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
        LOG.info("Size of list is {}", salesCollection.size());
//        salesCollection.forEach(sales -> save(sales,findUniqueRecord(Sales.class, sales)));
        List<Sales> salesList = new ArrayList<>(salesCollection);

        LOG.info("Pre Batch Save Called with size {}" ,salesList.size());
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
                                return getDslContext().newRecord(CK_SALES, sale);
                            })
                            .collect(Collectors.toList())
            ).execute();
        }

        LOG.info("Batch save successful");
        CacheManager.getInstance().evictAll("dataintegration-sales");
        return salesList;
    }



    private void save(Sales sales, Sales salesDb) throws JsonProcessingException {
        if(salesDb!=null && TALLY.equalsIgnoreCase(sales.getSource())){
                sales.setUpdate(true);
                sales.setOldModel(EntityUtils.deepClone(salesDb));
            }
        var sl = sales;
//       createAssociatedData(sl);

        if (salesDb != null) {
                LocalDateTime saleCreationTime = sl.getCreationTime();
                initializeDate(sl,salesDb,saleCreationTime);
            List<SalesDetails> salesDetails = salesDb.getSalesDetails();
            Map<String, SalesDetails> salesDMap = getSalesDetailMap(salesDetails);
            Map<String, SalesDetails> salesMap = getSalesDetailMap(sl.getSalesDetails());
            List<SalesDetails> deletedSalesDetails = salesDb.getSalesDetails().stream().filter(o -> !salesMap.containsKey(o.getBatchCode() + o.getBatchId() + o.getType())).collect(Collectors.toList());
            Sales finalSales = sales;
            deletedSalesDetails.forEach(od -> {
                od.setCaseQuantity(0.0);
                od.setPieceQuantity(0.0);
                od.setOtherUnitQuantity(0.0);
                od.setInitialCaseQuantity(0.0);
                od.setInitialPieceQuantity(0.0);
                od.setInitialOtherUnitQuantity(0.0);
                od.setNormalizedQuantity(0.0);
                od.setPrice(0.0);
                od.setNetAmount(0.0);
                od.setBillAmount(0.0);
                od.setInitialAmount(0.0);
                od.setBatchIds(org.jooq.JSON.valueOf(JSONUtils.getObjectMapper().createArrayNode().toString()));
                od.setDiscountInfo(null);
                finalSales.getSalesDetails().add(od);
            });
            sl.getSalesDetails().forEach(sld -> {
                if (salesDMap.get(sld.getBatchCode() + sld.getBatchId() + sld.getType()) != null) {
                    var saleDB = salesDMap.get(sld.getBatchCode()+sld.getBatchId()+sld.getType());
                    double amtDiff = sld.getInitialAmount() - saleDB.getInitialAmount();
                    double qtyDiff = sld.getNormalizedQuantity() - saleDB.getNormalizedQuantity();
                    if(NullUtils.isNull(sld.getDiscountInfo())){
                        saleDB.setDiscountInfo(null);
                    }
                    EntityUtils.copyPropertiesWithoutMerging(sld, saleDB, SYSTEM_TIME,VERSION );

                    addIncreasedAmountQuantity(saleDB,amtDiff,qtyDiff);

                } else {
                    salesDb.getSalesDetails().add(sld);
                    addIncreasedAmountQuantity(sld,sld.getInitialAmount(),sld.getNormalizedQuantity());
                }
            });
            EntityUtils.copyPropertiesWithoutMerging(sales, salesDb, SALES_DETAILS, VERSION);
            sales = salesDb;
        }else {

                for(SalesDetails sld:sl.getSalesDetails()) {
                    sld.setInvoiceNumber(sl.getId());
                    addIncreasedAmountQuantity(sld, sld.getInitialAmount(), sld.getNormalizedQuantity());
                }
        }
        addReturnParameters(sales);
        var sls = sales;
        cdmSave(sls);
        addingOrDeductingStock(sls);
    }



// save fucntions -----------------------------------------------------------------------------

    public boolean isPrimaryInvoice(String outletCode) {
        if(outletCode == null) return false;
        User user = userService.findByLoginId(outletCode);
        if(user == null) return false;
        Set<String> designation = new HashSet<>(Optional.ofNullable(user.getDesignation()).orElse(Set.of()));
        return designation.stream()
                       .anyMatch(divisionService::isChannelDivision);
    }

    public void cdmSave(Sales sales) throws JsonProcessingException {
        addReturnParameters(sales);
        super.save(sales);
        if (PropertyRegistry.getAsBoolean(PropertyDefinition.CREATE_GRN_FOR_INVOICE) && isPrimaryInvoice(sales.getOutletCode())) {
            JsonNode extendedAttributes = sales.getExtendedAttributes();
            String status = "IntegrationGrnStatus";
            if (!extendedAttributes.has(status) && sales.isCreate()) {
                GRNInfo grnInfo = new GRNInfo(
                        sales.getInvoiceNumber(),
                        sales.getOrderNumber(),
                        sales.getLoginId(),
                        GRNStatus.OPEN.name()
                );
                salesGRNService.addNewEntry(grnInfo);
            }
        }
    }


    private void addIncreasedAmountQuantity(SalesDetails saleDB, double amtDiff, double qtyDiff) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode flinkNode = saleDB.getExtendedAttributes();
        org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode extendedAttributes;
        try {
            if (flinkNode == null) {
                extendedAttributes = mapper.createObjectNode();
            } else {
                extendedAttributes = (org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(flinkNode.toString());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error processing extendedAttributes", e);
        }
        extendedAttributes.put(INCREASED_AMOUNT, amtDiff);
        extendedAttributes.put(INCREASED_QUANTITY, qtyDiff);
        extendedAttributes.put(POST_PROCESS, "true");

        saleDB.setExtendedAttributes(extendedAttributes);
    }

    private void addReturnParameters(Sales sales) throws JsonProcessingException {
        boolean flag = sales.getExtendedAttributes() != null ;
        if (flag) {
            ObjectNode exAttrNode = (ObjectNode) JSONUtils.getObjectMapper().readTree(sales.getExtendedAttributes().toString());
            exAttrNode.put(POST_PROCESS, "true");
            sales.setExtendedAttributes(exAttrNode);
            if (exAttrNode.get(RETURN) != null && exAttrNode.get(RETURN).asBoolean()) {
                salesReturn(sales);
            }
        } else {
            var extendedAttribute = JSONUtils.getObjectMapper().createObjectNode().put(POST_PROCESS, "true");
            sales.setExtendedAttributes(extendedAttribute);
        }
    }

    private void salesReturn(Sales sales) {

        sales.setTotalQuantity(negativeIfPositive(sales.getTotalQuantity()));
        sales.setTotalInitialQuantity(sales.getTotalInitialQuantity());
        sales.setInitialAmount(negativeIfPositive(sales.getInitialAmount()));
        sales.setNetAmount(negativeIfPositive(sales.getNetAmount()));
        sales.setBillAmount(negativeIfPositive(sales.getBillAmount()));
        sales.setNormalizedQuantity(negativeIfPositive(sales.getNormalizedQuantity()));
        sales.setInitialNormalizedQuantity(negativeIfPositive(sales.getInitialNormalizedQuantity()));

        sales.getSalesDetails().forEach(sal -> {
            negateTaxAmounts(sal);
            sal.setInitialAmount(negativeIfPositive(sal.getInitialAmount()));
            sal.setNetAmount(negativeIfPositive(sal.getNetAmount()));
            sal.setBillAmount(negativeIfPositive(sal.getBillAmount()));
            sal.setPieceQuantity(negativeIfPositive(sal.getPieceQuantity()));
            sal.setNormalizedQuantity(negativeIfPositive(sal.getNormalizedQuantity()));
            sal.setInitialNormalizedQuantity(negativeIfPositive(sal.getInitialNormalizedQuantity()));
            sal.setInitialCaseQuantity(negativeIfPositive(sal.getInitialCaseQuantity()));
            sal.setInitialPieceQuantity(negativeIfPositive(sal.getInitialPieceQuantity()));
            sal.setInitialOtherUnitQuantity(negativeIfPositive(sal.getInitialOtherUnitQuantity()));
            sal.setInitialQuantity(negativeIfPositive(sal.getInitialQuantity()));
            sal.setCaseQuantity(negativeIfPositive(sal.getCaseQuantity()));
            sal.setOtherUnitQuantity(negativeIfPositive(sal.getOtherUnitQuantity()));
        });
    }

    private void negateTaxAmounts(SalesDetails salesDetail) {
        ObjectNode extendedAttributes = (ObjectNode) salesDetail.getExtendedAttributes();
        if (extendedAttributes == null) {
            return;
        }
        ArrayNode taxInfo = (ArrayNode) extendedAttributes.get(TAX_INFO);
        if (taxInfo == null) {
            return;
        }

        for (int i = 0; i < taxInfo.size(); i++) {
            ObjectNode taxNode = (ObjectNode) taxInfo.get(i);
            if (taxNode.has(TAX_AMOUNT)) {
                double amount = taxNode.get(TAX_AMOUNT).asDouble();
                taxNode.put(TAX_AMOUNT, negativeIfPositive(amount));
            }
        }
    }

    private double negativeIfPositive(double value) {
        return  (value > 0 ? -value : value);
    }



// update sales details -----------------------------------------------------------------------------------

    private void initializeDate(Sales sl, Sales salesDb, LocalDateTime saleCreationTime) {
        sl.getSalesDetails().forEach(sld -> {
            if (sld.getCreationTime() == null) {
                sld.setCreationTime(saleCreationTime);
            }
            sld.setInvoiceNumber(salesDb.getId());
            sld.setLastModifiedTime(LocalDateTime.ofInstant(Calendar.getInstance().toInstant(), ZoneId.systemDefault()));
            sld.setSystemTime(LocalDateTime.ofInstant(Calendar.getInstance().toInstant(), ZoneId.systemDefault()));
        });
    }

    private Map<String, SalesDetails> getSalesDetailMap(List<SalesDetails> salesDetails) {
        Map<String, SalesDetails> salesDMap = new HashMap<>();
        for (SalesDetails salesD : salesDetails) {
            if (!salesDMap.containsKey(salesD.getBatchCode())) {
                salesDMap.put(salesD.getBatchCode() + salesD.getBatchId() + salesD.getType(), salesD);
            }
        }
        return salesDMap;
    }



//for stock deduction -----------------------------------------------------------------------------------

    public void addingOrDeductingStock(Sales sales) {
        // if new stock service is being used then current stock service should not be used to deduct stock
        if(PropertyRegistry.getAsBoolean(PropertyDefinition.NEW_STOCK_SERVICE_ENABLED)){
            return;
        }
        boolean flag = sales.getExtendedAttributes() != null ;
        if (flag) {
            var exAttrNode = sales.getExtendedAttributes();
            if (exAttrNode.get(RETURN) != null && exAttrNode.get(RETURN).asBoolean()) {
                Map<String, Map<String, Double>> suppBatCodQty = getSupplierBatchCodeQuantity(List.of(sales));
                stockService.deductAndAddStock(suppBatCodQty, true);
            } else {
                if (orderStockDeductionEnable(sales)) {
                    Map<String, Map<String, Double>> suppBatCodQty = getSupplierBatchCodeQuantity(List.of(sales));
                    stockService.deductAndAddStock(suppBatCodQty, false);
                }
            }
        } else {
            if (orderStockDeductionEnable(sales)) {
                Map<String, Map<String, Double>> suppBatCodQty = getSupplierBatchCodeQuantity(List.of(sales));
                stockService.deductAndAddStock(suppBatCodQty, false);
            }
        }

    }

    private Map<String, Map<String, Double>> getSupplierBatchCodeQuantity(List<Sales> sales) {
        return sales.stream().collect(Collectors.toMap(Sales::getOutletCode, s -> s.getSalesDetails().stream().collect(Collectors.toMap(SalesDetails::getBatchCode, sd -> {
            double quantity;
            double pieceQty = 0;
            double otherUnitQty = 0;
            double normalizedQty;
            var productDetails = sd.getProductDetails();
            if (sd.getCaseQuantity() != 0) {
                quantity = sd.getCaseQuantity();
                otherUnitQty = productDetails.getCaseToOtherUnitQuantity().multiply(BigDecimal.valueOf(quantity)).doubleValue();
                pieceQty = productDetails.getCaseToPieceQuantity().multiply(BigDecimal.valueOf(quantity)).doubleValue();
            } else if (sd.getPieceQuantity() != 0) {
                quantity = sd.getPieceQuantity();
                pieceQty = quantity;
            } else {
                quantity = sd.getOtherUnitQuantity();
                otherUnitQty = quantity;
            }
            if (productDetails.getPieceToOtherUnitQuantity().compareTo(BigDecimal.ZERO) != 0) {
                float pieceToOtherUnitQuantity = productDetails.getPieceToOtherUnitQuantity().floatValue();
                normalizedQty = (pieceToOtherUnitQuantity * pieceQty) + otherUnitQty;
            } else if (productDetails.getOtherUnitToPieceQuantity().compareTo(BigDecimal.ZERO) != 0) {
                float otherUnitToPieceQuantity = productDetails.getOtherUnitToPieceQuantity().floatValue();
                normalizedQty = (otherUnitToPieceQuantity * otherUnitQty) + pieceQty;
            } else {
                normalizedQty = otherUnitQty + pieceQty;
            }
            return normalizedQty;
        }))));
    }

    private boolean orderStockDeductionEnable(Sales sale) {
        var metaData = metaDataService.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE);
        boolean metaStockDeduction=(metaData != null && metaData.getDomainValues().get(0).get("enable").asBoolean());
        boolean extAttrStockDeduction=sale.getExtendedAttributes()!=null && (!sale.getExtendedAttributes().has("stockDeduction") || sale.getExtendedAttributes().get("stockDeduction").asBoolean());
        return (metaStockDeduction && extAttrStockDeduction);
    }

    private String findOutletCodeForInvoiceNumber(String invoiceNumber){
        Sales salesData = findByInvoiceNumber(invoiceNumber);
        if(salesData!=null){
            return salesData.getOutletCode();
        }
        return "";
    }
}