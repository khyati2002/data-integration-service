package com.salescode.channelkart.cache;

import com.salescode.channelkart.abstractdatasource.AbstractDataSourceConstants;
import com.salescode.channelkart.abstractdatasource.DatabaseProfileRegistry;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.repository.NativeCDMMapper;
import com.salescode.channelkart.security.Function;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.utils.JdbcUtils;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.persistence.Table;
import javax.sql.DataSource;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AllLOBRouter {
    private AllLOBRouter(){}
    private static Logger logger = LoggerFactory.getLogger(AllLOBRouter.class);
    
    private static final Map<String,Map<Class<? extends CommonDataModel>,List<CommonDataModel>>> registry = new HashMap<>();

    static NativeCDMMapper nm = new NativeCDMMapper();
    public static List<CommonDataModel> loadAll(Class<? extends CommonDataModel> type, Predicate<String> predicate){
        String tableName = getTableName(type);
        List<CommonDataModel> finalList = new ArrayList<>();
        if(tableName!=null && !tableName.isEmpty()) {
            if(predicate==null){
                DatabaseProfileRegistry.getDataSourceHashMap().forEach((key, value) -> {
                    if(AbstractDataSourceConstants.DEFAULT.equals(key)) {
                        logger.info("Skipping loading registry for default / lob datasource");
                        return;
                    }
                    try {
                            loadConfiguration(tableName,key.toString(),type,(DataSource) value,finalList);
                    } catch (Exception e) {
                        logger.info("loadAll: Error while loading data for " + key, e);
                    }
                    updateRegistry(finalList,type,key.toString());
                });
            }else{
                loadForPredicate(predicate,tableName,type,finalList);
            }
        }
        return finalList;
    }

    private static void updateRegistry(List<CommonDataModel> finalList, Class<? extends CommonDataModel> type, String key) {
    	try {
	    	List<CommonDataModel> lobCollection = finalList.stream().filter(c-> c.getLob() !=null && key!=null && c.getLob().equals(key) ).collect(Collectors.toList());
	        registry.computeIfAbsent(key, a->new HashMap<>()).put(type, lobCollection);
    	}catch (Exception e) {
    		logger.error("loadAll: Error while  updating internal registry " + key, e);
		}
	}
    
    private static void getRegistry(List<CommonDataModel> finalList, Class<? extends CommonDataModel> type, String key) {
    	try {
    		Map<Class<? extends CommonDataModel>, List<CommonDataModel>> lobMap = registry.computeIfAbsent(key, a->new HashMap<>());
        	if(finalList.isEmpty() && lobMap.containsKey(type) && !lobMap.get(type).isEmpty()) {
        		finalList.addAll(registry.get(key).get(type));
        	}
    	}catch (Exception e) {
    		logger.error("loadAll: Error while  getting internal registry data " + key, e);
		}
	}
    

    private static void loadForPredicate(Predicate<String> predicate,String tableName,Class<? extends CommonDataModel> type,List<CommonDataModel> finalList){
        if((predicate.test("root") || predicate.test("default"))){
            DataSource ds =  DatabaseProfileRegistry.getDefaultDs();
            loadConfiguration(tableName,"root",type,ds,finalList);
        }else{
            DatabaseProfileRegistry.getDataSourceHashMap().forEach((key, value) -> {
            	if (predicate.test(key.toString())) {
	                try {
	                	loadConfiguration(tableName,key.toString(),type,(DataSource) value,finalList);
	                } catch (Exception e) {
	                    logger.info("loadForPredicate: Error while loading data for " + key, e);
	                    getRegistry(finalList,type,key.toString());
	                }
	                updateRegistry(finalList,type,key.toString());
            	}
            });

        }
    }

    private static void loadConfiguration(String tableName,String key,Class<? extends CommonDataModel> type,DataSource ds,List<CommonDataModel> finalList){
        JdbcTemplate jdbcTemplate = JdbcUtils.createJdbcTemplate(getDatasourcebyProxyVanguard(ds));
        List<CommonDataModel> enrichMents = jdbcTemplate
            .query("select * from " + tableName, (rs, rowNum) -> {
                HashMap<String, String> dataMap = nm.getDataMap(rs);
                CommonDataModel pd = null;
                try {
                    pd =  nm
                        .mapFields(dataMap, type, new HashMap<>());
                    pd.setLob(key);
                } catch (Exception e) {
                    logger.error("Could not initialize the db configuration for lob:{}", key, e);
                }
                return pd;
            });
        finalList.addAll(enrichMents);
    }


    public static List<String> allLobs(){
        List<String> lobNames = new ArrayList<>();
        Map<Object,Object> datasourcemap= DatabaseProfileRegistry.getDataSourceHashMap();
        Iterator<Object> iter=datasourcemap.keySet().iterator();
        while(iter.hasNext()) {
            try {
                String lob = String.valueOf(iter.next());
                lobNames.add(lob);
            }catch (Exception e){
                logger.error("stacktrace", e);
            }
        }
        return lobNames;
    }

    private static String getTableName(Class<? extends CommonDataModel> c){
       return c.isAnnotationPresent(Table.class) ? (c.getAnnotation(Table.class)).name():null;
    }


    public static DataSource getDatasourcebyProxyVanguard(DataSource dataSource){
        String proxyUrlPortVanguard = System.getProperty("proxyUrlPortVanguard",null);
        if(proxyUrlPortVanguard!=null) {
            HikariDataSource bds = (HikariDataSource) dataSource;
            String[] be = bds.getJdbcUrl().split("/");
            String newUrl = be[0] + "//" + proxyUrlPortVanguard + "/" + be[3];
            bds.setJdbcUrl(newUrl);
            return dataSource;
        }
        return dataSource;
    }
}
