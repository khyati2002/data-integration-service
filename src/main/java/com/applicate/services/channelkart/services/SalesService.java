package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.client.properties.PropertyDefinition;
import com.applicate.services.channelkart.client.properties.PropertyRegistry;
import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.GRNStatus;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.utils.*;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.cache.Cacheable;
import com.salescode.dim.etl.enrichment.service.DataEnrichmentService;
import com.salescode.dim.etl.enrichment.service.EnrichmentInfoRegistry;
import com.salescode.dim.etl.registry.ETLRegistry;
import com.salescode.dim.etl.validation.service.DataValidationService;
import com.salescode.dim.etl.validation.service.ValidationExcludeGroupRegistry;
import com.salescode.dim.etl.validation.service.ValidationInfoRegistry;
import com.salescode.dim.jooq.impl.*;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.dim.scanner.ExternalRegistryScanner;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final DataValidationService dataValidationService;
    private final DataEnrichmentService dataEnrichmentService;
    private final ValidationInfoRegistry validationInfoRegistry;
    private final ValidationExcludeGroupRegistry validationExcludeGroupRegistry;
    private final EnrichmentInfoRegistry enrichmentInfoRegistry;
    private final ETLRegistry etlRegistry;
    private final StockService stockService;
    private final UserService userService;
    private final DivisionService divisionService;
    private final MetaDataService metaDataService;
    private final OutletDetailsService outletDetailsService;
    private final MicroOutletDetailsService microOutletDetailsService;
    private static final String CONTACT_NO = "0000000000";
    private final EntityUtils entityUtils;
    private final SalesGRNService salesGRNService;

    public SalesService() {
        salesGRNService = new SalesGRNService();
        divisionService = new DivisionService();
        userService = new UserService();
        metaDataService =  new MetaDataService();
        stockService =   new StockService();
        ExternalRegistryScanner externalRegistryScanner = ExternalRegistryScanner.getInstance();
        etlRegistry = ETLRegistry.getInstance(externalRegistryScanner);
        validationInfoRegistry = new ValidationInfoRegistry(getDslContext());
        validationExcludeGroupRegistry = new ValidationExcludeGroupRegistry(getDslContext());
        enrichmentInfoRegistry = new EnrichmentInfoRegistry(getDslContext());
        dataValidationService = new DataValidationService(validationInfoRegistry, validationExcludeGroupRegistry, etlRegistry);
        dataEnrichmentService = new DataEnrichmentService(enrichmentInfoRegistry, etlRegistry);
        outletDetailsService = new OutletDetailsService();
        entityUtils =  new EntityUtils(getDslContext());
        microOutletDetailsService = new MicroOutletDetailsService();
    }

    @Cacheable(cacheName = "dataintegration-sales")
    public Sales findByInvoiceNumber(String invoiceNumber) {
       return getDslContext()
                .select(CK_SALES.asterisk())
                .from(CK_SALES)
                .where(CK_SALES.INVOICE_NUMBER.eq(invoiceNumber))
                .fetchOneInto(Sales.class);
    }


    private MicroOutletDetails getOrSaveOutlet(String outletCode) {
        MicroOutletDetails out;
        MicroOutletDetails od = microOutletDetailsService
                .findByOutletCode(outletCode);
        if (od == null) {
            MicroOutletDetails microOutletDetails = new MicroOutletDetails();
            microOutletDetails.setOutletCode(outletCode);
            microOutletDetails.setActiveStatus(ActiveStatus.INACTIVE);
            microOutletDetails.setContactno(CONTACT_NO);
            microOutletDetails.setLoginid(outletCode);
            od = microOutletDetailsService.save(microOutletDetails);
            MicroOutletDetails outletDetails = microOutletDetailsService
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
            Location loc = new Location();
            loc.setCountry(userService.findByLoginId(SecurityContextUtils.getPrincipal()).getLocationHierarchy());
            user2.setLocationHierarchy(loc);
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
        if (sales.getOutletcode() != null) {
            MicroOutletDetails findByOutletCode = microOutletDetailsService.findByOutletCode(sales.getOutletcode());
            if (sales.getOutletcode() != null && findByOutletCode == null) {
                synchronized (sales.getOutletcode().intern()) {
                    MicroOutletDetails outlet = getOrSaveOutlet(sales.getOutletcode());
                    sales.setOutletcode(outlet.getOutletcode());
                    findByOutletCode = outlet;
                }
            }
            if (findByOutletCode.getActiveStatus() == null) {
                sales.setActiveStatus(ActiveStatus.INACTIVE);
            }
        }

        User findByLoginId = userService.findByLoginId(sales.getLoginid());
            if (sales.getLoginid() != null && findByLoginId == null) {
                try {
                    synchronized (sales.getLoginid().intern()) {
                        User user = getOrSetUser(sales.getLoginid());
                        sales.setLoginid(user!=null? user.getLoginid(): "");
                        findByLoginId = user;
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            if (findByLoginId.getActiveStatus() == null) {
                sales.setActiveStatus(ActiveStatus.INACTIVE);
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
                return getDslContext().selectFrom(CK_SALES)
                        .where(CK_SALES.ID.eq(idValue))
                        .fetchOptionalInto(Sales.class)
                        .orElse(null);
            }
        }

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
        return null;
    }

    @Override
    public Collection<Sales> batchSave(Collection<Sales> salesCollection) {
        LOG.info("Size of list is {}", salesCollection.size());
        return salesCollection.stream()
                       .map(sales -> {
                           try {
                               return save(sales, findByDynamicIdUsingJooq(sales));
                           } catch (JsonProcessingException e) {
                               throw new RuntimeException(e);
                           }
                       })
                       .collect(Collectors.toList());
    }

    private Sales save(Sales sales, Sales salesDb) throws JsonProcessingException {
        try {
            if (salesDb != null && TALLY.equalsIgnoreCase(sales.getSource())) {
                sales.setUpdate(true);
                sales.setOldModel(EntityUtils.deepClone(salesDb));
            }
            var sl = sales;
            createAssociatedData(sl);

            if (salesDb != null) {
                LocalDateTime saleCreationTime = sl.getCreationTime();
                initializeDate(sl, salesDb, saleCreationTime);
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
                            var saleDB = salesDMap.get(sld.getBatchCode() + sld.getBatchId() + sld.getType());
                            double amtDiff = sld.getInitialAmount() - saleDB.getInitialAmount();
                            double qtyDiff = sld.getNormalizedQuantity() - saleDB.getNormalizedQuantity();
                            if (NullUtils.isNull(sld.getDiscountInfo())) {
                                saleDB.setDiscountInfo(null);
                            }
                            EntityUtils.copyPropertiesWithoutMerging(sld, saleDB, SYSTEM_TIME, VERSION);

                            addIncreasedAmountQuantity(saleDB, amtDiff, qtyDiff);

                        } else {
                            salesDb.getSalesDetails().add(sld);
                            addIncreasedAmountQuantity(sld, sld.getInitialAmount(), sld.getNormalizedQuantity());
                        }
                });
                EntityUtils.copyPropertiesWithoutMerging(sales, salesDb, SALES_DETAILS, VERSION);
                sales = salesDb;
            } else {

                for (SalesDetails sld : sl.getSalesDetails()) {
                    sld.setInvoiceNumber(sl.getId());
                    addIncreasedAmountQuantity(sld, sld.getInitialAmount(), sld.getNormalizedQuantity());
                }
            }
            addReturnParameters(sales);
            var sls = sales;
            cdmSave(sls);
            addingOrDeductingStock(sls);
            return sls;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

        DSLContext dsl = getDslContext();
        String id =sales.getId();

        Sales existing = dsl.selectFrom(CK_SALES)
                                 .where(CK_SALES.ID.eq(id))
                                 .fetchOneInto(Sales.class);

        if (existing == null) {
            sales.setVersion(0);
            sales.setChanged(true);
            sales.setOperationPerformed(ActionType.INSERT);
            fillCommonAttributes(sales);
            addHash(sales);
            dsl.insertInto(CK_SALES)
                    .set(dsl.newRecord(CK_SALES, sales))
                    .execute();
        } else {
            sales.setId(existing.getId());
            sales.setVersion(existing.getVersion() + 1);
            sales.setChanged(true);
            sales.setOperationPerformed(ActionType.UPDATE);
            addHash(sales);
            sales.setChanges(CdmDiffUtil.getChanges(sales, existing));

            dsl.update(CK_SALES)
                    .set(dsl.newRecord(CK_SALES, sales))
                    .where(CK_SALES.ID.eq(id))
                    .execute();
        }

        if (PropertyRegistry.getAsBoolean(PropertyDefinition.CREATE_GRN_FOR_INVOICE) && isPrimaryInvoice(sales.getOutletCode())) {
            org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode extendedAttributes = sales.getExtendedAttributes();
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
        org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode flinkNode = saleDB.getExtendedAttributes();
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
                otherUnitQty = productDetails != null ? productDetails.getCaseToOtherUnitQuantity().multiply(BigDecimal.valueOf(quantity)).doubleValue(): 0.0;
                pieceQty = productDetails != null ?productDetails.getCaseToPieceQuantity().multiply(BigDecimal.valueOf(quantity)).doubleValue():0.0;
            } else if (sd.getPieceQuantity() != 0) {
                quantity = sd.getPieceQuantity();
                pieceQty = quantity;
            } else {
                quantity = sd.getOtherUnitQuantity();
                otherUnitQty = quantity;
            }
            if (productDetails!=null && productDetails.getPieceToOtherUnitQuantity().compareTo(BigDecimal.ZERO) != 0) {
                float pieceToOtherUnitQuantity = productDetails.getPieceToOtherUnitQuantity().floatValue();
                normalizedQty = (pieceToOtherUnitQuantity * pieceQty) + otherUnitQty;
            } else if (productDetails!=null && productDetails.getOtherUnitToPieceQuantity().compareTo(BigDecimal.ZERO) != 0) {
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

    private void addReturnParameters(Sales sales) {
        boolean flag = sales.getExtendedAttributes() != null ;
        JsonNode extAttr = sales.getExtendedAttributes();
        if (flag) {
            ObjectNode exAttrNode;
            if (extAttr != null && extAttr.isObject()) {
                exAttrNode = (ObjectNode) extAttr;
            } else {
                exAttrNode = JSONUtils.getObjectMapper().createObjectNode();
            }

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

}