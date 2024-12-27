/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.salescode.channelkart.abstractdatasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.DataIntegrationApplication;
import com.salescode.channelkart.exceptions.CustomRuntimeException;
import com.salescode.channelkart.models.Profile;
import com.salescode.channelkart.repository.ProfileRepository;
import com.salescode.channelkart.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseProfileRegistry {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());


	private static Map<Object, Object> profileRegistry =new HashMap<>();

	private static Map<Object, Object> allProfileRegistry =new HashMap<>();

	private static DataSource defaultDatasource;

	@Value("${dbUpgrade:false}")
	private boolean isDbUpgrade;

	@Autowired
	private Environment env;

	@Autowired ProfileRepository profileRepository;
	
	@PostConstruct
	public void init() {
//		if (isDbUpgrade) {
//			return;
//		}
//		List<Profile> databaseProfile = profileService.findByTypeFromDb(AbstractDataSourceConstants.DATABASE);
//		for(int i=0;i<databaseProfile.size() ;i++) {
//			if(databaseProfile.get(i).getLob() !=null){
//				String lobs = env.getProperty("channelkart.lobs");
//				if(lobs!=null && !lobs.isEmpty()){
//					if(lobs.contains(databaseProfile.get(i).getLob())) {
//						registerProfile(databaseProfile.get(i));
//					}else{
//						logger.info("Ignore Loading of LOB {}",databaseProfile.get(i).getLob());
//					}
//				}else {
//					registerProfile(databaseProfile.get(i));
//				}
//			}
//		}
		Profile profile = new Profile();
		profile.setLob(env.getProperty("channelkart.lobs"));
		registerProfile(profile);
		registerDefault();
//		customRoutingDataSource.setTargetDataSources(allProfileRegistry);
//		customRoutingDataSource.afterPropertiesSet();
	}

	public void registerDatasource(String name, DataSource ds){
		registerDatasource(name, ds, "");
	}

	public void registerDatasource(String name, DataSource ds, String dbPrivilege){
		if(StringUtils.isEmpty(dbPrivilege)){
			profileRegistry.put(name,ds);
			allProfileRegistry.put(name,ds);
		}else{
			allProfileRegistry.put(getDBPrivilegeName(name,dbPrivilege),ds);
		}
//		customRoutingDataSource.afterPropertiesSet();
	}

	public static String getDBPrivilegeName(String name, String dbPrivilege){
		return name+"_"+dbPrivilege;
	}

	public void registerProfile(Profile profile){
		try {
			String profileKey = profile.getLob();
			logger.info("loading database for lob:{}", profileKey);
//			JsonNode attributes = profile.getAttributes();
//			DataSource ds = getDatasource(attributes);
//			String dbPrivilege = attributes.has("privilege") ? attributes.get("privilege").asText() : "";
			registerDatasource(profileKey.toLowerCase(), defaultDatasource,"");
		}catch (Exception e){
			logger.info("Error while load datasource",e);
		}
		
	}


	public static void setProfileRegistry(
			Map<Object, Object> profileRegistry) {
		DatabaseProfileRegistry.profileRegistry = profileRegistry;
	}

	public static Map<Object, Object> getDataSourceHashMap() {

        return profileRegistry;
    }
	
    
    private void registerDefault() {
    	if(defaultDatasource == null) {
           throw new CustomRuntimeException("'defaultDatasource' found null. It may not have initialized from CustomDataSource. Please check dependencies and initialization route");
    	}
    	registerDatasource(AbstractDataSourceConstants.DEFAULT, defaultDatasource);
    }

    public static synchronized void setDefaultDs(DataSource dataSource){
		defaultDatasource =  dataSource;
	}
    
    public static synchronized DataSource getDefaultDs(){
		return defaultDatasource;
	}
	
}