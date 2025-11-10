package com.applicate.services.channelkart.utils;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.services.MetaDataService;
import com.applicate.services.channelkart.services.SalesService;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.dim.jooq.generated.tables.pojos.Metadata;
import com.salescode.dim.utils.ReflectionUtils;
import jakarta.activation.DataHandler;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class EntityUtils {

    private static volatile EntityUtils instance;
    private final transient DSLContext dslContext;
    private final Map<String, Class<? extends CommonDataModel>> entityImplClassMap = new ConcurrentHashMap<>();
    public static final String DYNAMIC_UNIQUE_KEY = "DynamicUniqueKey";
    private static final MetaDataService metadataService=new MetaDataService();
    private static final Logger LOG = LoggerFactory.getLogger(SalesService.class);


    Set<Class<? extends CommonDataModel>> subClasses = ReflectionUtils.findSubClasses(CommonDataModel.class);

    public EntityUtils(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public static EntityUtils getInstance(DSLContext dslContext) {
        if (instance == null) {
            synchronized (EntityUtils.class) {
                if (instance == null) {
                    instance = new EntityUtils(dslContext);
                }
            }
        }
        return instance;
    }

    public static EntityUtils getInstance() {
        if (instance == null) {
            throw new IllegalStateException("EntityUtils was not initialized");
        }
        return instance;
    }


    public Class<? extends CommonDataModel> getEntityClass(String entityName) {
        return entityImplClassMap.computeIfAbsent(entityName, key -> {
            List<Class<? extends CommonDataModel>> candidates = subClasses.stream()
                    .filter(e -> key.equalsIgnoreCase(e.getSimpleName())).collect(Collectors.toList());
            if (candidates.isEmpty()) {
                throw new IllegalArgumentException("Entity not found: " + key);
            }
            return candidates.stream().filter(e -> e.getPackage().getName().contains(".impl")).findFirst()
                    .orElse(candidates.get(0));
        });
    }

    public static String getMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            StringBuilder hashtext = new StringBuilder(no.toString(16));
            while (hashtext.length() < 32) {
                hashtext.append( "0" + hashtext);
            }
            return hashtext.toString();
        }
        catch (NoSuchAlgorithmException e) {
            LOG.info("no such algorithm",e.getMessage());
        }
    }

    public boolean checkGenerateMD5Hash(String entityName) {
        Metadata metaData = metadataService.fetchByValue(entityName, DYNAMIC_UNIQUE_KEY);

        boolean generateHash = false;
        if (metaData != null && metaData.getDomainValues()!=null) {
            JsonNode dynamicKeysNode = metaData.getDomainValues().get(0);
            if (dynamicKeysNode != null) {
                generateHash = dynamicKeysNode.has("generateHash") && dynamicKeysNode.get("generateHash").asBoolean();
            }
        }
        return generateHash;
    }

    public ArrayNode fetchDynamicPrimaryKeys(String entityName) {
        Metadata metaData=  metadataService.fetchByValue(entityName,DYNAMIC_UNIQUE_KEY);
        org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode columnArr = JSONUtils.getObjectMapper().createArrayNode();
        if(metaData!=null && metaData.getDomainValues()!=null) {
            var dynamicKeys=metaData.getDomainValues().get(0).get("dynamicKeys");
            if(dynamicKeys!=null) {
                columnArr = JSONUtils.convertToArrayNode(dynamicKeys);
            }
        }
        return columnArr;
    }

}