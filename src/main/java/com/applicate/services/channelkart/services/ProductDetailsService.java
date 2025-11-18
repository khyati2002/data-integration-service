package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.utils.IdGenerator;
import com.applicate.services.channelkart.utils.NullUtils;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import com.salescode.dim.jooq.generated.tables.pojos.Productmetadata;
import com.salescode.dim.jooq.impl.Location;
import com.salescode.dim.jooq.impl.ProductDetails;
import com.salescode.dim.jooq.impl.ProductMetaData;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_PRODUCTDETAILS;
import static com.salescode.dim.jooq.generated.tables.CkProductmetadata.CK_PRODUCTMETADATA;

public class ProductDetailsService extends AbstractCDMService<ProductDetails> {
    private static final Logger LOG = LoggerFactory.getLogger(ProductDetailsService.class);

    private final List<String> fileNameColumns = Arrays.asList("fileName", "fileName_a", "fileName_b", "fileName_c", "fileName_f", "fileName_l");

    public static final String BATCH_CODE_SEPARATOR = "-";
    private final LocationService locationService;

    public ProductDetailsService() {
        this.locationService = new LocationService();
    }


    public List<List<ProductDetails>> getDataToSaveList(List<ProductDetails> productDetailsList) {
        List<List<ProductDetails>> result = new ArrayList<>();

        for (ProductDetails productDetails : productDetailsList) {
            productDetails.setId(new IdGenerator(productDetails.getClass().getSimpleName()).getId(productDetails));
            productDetails.setChanged(true);
            productDetails.setActiveStatus(ActiveStatus.ACTIVE);
            fillBatchCode(productDetails);
            processFileNames(productDetails);
        }
        if (productDetailsList.isEmpty()) {
            result.add(new ArrayList<>());
            result.add(new ArrayList<>());
            return result;
        }
        List<String> batchCodes = productDetailsList.stream()
                .map(ProductDetails::getBatchCode)
                .collect(Collectors.toList());

        Map<String, com.salescode.dim.jooq.generated.tables.pojos.Productdetails> savedList =
                getDslContext().selectFrom(CK_PRODUCTDETAILS)
                        .where(CK_PRODUCTDETAILS.BATCH_CODE.in(batchCodes))
                        .fetch()
                        .intoMap(CK_PRODUCTDETAILS.BATCH_CODE,
                                rec -> rec.into(com.salescode.dim.jooq.generated.tables.pojos.Productdetails.class));

        List<ProductDetails> itemsToInsert = new ArrayList<>();
        List<ProductDetails> itemsToUpdate = new ArrayList<>();

        for (ProductDetails product : productDetailsList) {
            fillAttributes(product, ProductDetails.of(savedList.get(product.getBatchCode())));
            fillCommonAttributes(product);

            if (savedList.get(product.getBatchCode()) == null) {
                product.setVersion(0);
                product.setOperationPerformed(ActionType.INSERT);
                itemsToInsert.add(product);
            } else {
                ProductDetails existingProduct = ProductDetails.of(savedList.get(product.getBatchCode()));
                product.setVersion(existingProduct.getVersion() + 1);
                product.setOperationPerformed(ActionType.UPDATE);
                itemsToUpdate.add(product);
            }
        }

        result.add(itemsToInsert);
        result.add(itemsToUpdate);
        return result;
    }
    private void processFileNames(ProductDetails productDetails) {
        for (String column : fileNameColumns) {
            try {
                String value = (String) new PropertyDescriptor(column, ProductDetails.class)
                        .getReadMethod().invoke(productDetails);

                if (value != null && !value.isBlank()) {
                    String filesimplename = getSimpleFileNameWithExtension(value);
                    if (filesimplename != null) {
                        filesimplename = Optional.of(FilenameUtils.getBaseName(filesimplename).toLowerCase())
                                .orElse(null);
                    }

                    // Use reflection to set the property value
                    new PropertyDescriptor(column, ProductDetails.class)
                            .getWriteMethod().invoke(productDetails, filesimplename);
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                     | IntrospectionException e) {
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
                        buffer.append(BATCH_CODE_SEPARATOR ).append(getFieldValue(key, product));
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
            getDslContext().batchInsert(saveItemsList.get(0).stream()
                    .map(product -> getDslContext().newRecord(CK_PRODUCTDETAILS, product))
                    .collect(Collectors.toList())).execute();
            saveProductMetadata(saveItemsList.get(0),true);
        }

        if (!saveItemsList.get(1).isEmpty()) {
            getDslContext().batchUpdate(saveItemsList.get(1).stream()
                    .map(product -> getDslContext().newRecord(CK_PRODUCTDETAILS, product))
                    .collect(Collectors.toList())).execute();
            saveProductMetadata(saveItemsList.get(1),false);
        }
        LOG.info("Batch save for product details is successful");
        return productDetailsList;
    }

    private List<String> getBatchKeys() {
        return List.of("batchCode");
    }

    private Object getFieldValue(String fieldName, ProductDetails product) {
        try {
            return new PropertyDescriptor(fieldName, ProductDetails.class)
                    .getReadMethod().invoke(product);
        } catch (Exception e) {
            LOG.error("Error getting field value for: " + fieldName, e);
            return "";
        }
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
    public void saveProductMetadata(Collection<ProductDetails> productDetailsList, boolean insert ) {

        for (ProductDetails pd : productDetailsList) {
            List<ProductMetaData> metaList = pd.getProductMetaData();
            IdGenerator generator = new IdGenerator(metaList.get(0).getClass().getSimpleName());

            Map<String, Productmetadata> existingMetaMap = getDslContext().selectFrom(CK_PRODUCTMETADATA)
                    .where(CK_PRODUCTMETADATA.BATCH_CODE.eq(pd.getBatchCode()))
                    .fetch()
                    .map(rec -> rec.into(Productmetadata.class))   // convert record to POJO
                    .stream()
                    .collect(Collectors.toMap(Productmetadata::getId, m -> m));

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
                    getDslContext().batchInsert(
                            metaList.stream()
                                    .map(m -> getDslContext().newRecord(CK_PRODUCTMETADATA, m))
                                    .collect(Collectors.toList())
                    ).execute();
                }else
                {
                     getDslContext().batchUpdate(
                        metaList.stream()
                                .map(m -> getDslContext().newRecord(CK_PRODUCTMETADATA, m))
                                .collect(Collectors.toList())
                    ).execute();
                  }

            LOG.info("Batch save for product metadata is successful");
        }
    }

    private void populateBatchLocation(List<ProductMetaData> metaDataList) {
        List<Location> locationList = metaDataList.stream()
                .map(ProductMetaData::getLocation)  // Assuming there's a getLocation() method// Filter out null locations
                .collect(Collectors.toList());
        List<Location> savedList = locationService.findLocationOrPersistLocation(locationList);
        for (int i = 0; i < metaDataList.size(); i++) {
            metaDataList.get(i).setLocationHierarchy(savedList.get(i).getLocationHierarchy());
        }
    }

    public String fetchTaxGroupCode(Map<String, Object> inputMap, String sourceSuffix) {
        Condition condition;
        if (NullUtils.isNotNull(inputMap.get("ITEM_ID")) && !ObjectUtils.isEmpty(inputMap.get("ITEM_ID"))) {
            String itemId = inputMap.get("ITEM_ID").toString();
            condition = CK_PRODUCTDETAILS.ITEM_ID.eq(itemId.trim() + sourceSuffix);
        } else if (NullUtils.isNotNull(inputMap.get("BATCH_CODE")) && !ObjectUtils.isEmpty(inputMap.get("BATCH_CODE"))) {
            String batchCode = inputMap.get("BATCH_CODE").toString();
            condition = CK_PRODUCTDETAILS.BATCH_CODE.eq(batchCode.trim());
        } else {
            throw new DataTransformationService.TransformationException("producthierarchycode and itemCode both cannot be empty, please provide one of them");
        }

        // Use jOOQ DSL to build and execute the query safely
        Result<Record1<JSON>> result = getDslContext()
                .select(DSL.field("JSON_EXTRACT(extended_attributes, '$.taxgroupcode')", JSON.class).as("taxgroupcode"))
                .from(CK_PRODUCTDETAILS)
                .where(condition)
                .fetch();

        if (result.isEmpty()) {
            return null;
        } else {
            JSON taxGroupJson = result.get(0).get("taxgroupcode", JSON.class);
            if (taxGroupJson != null) {
                return taxGroupJson.data();
            } else {
                return null;
            }
        }
    }

    public Map<String, Object> findProductDetailsBySkuCode(String skuCode) {
        Result<Record4<String, String, String, String>> result = getDslContext().select(
                        CK_PRODUCTDETAILS.BATCH_CODE,
                        CK_PRODUCTDETAILS.CATEGORY.as("category"),
                        CK_PRODUCTDETAILS.BRAND,
                        CK_PRODUCTDETAILS.SKU_DESCRIPTION.as("sku_description")
                )
                .from(CK_PRODUCTDETAILS)
                .where(CK_PRODUCTDETAILS.SKU_CODE.eq(skuCode))
                .fetch();

        if (result.isEmpty()) {
            return null;
        }

        Record4<String, String, String, String> record = result.get(0);

        Map<String, Object> productMap = new HashMap<>();
        productMap.put("batch_code", record.get(CK_PRODUCTDETAILS.BATCH_CODE));
        productMap.put("category", record.get("category"));
        productMap.put("brand", record.get(CK_PRODUCTDETAILS.BRAND));
        productMap.put("sku_description", record.get("sku_description"));

        return productMap;
    }



}