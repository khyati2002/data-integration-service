/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.services;


import com.salescode.channelkart.cache.AppCacheManager;
import com.salescode.channelkart.cache.DistributedCache;
import com.salescode.channelkart.models.enums.RoleName;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.utils.NullUtils;
import com.salescode.channelkart.utils.TimerUtils;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.channelkart.repository.UserRepository;
import com.salescode.dataintegration.etl.enums.OperationType;
import com.salescode.jooq.CkSupplierMetadata;
import com.salescode.jooq.generated.tables.pojos.*;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserService extends AbstractCDMService<CkUser> {

	private static final Logger logger = LoggerFactory.getLogger(UserService.class);

	private static final String LOG_TYPE = "GENERAL";

	public static final String CACHE_DOMAIN="users";

	public static final String CHANNEL_CACHE = "users_channel";

	private static final String DEFAULT_PASSWORD_DOMAIN_NAME="password";

	private static final String DEFAULT_PASSWORD_DOMAIN_TYPE="user";

	private static final String UNAUTHORIZED_USER="Unauthorized user";

	public static final String DEFAULT_LOCATION_FIELD="locationHierarchy";

	public static final String NORMALIZED_CHARECTORS = "U";

	public static final String NORMALIZED_JOINING_CHARECTORS = "U>U";

	public static final String TEST_USER_STARTS_WITH = "test";

	private static final String MULTIPLE_USER_FOUND = "Got multiple users for single login ";

	public static final String DEFAULT_ERROR_MESSAGE = "invalid username or password";
	private static final String LOGIN_ID = "loginId";
	public static final String RETAILER= "retailer";

	@Autowired private UserRepository userRepository;


	private final RoleService roleService;

	@Autowired private HierarchyMetaDataService hierarchyMetaDataService;
//
	private UserParentService userparentservice;

	private final DSLContext dsl;

	private MetaDataService metadataservice;

	private final PasswordEncoder encoder= new BCryptPasswordEncoder();

	@Autowired
	private SupplierMetaDataService supplierMetaDataService;

	@Autowired
	private DistributedCache distributedCache;


	public static final String DEFAULT_PASSWORD="@1234";

	public static final String DEFAULT_ENCODED_PASSWORD="$2a$10$GetnNjgilfLkIv.2R3nHMevLZfI9HGHWQ3iXw3nrCfJlrpePirkIi";


	public UserService(HierarchyMetaDataService hierarchyMetaDataService, RoleService roleService, UserParentService userparentservice, DSLContext dsl){
		this.hierarchyMetaDataService = hierarchyMetaDataService;
		this.roleService = roleService;
		this.userparentservice = userparentservice;
		this.dsl = dsl;
	}


	public CkUser findByLoginId(String loginId,boolean cached) {
		return findByLoginId(loginId,true,true);
	}
	public CkUser findByLoginId(String loginId,boolean cached,boolean hierarchy) {
		String lob = SecurityContextUtils.getLob();
		Function<String,CkUser> function = (String lid)->{
			UserService service = SpringContext.getBean(UserService.class);
			return service.getLoadedUserObject(lid,hierarchy);
		};

		if(cached) {
			CkUser user = distributedCache.withCache(lob,CACHE_DOMAIN, loginId,function);
			if(user != null && StringUtils.isBlank(user.getHierarchy())) {
				return reloadCache(loginId);
			}
			return user;
		}

		return function.apply(loginId);
	}

	public CkUser reloadCache(String loginId) {
		String lob = SecurityContextUtils.getLob();
		Function<String,CkUser> function = (String lid)->{
			UserService service= SpringContext.getBean(UserService.class);
			return service.getLoadedUserObject(lid,true);
		};

		return  distributedCache.withCache(lob,CACHE_DOMAIN, loginId,function);
	}

	public CkUser getLoadedUserObject(String lid,boolean hierarchy) {

		CkUser u = userRepository.findByLoginId(lid);
		if (u != null) {
			loadUserAssociationObjects(u);
			if (hierarchy && u.getImmediateParent() == null) {
				TimerUtils
						.withTime("Time taken UserService hierarchyMetaDataService load ", () ->
								u.setImmediateParent(hierarchyMetaDataService.findParentThroughUserLoginId(lid))
						);
			}
		}
		return u;
	}

		public void clearCache (String lob, String loginId){
			if (org.apache.commons.lang.StringUtils.isNotBlank(loginId)) {
				distributedCache.clearCache(lob, CACHE_DOMAIN, loginId);
				SupplierInfoService supplierInfoService = SpringContext.getBean(SupplierInfoService.class);
				supplierInfoService.clearCache(lob, "u:" + loginId);
				supplierInfoService.clearCache(lob, "o:" + loginId);
				hierarchyMetaDataService.clearCache(lob, loginId);
				clearChannelCache(loginId);
			}
		}

		private void clearChannelCache (String loginId){
			AppCacheManager.getInstance().removeByDomain(SecurityContextUtils.getLob(), CHANNEL_CACHE + loginId);
			//AppCacheManager.getInstance().removeByDomain(SecurityContextUtils.getLob(), CHANNEL_CACHE + loginId + NotificationTypeRegistry.FIREBASE.name());

		}


		@Override
		public CkUser save (CkUser inUser){
			return this.save(inUser, OperationType.insert);
		}

		public void clearCache (String lob, CkUser user){
			if (user != null) {
				//clearCache(lob, user.getLoginid());
				if (user.getLocationHierarchy() != null) {
					//locationService.clearCache(lob,user.getLocationHierarchy().getLocationHierarchy());
				}
			}
		}


		public CkUser save(CkUser inUser, OperationType type){
			String lob = SecurityContextUtils.getLob();
			//CkUser user= TimerUtils.withTime("Time Taken to execute fillUser()", u-> fillUser(inUser));

			CkUser user = fillUser(inUser);
		//	clearCache(lob, user);
//			if (user.getRoles().size() == 1 && user.getRoles().stream().allMatch(desig -> desig.getName().equals(RoleName.ROLE_ADMIN.name()))) {
//				CkUserParent up = new CkUserParent();
//				up.setUserloginid(user.getLoginid());
//				up.setParent(null);
//				up.setLob(inUser.getLob());
//				//UserParent refreshedObj=TimerUtils.withTime("Time taken to refresh UserParent", s-> userparentservice.refresh(up));
//				CkUserParent refreshedObj = userparentservice.refresh(up);
//				//TimerUtils.withTime("Time taken to save UserParent", ()->
//				userparentservice.save(refreshedObj);
//				//);
//			}

			if (user.getImmediateParent() != null && !user.getImmediateParent().isEmpty()) {
				//TimerUtils.withTime("Time taken to save UserParent", ()->
				saveUserParent(user, type);
				//);
			}

			saveUser(user);
			CkUser savedObj = super.save(user);
			if (inUser.getSupplierMetaData() != null && !inUser.getSupplierMetaData().isEmpty()) {
				List<CkSupplierMetadata> supplierMetaInfo = user.getSupplierMetaData();
				if (!isSameSupplierMetada(savedObj, user)) {
					supplierMetaInfo.forEach(cdmObject -> {
						cdmObject.setUser(savedObj);
						cdmObject.setLob(user.getLob());
					});
					//TimerUtils.withTime("Time taken to batchSave SupplierMetadata of Size "+supplierMetaInfo.size(), ()->
					//supplierMetaDataService.batchSave(supplierMetaInfo));
					try {
						supplierMetaDataService.batchSave(supplierMetaInfo);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}

			}

			//AuditLogger.log(LOG_TYPE, "Created new User with loginId '{}'",user.getLoginId());
			clearCache(lob,user);

			return savedObj;
		}

		private CkUser saveUser (CkUser user){

			if (user.getVerified() == null) user.setVerified((byte) 1);
			if (user.getVersion() == null) user.setVersion(1);
			if (user.getPassword() == null) user.setPassword(getDefaultEncryptedUserPassword());
			if (user.getId() == null) user.setId(UUID.randomUUID().toString());
			if (user.getBlocked() == null) user.setBlocked(false);
			return user;
		}
//
		public CkUser fillUser (CkUser user){
			if (NullUtils.isNotNull(user.getLocationHierarchy())) {
				try {
//				CkLocation loc= user.getLocation();
//				loc = locationService.findLocationOrPersistLocation(loc);
//				user.setLocationHierarchy(loc);
				} catch (Exception ex) {
					throw new IllegalStateException("Error occured while setting location for user: " + user.getLoginid(), ex);
				}
			} else {
				//	throw new IllegalStateException("Missing location data. Data cannot be saved without location information for user : "+user.getLoginid());
			}
			if (user.getRoles() == null || user.getRoles().isEmpty()) {
				List<CkAuthRole> roles = roleService.getRoleAsList(RoleName.ROLE_USER.name());
				user.setRoles(roles);
			} else {
				List<CkAuthRole> roles = new ArrayList<>();
				for (CkAuthRole objectRole : user.getRoles()) {
					roleService.getRole(objectRole.getName()).ifPresent(elem -> {
						if (!roles.contains(elem)) {
							roles.add(elem);
						}
					});
				}
				user.setRoles(roles);
			}

			if (user.getSupplierMetaData() != null) {
				user.getSupplierMetaData().forEach(s ->
				{
					supplierMetaDataService.fillCommonAttributes(s);
					s.setUser(user);
				});
			}

			if (user.getVerified() == null) {
				user.setVerified((byte) 0);
			}

			if (user.getPassword() == null) {
				user.setPassword(DEFAULT_ENCODED_PASSWORD);
			}
//			if(!user.getBlocked()){
//				user.setBlocked(false);
//			}

			return user;
		}

//
		public String getDefaultEncryptedUserPassword () {
			try {
//			CkMetadata metadata= metadataservice.fetchByValue(DEFAULT_PASSWORD_DOMAIN_NAME, DEFAULT_PASSWORD_DOMAIN_TYPE);
//			String rawPassword;
//			if(metadata == null) {
//				rawPassword= DEFAULT_PASSWORD;
//			}else {
//				ArrayNode arraynode= (ArrayNode) metadata.getDomainValues();
//				if(arraynode == null || arraynode.size()==0) {
//					throw new Exception("System has found metadata resource for default password but seems misconfigured. Please check configuration.");
//				}
//				JsonNode node= arraynode.get(0);
//				if(!node.has("default")) {
//					throw new Exception("System has found metadata resource for default password but 'default' key not found. Please check configuration.");
//				}
//				rawPassword= node.get("default").textValue();
//			}
//			if(StringUtils.equals(DEFAULT_PASSWORD,rawPassword)){
				return DEFAULT_ENCODED_PASSWORD;
//			}else {
//				//return TimerUtils.withTime("time taken to encode password", () -> encoder.encode(rawPassword));
//				return encoder.encode(rawPassword);
//			}
			} catch (Exception ex) {
				throw new RuntimeException("Error");
			}

		}

		private static boolean staleRecords
		(Set < String > existingParents, List < CkHierarchyMetadata > immediateParents){
			Set<String> hmlist = immediateParents.stream().map(CkHierarchyMetadata::getParent).collect(Collectors.toSet());
			return !existingParents.equals(hmlist);
		}

		public void saveUserParent (CkUser user, OperationType type){
			Set<CkUserParent> userParents = new HashSet<>();
			List<CkUserParent> dbParents = userparentservice.findByUserLoginId(user.getLoginid());
			if (dbParents != null && !dbParents.isEmpty()) {
				Set<String> dataset = dbParents.stream().map(CkUserParent::getParent).collect(Collectors.toSet());
				if (staleRecords(new HashSet<>(dataset), user.getImmediateParent())) {
					userParents.addAll(getNewUserParents(user, dataset, type));
					if (type.equals(OperationType.insert) || user.getDesignation().contains(RETAILER)) {
						userparentservice.deleteByUserLoginId(user.getLoginid());
						//hierarchySynchronizer.removeUser(user.getLoginid());
					}
				}
			} else {
				userParents.addAll(getUserParents(user));
			}
			if (!userParents.isEmpty()) {
				try {
					userparentservice.batchSave(userParents);
					evaluateUserHierarchy(user, userParents);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}

		private void evaluateUserHierarchy (CkUser user, Set < CkUserParent > userParents){
			StringBuilder hierarchyStr = new StringBuilder();
			userParents.forEach(parent -> {
				List<CkHierarchyMetadata> hmList = (List<CkHierarchyMetadata>) hierarchyMetaDataService.findByImmediateParent(parent.getParent());
				if (hmList.isEmpty()) {
					hierarchyStr.append(user.getLoginid() + " > " + parent.getParent() + " > " + getCustomerAccountsService().getAdminLoginId());
					hierarchyStr.append(",");
				} else {
					hmList.forEach(hierarchyMetaData -> {
						hierarchyStr.append(user.getLoginid() + " > " + hierarchyMetaData.getHierarchy());
						hierarchyStr.append(",");
					});
				}
			});
			user.setHierarchy(hierarchyStr.substring(0, hierarchyStr.length() - 1));
			user.setNormalizedHierarchy(UserService.getNormalizedHierarchy(user.getHierarchy()));
		}

		private CustomerAccountsService getCustomerAccountsService () {
			return SpringContext.getBean(CustomerAccountsService.class);
		}

		private List<CkUserParent> getNewUserParents (CkUser user, Set < String > dataset, OperationType type){
			List<CkUserParent> userParentList = new ArrayList<>();
			for (CkHierarchyMetadata hm : user.getImmediateParent()) {
				if (!dataset.contains(hm.getParent()) || type.equals(OperationType.insert)) {
					CkUserParent up = new CkUserParent();
					up.setUserloginid(user.getLoginid());
					up.setParent(hm.getParent());
					if (up.getUserloginid().equalsIgnoreCase(up.getParent())) {
						//	throw new UnexpectedResultException("User can't be mapped to itself. Found a record for user " + up.getUserLoginId() + " mapped to itself. Please verify the data once.");
					}
					userParentList.add(up);
				}
			}
			return userParentList;
		}
//
		private List<CkUserParent> getUserParents (CkUser user){
			List<CkUserParent> userParentList = new ArrayList<>();
			for (CkHierarchyMetadata hm : user.getImmediateParent()) {
				CkUserParent up = new CkUserParent();
				up.setUserloginid(user.getLoginid());
				up.setParent(hm.getParent());
				up.setId(UUID.randomUUID().toString());
				if (up.getUserloginid().equalsIgnoreCase(up.getParent())) {
					//	throw new UnexpectedResultException("User can't be mapped to itself. Found a record for user " + up.getUserLoginId() + " mapped to itself. Please verify the data once.");
				}
				userParentList.add(up);
			}
			return userParentList;
		}

		private String getUserParentList (Set < CkUserParent > userParents) {
			StringBuilder userParentList = new StringBuilder();
			for (CkUserParent parent : userParents) {
				userParentList.append(parent.getParent() + ", ");
			}
			return userParentList.toString();
		}
//
		private static boolean isSameSupplierMetada (CkUser user1, CkUser user2){

			if ((user1.getSupplierMetaData() == null || user1.getSupplierMetaData().isEmpty()) && (user2.getSupplierMetaData() == null || user2.getSupplierMetaData().isEmpty())) {
				return true;

			} else if (user1.getSupplierMetaData() != null && !user1.getSupplierMetaData().isEmpty()) {

				List<CkSupplierMetadata> spms1 = user1.getSupplierMetaData();

				if (user2.getSupplierMetaData() == null || user2.getSupplierMetaData().isEmpty()) {

					return false;
				} else {

					List<CkSupplierMetadata> spms2 = user2.getSupplierMetaData();


					if (spms2.size() != spms1.size()) {

						return false;
					} else {
						try {

							return spms1.stream().allMatch(s -> s.getUser() != null && spms2.contains(s));

						} catch (Exception e) {
							logger.error("stacktrace", e);
						}
						return false;
					}

				}

			} else {

				return false;
			}

		}

		public static String getNormalizedHierarchy (String hierarchy){
			if (StringUtils.isBlank(hierarchy)) {
				return hierarchy;
			}
			String normalizedHierarchy = NORMALIZED_CHARECTORS + Arrays.asList(hierarchy.split(","))
					.stream().map(h -> Arrays.asList(h.split(" > "))).flatMap(List::stream)
					.collect(Collectors.toSet()).stream()
					.collect(Collectors.joining(NORMALIZED_JOINING_CHARECTORS)) + NORMALIZED_CHARECTORS;
			return removeSpecialCharacters(normalizedHierarchy);
		}

		private static String removeSpecialCharacters (String normalizedHierarchy){
			String exludedCharactors = getExludedCharactors();
			try {
				String str = normalizedHierarchy.replaceAll(exludedCharactors, "");
				return str;
			} catch (Exception e) {
				logger.error("Exception happend while removing special charactors {} in normalized hierarchy {}", exludedCharactors, normalizedHierarchy);
				return normalizedHierarchy;
			}
		}

		public static String getExludedCharactors () {
			return System.getProperty("excludeCharNormalizedHierarchy", "[^a-zA-Z0-9>]");
		}

		private void loadUserAssociationObjects(CkUser u){
			TimerUtils
					.withTime("Time taken UserService Association load ", () -> {
						if (u.getRoles() != null) u.getRoles().size();
						if (u.getSupplierMetaData() != null) u.getSupplierMetaData().size();
						if (u.getDesignation() != null) u.getDesignation().size();
						//	if (u.getMessengerinfo() != null) u.getMessengerInfo().size();
					});

		}

	}