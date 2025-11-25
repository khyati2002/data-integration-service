package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.utils.CdmDiffUtil;
import com.applicate.services.channelkart.utils.IdGenerator;
import com.salescode.dim.jooq.generated.tables.CkProductdetails;
import com.salescode.dim.jooq.generated.tables.pojos.Productmetadata;
import com.salescode.dim.jooq.impl.Location;
import com.salescode.dim.jooq.impl.ProductDetails;
import com.salescode.dim.jooq.impl.ProductMetaData;
import org.apache.commons.io.FilenameUtils;
import org.jooq.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jooq.Record;

import static com.salescode.dim.jooq.generated.Tables.CK_PRODUCTDETAILS;
import static com.salescode.dim.jooq.generated.tables.CkProductmetadata.CK_PRODUCTMETADATA;

public class ProductDetailsService extends AbstractCDMService<ProductDetails> {
    public static final String BATCH_CODE_SEPARATOR = "-";
    private static final Logger LOG = LoggerFactory.getLogger(ProductDetailsService.class);
    private final List<String> fileNameColumns = Arrays.asList("fileName", "fileName_a", "fileName_b", "fileName_c", "fileName_f", "fileName_l");
    private final LocationService locationService;

    public ProductDetailsService() {
        this.locationService = new LocationService();
    }

    public List<String> checkIfBatchCodeExists(String batchCode) {
        return getDslContext().select(CkProductdetails.CK_PRODUCTDETAILS.BATCH_CODE).from(CkProductdetails.CK_PRODUCTDETAILS).where(CkProductdetails.CK_PRODUCTDETAILS.BATCH_CODE.eq(batchCode)).fetch(CkProductdetails.CK_PRODUCTDETAILS.BATCH_CODE);
    }

    private static String getSimpleFileNameWithExtension(String filepath) {
        if (filepath == null) {
            return null;
        }
        String basename = FilenameUtils.getBaseName(filepath);
        if (basename != null) {
            basename = basename.replaceAll("[^A-Za-z0-9_/\\-()]", "");
        }
        String ext = FilenameUtils.getExtension(filepath);
        return !ext.isEmpty() ? basename + "." + ext : basename;

    }

    public List<List<ProductDetails>> getDataToSaveList(List<ProductDetails> productDetailsList) {

        List<List<ProductDetails>> result = new ArrayList<>();

        for (ProductDetails product : productDetailsList) {
            product.setChanged((byte) 0);
            product.setPriority(0);
            product.setActiveStatus(ActiveStatus.ACTIVE);

            fillBatchCode(product);
            processFileNames(product);
        }

        if (productDetailsList.isEmpty()) {
            result.add(new ArrayList<>());
            result.add(new ArrayList<>());
            return result;
        }

        List<String> batchCodes = productDetailsList.stream()
                .map(ProductDetails::getBatchCode)
                .collect(Collectors.toList());

        Map<String, com.salescode.dim.jooq.generated.tables.pojos.Productdetails> savedMap =
                getDslContext()
                        .selectFrom(CK_PRODUCTDETAILS)
                        .where(CK_PRODUCTDETAILS.BATCH_CODE.in(batchCodes))
                        .fetch()
                        .intoMap(CK_PRODUCTDETAILS.BATCH_CODE,
                                rec -> rec.into(com.salescode.dim.jooq.generated.tables.pojos.Productdetails.class)
                        );

        List<ProductDetails> itemsToInsert = new ArrayList<>();
        List<ProductDetails> itemsToUpdate = new ArrayList<>();

        for (ProductDetails product : productDetailsList) {

            ProductDetails existing = ProductDetails.of(savedMap.get(product.getBatchCode()));

            fillAttributes(existing, product);
            fillCommonAttributes(product);

            if (existing == null) {
                product.setId(new IdGenerator(product.getClass().getSimpleName()).getId(product));
                product.setVersion(0);
                product.setOperationPerformed(ActionType.INSERT);
                product.setChanged((byte)1);

                super.addHash(product);

                itemsToInsert.add(product);
                applyDefaults(product);

            } else {
                product.setId(existing.getId());
                product.setVersion(existing.getVersion() + 1);

                super.addHash(product);

                if (!Objects.equals(product.getHash(), existing.getHash())) {
                    product.setChanges(CdmDiffUtil.getChanges(product, existing));
                    product.setOperationPerformed(ActionType.UPDATE);
                    product.setChanged((byte)1);

                    itemsToUpdate.add(product);
                }
            }
        }

        result.add(itemsToInsert);
        result.add(itemsToUpdate);
        return result;
    }

    private void processFileNames(ProductDetails productDetails) {
        for (String column : fileNameColumns) {
            try {
                String value = (String) new PropertyDescriptor(column, ProductDetails.class).getReadMethod().invoke(productDetails);

                if (value != null && !value.isBlank()) {
                    String filesimplename = getSimpleFileNameWithExtension(value);
                    if (filesimplename != null) {
                        filesimplename = Optional.of(FilenameUtils.getBaseName(filesimplename).toLowerCase()).orElse(null);
                    }

                    // Use reflection to set the property value
                    new PropertyDescriptor(column, ProductDetails.class).getWriteMethod().invoke(productDetails, filesimplename);
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException |
                     IntrospectionException e) {
                LOG.error("Error processing file name for column: " + column, e);
            }
        }
    }

    public void fillBatchCode(ProductDetails product) {
        if (product.getBatchCode() == null) {
            List<String> keys = getBatchKeys();
            if (!keys.isEmpty()) {
                StringBuilder buffer = new StringBuilder();
                for (String key : keys) {
                    if (buffer.length() == 0) {
                        buffer.append(getFieldValue(key, product));
                    } else {
                        buffer.append(BATCH_CODE_SEPARATOR).append(getFieldValue(key, product));
                    }
                }
                product.setBatchCode(buffer.toString().toLowerCase().replaceAll("[ .]", ""));
            } else {
                LOG.error("Neither batch code config found in metadata nor it's present in input data. Please check.");
            }
        }
    }

    @Override
    public Collection<ProductDetails> batchSave(Collection<ProductDetails> productDetailsList) {
        LOG.info("Size of list is {}", productDetailsList.size());

        List<List<ProductDetails>> saveItemsList = getDataToSaveList(new ArrayList<>(productDetailsList));

        if (!saveItemsList.get(0).isEmpty()) {
            getDslContext().batchInsert(saveItemsList.get(0).stream().map(product -> getDslContext().newRecord(CK_PRODUCTDETAILS, product)).collect(Collectors.toList())).execute();
            saveProductMetadata(saveItemsList.get(0), true);
        }

        if (!saveItemsList.get(1).isEmpty()) {
            getDslContext().batchUpdate(saveItemsList.get(1).stream().map(product -> getDslContext().newRecord(CK_PRODUCTDETAILS, product)).collect(Collectors.toList())).execute();
            saveProductMetadata(saveItemsList.get(1), false);
        }
        LOG.info("Batch save for product details is successful");
        return productDetailsList;
    }

    private List<String> getBatchKeys() {
        return List.of("batchCode");
    }

    private Object getFieldValue(String fieldName, ProductDetails product) {
        try {
            return new PropertyDescriptor(fieldName, ProductDetails.class).getReadMethod().invoke(product);
        } catch (Exception e) {
            LOG.error("Error getting field value for: " + fieldName, e);
            return "";
        }
    }

    public void saveProductMetadata(Collection<ProductDetails> productDetailsList, boolean insert) {

        for (ProductDetails pd : productDetailsList) {
            List<ProductMetaData> metaList = pd.getProductMetaData();
            if (metaList == null || metaList.isEmpty()) {
                continue;
            }
            IdGenerator generator = new IdGenerator(metaList.get(0).getClass().getSimpleName());
            Map<String, Productmetadata> existingMetaMap = getDslContext().selectFrom(CK_PRODUCTMETADATA).where(CK_PRODUCTMETADATA.BATCH_CODE.eq(pd.getBatchCode())).fetch().map(rec -> rec.into(Productmetadata.class))   // convert record to POJO
                    .stream().collect(Collectors.toMap(Productmetadata::getId, m -> m));

            if (metaList.isEmpty()) {
                continue;
            }
            for (Productmetadata meta : metaList) {
                if (existingMetaMap.get(meta.getId()) == null) {
                    meta.setId(generator.getId(meta));
                }
                Productmetadata existing = existingMetaMap.get(meta.getId());
                if (existing == null) {
                    meta.setVersion(0);
                } else {
                    meta.setVersion(existing.getVersion() + 1);
                }
            }
            populateBatchLocation(metaList);
            if (insert) {
                getDslContext().batchInsert(metaList.stream().map(m -> getDslContext().newRecord(CK_PRODUCTMETADATA, m)).collect(Collectors.toList())).execute();
            } else {
                getDslContext().batchUpdate(metaList.stream().map(m -> getDslContext().newRecord(CK_PRODUCTMETADATA, m)).collect(Collectors.toList())).execute();
            }

            LOG.info("Batch save for product metadata is successful");
        }
    }

    private void populateBatchLocation(List<ProductMetaData> metaDataList) {
        List<Location> locationList = metaDataList.stream().map(ProductMetaData::getLocation).collect(Collectors.toList());
        List<Location> savedList = locationService.findLocationOrPersistLocation(locationList);
        for (int i = 0; i < metaDataList.size(); i++) {
            metaDataList.get(i).setLocationHierarchy(savedList.get(i).getLocationHierarchy());
        }
    }

    public ProductDetails findBySkuCode(String skuCode) {
        if (skuCode == null || skuCode.isBlank()) {
            throw new IllegalArgumentException("SKU Code cannot be null or empty");
        }

        try {
            return getDslContext()
                    .selectFrom(CK_PRODUCTDETAILS)
                    .where(CK_PRODUCTDETAILS.SKU_CODE.eq(skuCode))
                    .limit(1)
                    .fetchOptionalInto(com.salescode.dim.jooq.generated.tables.pojos.Productdetails.class)
                    .map(ProductDetails::of)
                    .orElse(null);

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to fetch ProductDetails for SKU: " + skuCode, e);
        }
    }

    public ProductDetails findByBatchCode(String batchCode) {
        if (batchCode == null || batchCode.isBlank()) {
            throw new IllegalArgumentException("Batch code cannot be null or empty");
        }

        try {
            return getDslContext()
                    .selectFrom(CK_PRODUCTDETAILS)
                    .where(CK_PRODUCTDETAILS.BATCH_CODE.eq(batchCode))
                    .limit(1)
                    .fetchOptionalInto(com.salescode.dim.jooq.generated.tables.pojos.Productdetails.class)
                    .map(ProductDetails::of)
                    .orElse(null);

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to fetch ProductDetails for Batch Code: " + batchCode, e);
        }
    }

    /**
     * Replicates the 'blobKeyValue' query to find blob_key by file_name.
     */
    public List<Map<String, Object>> findBlobKeyByFileName(String fileName) {

        Record result = getDslContext()
                .select(
                        CK_PRODUCTDETAILS.FILE_NAME,
                        CK_PRODUCTDETAILS.BLOB_KEY
                )
                .from(CK_PRODUCTDETAILS)
                .where(CK_PRODUCTDETAILS.BLOB_KEY.isNotNull())
                .and(CK_PRODUCTDETAILS.BLOB_KEY.ne(""))
                .and(CK_PRODUCTDETAILS.FILE_NAME.eq(fileName))
                .limit(1)
                .fetchOne();

        return result != null
                ? List.of(result.into(Map.class))
                : Collections.emptyList();
    }

    /**
     * Replicates the 'productImageDataQuery' to fetch product data by MSKU and (optionally) SSKU.
     */
    public List<Map<String, Object>> findProductImageData(String msku, String ssku) {

        Condition condition = CK_PRODUCTDETAILS.MARKET_SKU_CODE.eq(msku);

        if (ssku != null && !ssku.isBlank()) {
            condition = condition.and(CK_PRODUCTDETAILS.SKU_CODE.eq(ssku));
        }

        return getDslContext()
                .select(
                        CK_PRODUCTDETAILS.EXTENDED_ATTRIBUTES,
                        CK_PRODUCTDETAILS.BLOB_KEY,
                        CK_PRODUCTDETAILS.SKU_CODE,
                        CK_PRODUCTDETAILS.BATCH_CODE,
                        CK_PRODUCTDETAILS.FILE_NAME
                )
                .from(CK_PRODUCTDETAILS)
                .where(condition)
                .fetchMaps();
    }

    private void applyDefaults(ProductDetails p) {
        if (p.getMrp() == null) p.setMrp(BigDecimal.ZERO);
        if (p.getCaseMrp() == null) p.setCaseMrp(BigDecimal.ZERO);
        if (p.getOtherUnitMrp() == null) p.setOtherUnitMrp(BigDecimal.ZERO);
        if (p.getCaseToOtherUnitQuantity() == null) p.setCaseToOtherUnitQuantity(BigDecimal.ZERO);
        if (p.getCaseToPieceQuantity() == null) p.setCaseToPieceQuantity(BigDecimal.ZERO);
        if (p.getOtherUnitToPieceQuantity() == null) p.setOtherUnitToPieceQuantity(BigDecimal.ZERO);
        if (p.getPieceToOtherUnitQuantity() == null) p.setPieceToOtherUnitQuantity(BigDecimal.ZERO);
        if (p.getPriority() == null) p.setPriority(0);
        if(p.getPieceToVolume() == null) p.setPieceToVolume(BigDecimal.ZERO);
        if(p.getSkuCaseWeight() == null) p.setSkuCaseWeight(0.0);
        if(p.getSkuPieceWeight() == null) p.setSkuPieceWeight(0.0);
        if(p.getSkuOtherWeight() == null) p.setSkuOtherWeight(0.0);
    }
}