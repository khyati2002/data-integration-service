/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.dataintegration.etl.cdm.services;


import com.salescode.channelkart.converters.ActiveStatus;
import com.salescode.channelkart.models.diff.Change;
import com.salescode.channelkart.models.enums.RoleName;
import com.salescode.channelkart.services.SpringContext;
import com.salescode.channelkart.utils.CdmDiffUtil;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.channelkart.utils.NullUtils;
import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.dataintegration.etl.cdm.repository.UserRepository;
import com.salescode.dataintegration.etl.enums.OperationType;
import com.salescode.jooq.CkSupplierMetadata;
import com.salescode.jooq.generated.tables.pojos.*;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

;
import static com.salescode.jooq.generated.Tables.CK_USER_PARENT;
import static com.salescode.jooq.generated.tables.CkHierarchyMetadata.CK_HIERARCHY_METADATA;

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

	private RoleService roleService;
//
//	private LocationService locationService;
//
	@Autowired private HierarchyMetaDataService hierarchyMetaDataService;
//
	private UserParentService userparentservice;

	private final DSLContext dsl;
//
//	private MetaDataService metadataservice;
//
//	@Autowired
//	private UserMetadataRepository userMetadataRepository;
//
//	@Autowired
//	private UserSubscriptionService userSubscriptionService;
//
//	@Autowired
//	private OutletDetailsService outletDetailsService;
//
//	@Autowired
//	private PermissionEvaluator permissionEvaluator;
//
//	@Autowired
//    private PropertyRegistry propertyRegistry;
//
//	@Autowired
//	private HierarchySynchronizer hierarchySynchronizer;
//
//	private final PasswordEncoder encoder= new BCryptPasswordEncoder();
//
//	@Autowired
//	private EntityManager em;
//
	@Autowired
	private SupplierMetaDataService supplierMetaDataService;
//
//	@Autowired
//	private UserMetadataService userMetadataService;
//
//	public static final String RETAILER= "retailer";
//
//	@Autowired
//	private UserStatusService userStatusService;
//
//	private DistributedCache distributedCache;
//
//	/** The Constant DEFAULT_PASSWORD. */
//	public static final String DEFAULT_PASSWORD="@1234";
//
//	public static final String DEFAULT_ENCODED_PASSWORD="$2a$10$GetnNjgilfLkIv.2R3nHMevLZfI9HGHWQ3iXw3nrCfJlrpePirkIi";
//
//	private NativeEntityManager nem;
//
//	private AttributeUpdateOverrideManager attributeUpdateOverrideManager;
//
//
//public UserService(UserRepository repository, RoleService roleService, LocationService locationService, HierarchyMetaDataService hierarchyMetaDataService, UserParentService userparentservice,
//                       MetaDataService metadataservice, DistributedCache distributedCache, NativeEntityManager nem, AttributeUpdateOverrideManager attributeUpdateOverrideManager) {
//		super(repository);
//		this.userRepository=repository;
//		this.roleService=roleService;
//		this.locationService= locationService;
//		this.hierarchyMetaDataService = hierarchyMetaDataService;
//		this.userparentservice= userparentservice;
//		this.metadataservice= metadataservice;
//		this.distributedCache = distributedCache;
//		this.nem=nem;
//		this.attributeUpdateOverrideManager=attributeUpdateOverrideManager;
//
//	}

	public UserService(HierarchyMetaDataService hierarchyMetaDataService, RoleService roleService, UserParentService userparentservice, DSLContext dsl){
		this.hierarchyMetaDataService = hierarchyMetaDataService;
		this.roleService = roleService;
		this.userparentservice = userparentservice;
		this.dsl = dsl;
	}

//	public CkUser findByLoginId(String loginId) {
//		return findByLoginId(loginId,true,true);
//	}
//
//	/***
//	 *
//	 * @param loginId
//	 * @param cached indicate the use of caching
//	 * @return
//	 */
	public CkUser findByLoginId(String loginId,boolean cached) {
		return findByLoginId(loginId,true,true);
	}
	public CkUser findByLoginId(String loginId,boolean cached,boolean hierarchy) {
		//String lob = SecurityContextUtils.getLob();
		Function<String,CkUser> function = (String lid)->{
			UserService service = SpringContext.getBean(UserService.class);
			return service.getLoadedUserObject(lid,hierarchy);
		};

//		if(cached) {
//			User user = distributedCache.withCache(lob,CACHE_DOMAIN, loginId,function);
//			if(user != null && StringUtils.isBlank(user.getHierarchy())) {
//				return reloadCache(loginId);
//			}
//			return user;
//		}

		return function.apply(loginId);
	}
//
//	public User reloadCache(String loginId) {
//		String lob = SecurityContextUtils.getLob();
//		Function<String,User> function = (String lid)->{
//			UserService service= SpringContext.getBean(UserService.class);
//			return service.getLoadedUserObject(lid,true);
//		};
//
//		return  distributedCache.withCache(lob,CACHE_DOMAIN, loginId,function);
//	}
//
	public CkUser getLoadedUserObject(String lid,boolean hierarchy) {
//		CkUser u = TimerUtils
//				.withTime("Time taken UserService record ", () -> userRepository.findByLoginId(lid));
		CkUser u = userRepository.findByLoginId(lid);
		if (u != null) {
			loadUserAssociationObjects(u);
//			if (hierarchy && outletDetailsService.getImmediateParent(u) == null) {
//				TimerUtils
//						.withTime("Time taken UserService hierarchyMetaDataService load ", () ->
//				u.setImmediateParent(hierarchyMetaDataService.findParentThroughUserLoginId(lid));
//						);
//				u.setImmediateParent(hierarchyMetaDataService.findParentThroughUserLoginId(lid));
			}
		return u;
		}


//
//	public User findByHierarchy(String hierarchy) {
//		return userRepository.findByHierarchy(hierarchy);
//	}
//
//	public User findByMobile(String mobile) {
//		List<User> users= userRepository.findByMobile(mobile);
//		if(CollectionUtils.isEmpty(users)) {
//			return null;
//		}else if(users.size()>1) {
//			var us =JsonUtils.toJsonString(users);
//			logger.info("users fetched {}",us);
//			throw new MultipleRecordsFoundException("Multiple records found with same mobile number");
//		}
//		return users.get(0);
//	}
//
//	public Optional<List<User>> findByMobileSafely(String mobile) {
//		List<User> users= userRepository.findByMobile(mobile);
//		return CollectionUtils.isEmpty(users)? Optional.empty() : Optional.of(users);
//	}
//
//	public Optional<User> findActiveUserByMobile(String mobile) {
//		Optional<List<User>> users= findByMobileSafely(mobile);
//		if(users.isEmpty()) {
//			return Optional.empty();
//		}else {
//			return users.get().stream().filter(User::isActive).findFirst();
//		}
//	}
//
//	public Optional<List<User>> findByEmail(String email) {
//		List<User> users= userRepository.findByEmail(email);
//		return CollectionUtils.isEmpty(users)? Optional.empty() : Optional.of(users);
//	}
//
//	public User findByFacebookPSID(String psid) {
//		return userRepository.findByFacebookPSID(psid);
//	}
//
	@Override
	public CkUser save(CkUser inUser) {
		return this.save(inUser, OperationType.insert);
	}

   public void saveUserHierarchyMetadata(CkUser user){
	   List<CkHierarchyMetadata> hierarchy = user.getImmediateParent();
	   var record = dsl.newRecord(CK_HIERARCHY_METADATA,hierarchy.get(0));
	   if(hierarchy.get(0).getId()==null){
		   user.getImmediateParent().get(0).setId("hierarchy-parent");
	   }
	   if(record.get(CK_HIERARCHY_METADATA.ID)==null) record.set(CK_HIERARCHY_METADATA.ID,"hierarchy-parent");
	   if(record.get(CK_HIERARCHY_METADATA.VERSION)==null) record.set(CK_HIERARCHY_METADATA.VERSION,1);
	   dsl.insertInto(CK_HIERARCHY_METADATA)
			   .set(record)
			   .onDuplicateKeyUpdate()
			   .set(record)
			   .execute();
   }

	public CkUser save(CkUser inUser, OperationType type) {
		//String lob = SecurityContextUtils.getLob();
		//CkUser user= TimerUtils.withTime("Time Taken to execute fillUser()", u-> fillUser(inUser));
		CkUser user = fillUser(inUser);
		//clearCache(lob,user);
		if(user.getRoles().size()==1 && user.getRoles().stream().allMatch(desig->desig.getName().equals(RoleName.ROLE_ADMIN.name()))) {
			CkUserParent up= new CkUserParent();
			up.setUserloginid(user.getLoginid());
			up.setParent(null);
			up.setLob(inUser.getLob());
			//UserParent refreshedObj=TimerUtils.withTime("Time taken to refresh UserParent", s-> userparentservice.refresh(up));
			CkUserParent refreshedObj = userparentservice.refresh(up);
			//TimerUtils.withTime("Time taken to save UserParent", ()->
			var record = dsl.newRecord(CK_USER_PARENT,refreshedObj);
			dsl.insertInto(CK_USER_PARENT)
					.set(record)
					.onDuplicateKeyUpdate()
					.set(record)
					.execute();
			userparentservice.save(refreshedObj);
		//);
		}

		if(user.getImmediateParent() != null && !user.getImmediateParent().isEmpty()) {
			//TimerUtils.withTime("Time taken to save UserParent", ()->
			saveUserParent(user,type);
			//);
		}


		//User savedObj= TimerUtils.withTime("Time Taken to save User[["+user.getLoginId()+"]]", u-> super.save(user));
		saveUser(user);
		if(user.getImmediateParent()!=null && user.getImmediateParent().size() > 0) {
			saveUserHierarchyMetadata(user);
		}
        CkUser savedObj = super.save(user);
		if(inUser.getSupplierMetaData()!=null && !inUser.getSupplierMetaData().isEmpty()) {
			List<CkSupplierMetadata> supplierMetaInfo= user.getSupplierMetaData();
			if(!isSameSupplierMetada(savedObj, user)) {
				supplierMetaInfo.forEach(cdmObject->{
					cdmObject.setUser(savedObj);
					cdmObject.setLob(user.getLob());
				});
				//TimerUtils.withTime("Time taken to batchSave SupplierMetadata of Size "+supplierMetaInfo.size(), ()->
				//supplierMetaDataService.batchSave(supplierMetaInfo));
			}

		}

		//AuditLogger.log(LOG_TYPE, "Created new User with loginId '{}'",user.getLoginId());
		//clearCache(lob,user);

		return savedObj;
	}

	private CkUser saveUser(CkUser user){
		user.setActiveStatus(ActiveStatus.ACTIVE);
		var record = dsl.newRecord(com.salescode.jooq.generated.tables.CkUser.CK_USER,user);
		if(record.get(com.salescode.jooq.generated.tables.CkUser.CK_USER.VERIFIED)==null) record.set(com.salescode.jooq.generated.tables.CkUser.CK_USER.VERIFIED,(byte)1);
		if(record.get(com.salescode.jooq.generated.tables.CkUser.CK_USER.PASSWORD) == null) record.set(com.salescode.jooq.generated.tables.CkUser.CK_USER.PASSWORD,user.getId());
		if(record.get(com.salescode.jooq.generated.tables.CkUser.CK_USER.VERSION) == null) record.set(com.salescode.jooq.generated.tables.CkUser.CK_USER.VERSION,1);
		dsl.insertInto(com.salescode.jooq.generated.tables.CkUser.CK_USER)
				.set(record)
				.onDuplicateKeyUpdate()
				.set(record)
				.execute();
		return user;
	}
//
	public CkUser fillUser(CkUser user){
		if(NullUtils.isNotNull(user.getLocationHierarchy())) {
			try {
//				CkLocation loc=user.getLocationHierarchy();
//				//loc = locationService.findLocationOrPersistLocation(loc);
//				user.setLocationHierarchy(loc);
			}
			catch(Exception ex) {
				throw new IllegalStateException("Error occured while setting location for user: "+user.getLoginid(),ex);
			}
		}else {
		//	throw new IllegalStateException("Missing location data. Data cannot be saved without location information for user : "+user.getLoginid());
		}
		if(user.getRoles()== null || user.getRoles().isEmpty()) {
			List<CkAuthRole> roles = roleService.getRoleAsList(RoleName.ROLE_USER.name());
			user.setRoles(roles);
		} else{
			List<CkAuthRole> roles = new ArrayList<>();
			for (CkAuthRole objectRole : user.getRoles()) {
				roleService.getRole(objectRole.getName()).ifPresent(elem->{
					if(!roles.contains(elem)) {
						roles.add(elem);
					}
				});
			}
			user.setRoles(roles);
		}

		if(user.getSupplierMetaData()!=null) {
			user.getSupplierMetaData().forEach(s->
			{
				//supplierMetaDataService.fillCommonAttributes(s);
				s.setUser(user);
			});
		}

		if(user.getVerified()==null) {
		//	user.setVerified(false);
		}

		return user;
	}
//
//	public List<User> findUserByQuery(String query,boolean isNative){
//		if(NullUtils.isNotNull(query)) {
//			List<User> users= null;
//			if(isNative) {
//				Query q = em.createNativeQuery(query, User.class);
//				users = q.getResultList();
//			}
//			else {
//				TypedQuery<User> q = em.createQuery(query, User.class);
//				users = q.getResultList();
//			}
//			return users;
//		}
//		return List.of();
//	}
//
//	public List<String> findLoginIdByQuery(String query,boolean isNative){
//		List<String> userstr= new ArrayList<>();
//		List<User> users= findUserByQuery(query,isNative);
//		if(users != null) {
//			users.forEach(user->userstr.add(user.getLoginId()));
//		}
//		return userstr;
//	}
//
//	@Transactional
//	public List<User> findByLoginIdIn(List<String> loginIds) {
//		TypedQuery<User> query = em.createQuery("SELECT e FROM User e WHERE e.loginId IN (:loginIds)",User.class);
//		query.setParameter("loginIds", loginIds);
//		return query.getResultList().stream().map(u->{
//			if (u.getRoles() != null) u.getRoles().size();
//			if (u.getImmediateParent() != null) u.getImmediateParent().size();
//			if (u.getSupplierMetaData() != null) u.getSupplierMetaData().size();
//			if (u.getMessengerInfo() != null) u.getMessengerInfo().size();
//			return u;
//		}).collect(Collectors.toList());
//	}
//
//	/**
//	 * Execute hierarchy procedure for all users.
//	 *
//	 */
//	public void executeProcedure() {
//		UserHierarchyManager.createHierarchy(SecurityContextUtils.getLob());
//	}
//
//	/**
//	 * Execute procedure.
//	 *
//	 * @param loginids the loginids
//	 */
//	public void executeProcedure(List<String> loginids) {
//		String ids=StringUtils.join(loginids, ',');
//		userRepository.executeProcedure(ids);
//	}
//
//	@Transactional(propagation= Propagation.REQUIRED)
//	public void updateUserContext(UserContextDTO uc){
//		var user = findByLoginId(uc.getLoginId());
//		if(StringUtils.isNotBlank(uc.getUserContext()) && !uc.getUserContext().equals(user.getUserContext())) {
//			updateUserContext(uc.getLoginId(), uc.getUserContext());
//		}
//	}
//
//	@Transactional(propagation= Propagation.REQUIRED)
//	public int updateUserContext(String loginId,String userContext) {
//		int updateUserContext = userRepository.updateUserContext(loginId, userContext);
//		AuditLogger.log(LOG_TYPE,"User with loginId '{}' updated with userContext '{}'",loginId,userContext);
//		clearCache(SecurityContextUtils.getLob(), loginId);
//		return updateUserContext;
//	}
//
//	@Transactional(propagation= Propagation.REQUIRED)
//	public int updateDeviceId(String loginId,String deviceId) {
//		int updateUserContext = userRepository.updateDeviceId(loginId, deviceId);
//		AuditLogger.log(LOG_TYPE,"User with loginId '{}' updated with deviceId '{}'",loginId,deviceId);
//		clearCache(SecurityContextUtils.getLob(), loginId);
//		return updateUserContext;
//	}
//	@Transactional(propagation= Propagation.REQUIRED)
//	public int updateUserContextAndDevideId(String loginId,String userContext,String deviceId) {
//		int updateUserContext = userRepository.updateUserContextAndDevideId(loginId, userContext,deviceId);
//		AuditLogger.log(LOG_TYPE,"User with loginId '{}' updated with deviceId '{}' and userContext '{}'",loginId,deviceId,userContext);
//		clearCache(SecurityContextUtils.getLob(), loginId);
//		return updateUserContext;
//	}
//
//	@Transactional
//	public void resetUserContext(String loginId) {
//		userRepository.updateUserContext(loginId, null);
//		AuditLogger.log(LOG_TYPE,"UserContext reset successful for User with loginId '{}'.",loginId);
//		clearCache(SecurityContextUtils.getLob(), loginId);
//	}
//
//	public void updateUserHierarchy() {
//		executeProcedure();
//		hierarchyMetaDataService.updateHierarchy();
//	}
//
//	/**
//	 * Update user hierarchy.
//	 */
//	public void updateUserHierarchy(List<String> loginids) {
//		List<UserParent> childs= userparentservice.findByParentIn(loginids);
//		if(!childs.isEmpty()) {
//			loginids.addAll(childs.stream().map(UserParent::getUserLoginId).collect(Collectors.toList()));
//		}
//		executeProcedure(loginids);
//		hierarchyMetaDataService.updateHierarchy();
//	}
//
//	public String getDefaultEncryptedUserPassword() {
//		try {
//			MetaData metadata= metadataservice.fetchByValue(DEFAULT_PASSWORD_DOMAIN_NAME, DEFAULT_PASSWORD_DOMAIN_TYPE);
//			String rawPassword;
//			if(metadata == null) {
//				rawPassword= DEFAULT_PASSWORD;
//			}else {
//				ArrayNode arraynode= metadata.getDomainValues();
//				if(arraynode == null || arraynode.size()==0) {
//					throw new MissingConfigurationException("System has found metadata resource for default password but seems misconfigured. Please check configuration.");
//				}
//				JsonNode node= arraynode.get(0);
//				if(!node.has("default")) {
//					throw new MissingConfigurationException("System has found metadata resource for default password but 'default' key not found. Please check configuration.");
//				}
//				rawPassword= node.get("default").textValue();
//			}
//			if(StringUtils.equals(DEFAULT_PASSWORD,rawPassword)){
//				return DEFAULT_ENCODED_PASSWORD;
//			}else {
//				return TimerUtils.withTime("time taken to encode password", () -> encoder.encode(rawPassword));
//			}
//		}catch(Exception ex) {
//			throw new ExecutionInteruptedException(ex,"Some error occured while getting default password");
//		}
//	}
//
//
	private static boolean staleRecords(Set<String> existingParents,List<CkHierarchyMetadata> immediateParents) {
		Set<String> hmlist = immediateParents.stream().map(CkHierarchyMetadata::getParent).collect(Collectors.toSet());
		return !existingParents.equals(hmlist);
	}
//
//
	public void saveUserParent(CkUser user, OperationType type){
		Set<CkUserParent> userParents= new HashSet<>();
		List<CkUserParent> dbParents= userparentservice.findByUserLoginId(user.getLoginid());
		if(dbParents != null && !dbParents.isEmpty()) {
			Set<String> dataset= dbParents.stream().map(CkUserParent::getParent).collect(Collectors.toSet());
			if(staleRecords(new HashSet<>(dataset), user.getImmediateParent())) {
				userParents.addAll(getNewUserParents(user, dataset, type));
				if(type.equals(OperationType.insert) || user.getDesignation().contains(RETAILER)) {
					userparentservice.deleteByUserLoginId(user.getLoginid());
					//hierarchySynchronizer.removeUser(user.getLoginid());
				}
			}
		}else {
			userParents.addAll(getUserParents(user) );
		}
		if(!userParents.isEmpty()) {
			CkUserParent par= userParents.stream().findFirst().orElseThrow(()-> new RuntimeException("value not found"));
			var record = dsl.newRecord(CK_USER_PARENT,userParents.stream().findFirst());
			record.set(CK_USER_PARENT.ID,par.getParent());
			dsl.insertInto(CK_USER_PARENT)
					.set(record)
					.onDuplicateKeyUpdate()
					.set(record)
					.execute();
//			userparentservice.batchSave(userParents);
//			evaluateUserHierarchy(user,userParents);
//			AuditLogger.log(LOG_TYPE, "Updated parents of User with loginId '{}'. LoginId of parents: '[{}]'", user.getLoginId(), getUserParentList(userParents));
		}
	}
//
//	private void evaluateUserHierarchy(User user,Set<UserParent> userParents) {
//		StringBuilder hierarchyStr = new StringBuilder();
//		userParents.forEach(parent -> {
//			List<HierarchyMetaData> hmList = (List<HierarchyMetaData>) hierarchyMetaDataService.findByImmediateParent(parent.getParent());
//			if (hmList.isEmpty()) {
//				hierarchyStr.append(user.getLoginId() + " > " + parent.getParent() + " > " + getCustomerAccountsService().getAdminLoginId());
//				hierarchyStr.append(",");
//			} else {
//				hmList.forEach(hierarchyMetaData -> {
//					hierarchyStr.append(user.getLoginId() + " > " + hierarchyMetaData.getHierarchy());
//					hierarchyStr.append(",");
//				});
//			}
//		});
//		user.setHierarchy(hierarchyStr.substring(0, hierarchyStr.length() - 1));
//		user.setNormalizedHierarchy(UserService.getNormalizedHierarchy(user.getHierarchy()));
//	}
	private List<CkUserParent> getNewUserParents(CkUser user, Set<String> dataset, OperationType type){
		List<CkUserParent> userParentList = new ArrayList<>();
		for(CkHierarchyMetadata hm: user.getImmediateParent()) {
			if(!dataset.contains(hm.getParent()) || type.equals(OperationType.insert)) {
				CkUserParent up= new CkUserParent();
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
	private List<CkUserParent> getUserParents(CkUser user){
		List<CkUserParent> userParentList = new ArrayList<>();
		for(CkHierarchyMetadata hm: user.getImmediateParent()) {
			CkUserParent up= new CkUserParent();
			up.setUserloginid(user.getLoginid());
			up.setParent(hm.getParent());
			up.setId(hm.getId());
			if (up.getUserloginid().equalsIgnoreCase(up.getParent())) {
			//	throw new UnexpectedResultException("User can't be mapped to itself. Found a record for user " + up.getUserLoginId() + " mapped to itself. Please verify the data once.");
			}
			userParentList.add(up);
		}
		return userParentList;
	}

	private String getUserParentList(Set<CkUserParent> userParents){
		StringBuilder userParentList = new StringBuilder();
		for (CkUserParent parent: userParents) {
			userParentList.append(parent.getParent() + ", ");
		}
		return userParentList.toString();
	}
//
	private static boolean isSameSupplierMetada(CkUser user1,CkUser user2) {

		if( (user1.getSupplierMetaData()==null|| user1.getSupplierMetaData().isEmpty()) && (user2.getSupplierMetaData()==null || user2.getSupplierMetaData().isEmpty())) {
			return true;

		} else if(user1.getSupplierMetaData()!=null && !user1.getSupplierMetaData().isEmpty()) {

			List<CkSupplierMetadata> spms1 = user1.getSupplierMetaData();

			if(user2.getSupplierMetaData()==null || user2.getSupplierMetaData().isEmpty()) {

				return false;
			}else {

				List<CkSupplierMetadata> spms2 = user2.getSupplierMetaData();


				if(spms2.size()!=spms1.size()) {

					return false;
				}else {
					try {

						return spms1.stream().allMatch(s->s.getUser()!=null && spms2.contains(s));

					}catch (Exception e) {
						logger.error("stacktrace", e);
					}
					return false;
				}

			}

		}else {

			return false;
		}

	}

	public static void main(String[] args) {

		CkUser u1 = new CkUser();
		u1.setLoginid("test1");
		List<CkSupplierMetadata> spms = new ArrayList<>();
		u1.setSupplierMetaData(spms);


		CkSupplierMetadata spm0 = new CkSupplierMetadata();
		spm0.setId("11");
		spm0.setMin(11);
		spm0.setUser(u1);
		spms.add(spm0);

		CkSupplierMetadata spm = new CkSupplierMetadata();
		spm.setId("1");
		spm.setMin(10);
		spm.setUser(u1);
		spms.add(spm);

		CkUser u2 = new CkUser();
		u2.setLoginid("test1");
		List<CkSupplierMetadata> spms1 = new ArrayList<>();
		u2.setSupplierMetaData(spms1);
		CkSupplierMetadata spm1 = new CkSupplierMetadata();
		spm1.setId("1");
		spm1.setMin(10);
		spm1.setUser(u2);
		spms1.add(spm1);

		CkSupplierMetadata spm2 = new CkSupplierMetadata();
		spm2.setId("11");
		spm2.setMin(11);
		spm2.setUser(u2);
		spms1.add(spm2);

		logger.error("{}",isSameSupplierMetada(u1, u2));


	}
	@Override
	public CkUser refresh(CkUser cdmObject) {
		var dbRecord = CdmDiffUtil.withOldModel(() -> (CkUser)EntityUtils.getInstance().findRecords(cdmObject.getClass(), cdmObject));
		if(dbRecord!=null){
			cdmObject.setOldModel(dbRecord.getOldModel());
			CkUser dbrecordsCopy = synchronizeNewObject(dbRecord,cdmObject);
			setSupplierChanges(dbrecordsCopy,cdmObject);
			setChanges(dbRecord,dbrecordsCopy);
			return dbrecordsCopy;
		}
		return cdmObject;
	}

	private CkUser synchronizeNewObject(CkUser dbRecord,CkUser cdmObject){
		CkUser dbrecordsCopy = new CkUser();
		CkUser clonedDBRecord=EntityUtils.deepClone(dbRecord);
		EntityUtils.copyProperties(clonedDBRecord, dbrecordsCopy);


		List<CkHierarchyMetadata> tempList = NullUtils.isNull(cdmObject.getImmediateParent())?dbrecordsCopy.getImmediateParent():cdmObject.getImmediateParent();
		//attributeUpdateOverrideManager.mergeProperties(cdmObject,dbrecordsCopy);

		Map<String,CkHierarchyMetadata> hmMap = new HashMap<>();
		dbrecordsCopy.getImmediateParent().forEach(h->hmMap.put(h.getParent(),h));

		List<CkHierarchyMetadata> changedList = new ArrayList<>();
		dbrecordsCopy.setImmediateParent(tempList.stream().map(h->{
			if(!hmMap.containsKey(h.getParent())){
				changedList.add(h);
			}
			return h;
		}).collect(Collectors.toList()));



		EntityUtils.copyProperties(cdmObject,dbrecordsCopy,"supplierMetaData","version");

		if(dbrecordsCopy.getMobile() != null && !dbrecordsCopy.getMobile().equals(dbRecord.getMobile())) {
			//dbrecordsCopy.setVerified(false);
		}

		if(!changedList.isEmpty()) {
			dbrecordsCopy.setHash(null);
			Set<Change<Serializable>> userChanges = dbrecordsCopy.getChanges();
			userChanges.add(new Change<>("immediateParent", null, null));
			dbrecordsCopy.setChanges(userChanges);
		}
		return dbrecordsCopy;
	}
	private void setSupplierChanges(CkUser dbrecordsCopy,CkUser cdmObject){
		if(!dbrecordsCopy.getSupplierMetaData().isEmpty()) {
			List<CkSupplierMetadata> cdmSupplierList=cdmObject.getSupplierMetaData();
			for(CkSupplierMetadata supplier:cdmSupplierList){
				supplier.setUser(dbrecordsCopy);
			}
			cdmSupplierList=supplierMetaDataService.refresh(cdmSupplierList);
			dbrecordsCopy.getSupplierMetaData().clear();
			dbrecordsCopy.getSupplierMetaData().addAll(cdmSupplierList);
		}else{
			dbrecordsCopy.setSupplierMetaData(cdmObject.getSupplierMetaData());
		}

	}


//
//	@Override
//	public String getKey(User cdmObject) {
//		return cdmObject.getLoginId();
//	}
//
//	public List<UserNameAndContext>  getAllUserFirebaseTokens() {
//		return  distributedCache.withCache(CHANNEL_CACHE, "all-firebase-tokens", k -> userRepository.getUserContexts());
//	}
//
//	/**
//	 * Delete user.
//	 *
//	 * @param loginId the login id
//	 * @throws IllegalArgumentException the illegal argument exception
//	 * @throws NullPointerException the null pointer exception
//	 * @throws UnexpectedResultException the unexpected result exception
//	 */
//	public void deleteUser(String loginId) throws IllegalArgumentException, NullPointerException, UnexpectedResultException {
//		if(!StringUtils.isBlank(loginId)) {
//			User user= findByLoginId(loginId);
//			if(user != null) {
//				try {
//					clearCache(SecurityContextUtils.getLob(),user);
//					userRepository.deleteById(user.getId());
//					AuditLogger.log(LOG_TYPE,"Deleted User with loginId '{}'",user.getLoginId());
//					return;
//				}catch(Exception th) {
//					throw new UnexpectedResultException(th, "Cannot delete user with loginid : '{}'", loginId);
//				}
//			}
//			throw new NullPointerException("Database record with loginid : '"+loginId+"' not found");
//		}
//		throw new com.applicate.services.channelkart.exceptions.IllegalArgumentException("User loginid : '{}' found null or empty", loginId);
//	}
//
//	@Transactional(propagation= Propagation.REQUIRED)
//	public List<User> bulkSave(List<User> inUsers) {
//		List<User> savedUser=new ArrayList<>();
//		List<User> failedUser=new ArrayList<>();
//		inUsers.forEach(user->{
//			try {
//				savedUser.add(save(user));
//			}catch(Exception e) {
//				failedUser.add(user);
//			}
//		});
//		return failedUser;
//	}
//
//	public List<User> findByDesignation(List<String> designation, List<String> loginIds, String nativeCondition,String alias){
//
//
//		String designationIn = designation.stream().map(d->"'"+d+"'").collect(Collectors.joining(","));
//
//		String loginIdCondition = loginIds == null?null:loginIds.stream().map(d->"'"+d+"'").collect(Collectors.joining(","));
//
//		String userQuery = "select "+alias+".* from ck_user "+alias+" join ck_userdesignation ud on "+alias+".loginid = ud.login_id where ud.designation in ("+designationIn+") " +(loginIdCondition==null?"":("and "+alias+".loginid in ("+loginIdCondition+")"))+(nativeCondition==null?"":("and "+nativeCondition));
//
//		Query query = em.createNativeQuery(userQuery, User.class);
//
//		return query.getResultList();
//
//	}
//
//	@SuppressWarnings("unchecked")
//	public List<User> findUsers(String value, Set<String> matchingAttributes) {
//		String sql = "select user from User user where ";
//		String condition = matchingAttributes.stream()
//				.map(attribute -> "user." + attribute + " = '" + value + "'")
//				.collect(Collectors.joining(" or "));
//		Query query = em.createQuery(sql + condition);
//		return query.getResultList();
//	}
//
//	/**
//	 * Find by login id.
//	 *
//	 * @param loginId the login id
//	 * @param status the status
//	 * @return the user
//	 */
//	public User findByLoginId(String loginId, ActiveStatus status) {
//		User user= findByLoginId(loginId,true);
//		if(user != null) {
//			ActiveStatus activeStatus= user.getActiveStatus();
//			if(activeStatus == status) {
//				return user;
//			}else {
//				throw new AccessDeniedException("unauthorized user");
//			}
//		}
//		return null;
//	}
//
//	/**
//	 * Checks if user is active.
//	 * @param user the user
//	 * @return true, if is active
//	 */
//	public static boolean isActive(User user) {
//		if(user == null) {
//			throw new IllegalArgumentException("Illegal null user provided in argument");
//		}
//		return user.getActiveStatus().equals(ActiveStatus.ACTIVE);
//	}
//
//	/**
//	 * Clear cache.
//	 *
//	 * @param lob the lob
//	 * @param loginId the login id
//	 */
//	public void clearCache(String lob, String loginId) {
//		if(StringUtils.isNotBlank(loginId)) {
//			distributedCache.clearCache(lob,CACHE_DOMAIN,loginId);
//			SupplierInfoService supplierInfoService= SpringContext.getBean(SupplierInfoService.class);
//			supplierInfoService.clearCache(lob,"u:"+loginId);
//			supplierInfoService.clearCache(lob,"o:"+loginId);
//			hierarchyMetaDataService.clearCache(lob, loginId);
//			clearChannelCache(loginId);
//		}
//	}
//
//	/**
//	 * Clear cache.
//	 *
//	 * @param lob the lob
//	 * @param user the user
//	 */
//	public void clearCache(String lob, User user) {
//		if(user != null) {
//			clearCache(lob, user.getLoginId());
//			locationService.clearCache(lob,user.getLocationHierarchy().getLocationHierarchy());
//		}
//	}
//
//	public User findOrFailByLoginId(String loginId) {
//		User user = findByLoginId(loginId);
//		if (user == null) {
//			throw new ResourceNotFoundException("Could not find user with login Id:" + loginId);
//		}
//		return user;
//	}
//
//	@Transactional
//	public User assignHierarchies(String from, String to) {
//		User fromUser = findOrFailByLoginId(from);
//		User toUser = findOrFailByLoginId(to);
//		String assignedHierarchy = joinHierarchies(fromUser.getHierarchy(), toUser.getAssignedHierarchy());
//		toUser.setAssignedHierarchy(assignedHierarchy);
//		return save(toUser);
//	}
//
//	@Transactional
//	public User revertAssignedHierarchies(String loginId) {
//		User user = findOrFailByLoginId(loginId);
//		Set<String> assignedHierarchies = toHierarchySet(user.getAssignedHierarchy());
//		Set<String> hierarchy = toHierarchySet(user.getHierarchy());
//		String hierarchyToUpdate = hierarchy.stream()
//				.filter(item -> !assignedHierarchies.contains(item))
//				.collect(Collectors.joining(","));
//		user.setHierarchy(hierarchyToUpdate);
//		user.setNormalizedHierarchy(UserService.getNormalizedHierarchy(user.getHierarchy()));
//		user.setAssignedHierarchy(null);
//		return save(user);
//	}
//
//	private String joinHierarchies(String from, String to) {
//		Set<String> fromHierarchies = toHierarchySet(from);
//		Set<String> toHierarchies = toHierarchySet(to);
//		fromHierarchies.addAll(toHierarchies);
//		return String.join(",", fromHierarchies);
//	}
//
//	private Set<String> toHierarchySet(String hierarchy) {
//		if (StringUtils.isEmpty(hierarchy)) {
//			return new HashSet<>();
//		}
//		return Arrays.stream(hierarchy.split(","))
//				.filter(StringUtils::isNotEmpty)
//				.map(String::trim)
//				.collect(Collectors.toSet());
//	}
//
//	/**
//	 * Find by messenger channel id.
//	 *
//	 * @param channelId the channel id
//	 * @return the optional
//	 */
//	public Optional<User> findByMessengerChannelId(String channelId) throws IllegalArgumentException{
//		Assert.hasLength(channelId,String.format("Illegal channel id : '%s' passed in argument", channelId));
//		User user= userRepository.findByMessengerInfoChannelId(channelId);
//		return Optional.ofNullable(user);
//	}
//
//	/**
//	 *
//	 * @param loginId
//	 * @param channel
//	 * @param channelId
//	 * @return
//	 */
//	public User saveMessengerInfo(String loginId, NotificationTypeRegistry channel, String channelId) {
//		Optional<User> channelUser= findByMessengerChannelId(channelId);
//
//		if(channelUser.isPresent()) {
//			return findByLoginId(channelUser.get().getLoginId());
//		}
//		User cachedUser= findByLoginId(loginId);
//		if(cachedUser != null) {
//			Set<UserMessengerInfo> messengerInfo= cachedUser.getMessengerInfo();
//			if(messengerInfo == null) {
//				messengerInfo= new HashSet<>();
//			}else {
//				messengerInfo= messengerInfo.stream().filter(p->!p.getChannel().equals(channel.name()))
//				               .collect(Collectors.toSet());
//			}
//			UserMessengerInfo minfo= UserMessengerInfo.Builder()//.setLoginId(cachedUser)
//					.setChannel(channel.name())
//					.setChannelId(channelId);
//			messengerInfo.add(minfo);
//			cachedUser.setMessengerInfo(messengerInfo);
//			return save(cachedUser);
//		}
//		return null;
//	}
//
//	public boolean isSuperAdmin(User user) {
//		return user.getRoles()
//				.stream()
//				.anyMatch(role -> role.getName().equals(RoleName.ROLE_SUPER_ADMIN.name()));
//	}
//
//	public boolean isAdmin(String loginId) {
//		if (loginId == null) {
//			return false;
//		}
//		User user = findByLoginId(loginId);
//		if (user != null) {
//			return user.getRoles()
//					.stream()
//					.anyMatch(role -> role.getName().equals(RoleName.ROLE_SUPER_ADMIN.name())
//							|| role.getName().equals(RoleName.ROLE_ADMIN.name())
//					);
//		}
//		return false;
//	}
//
//	/**
//	 * Checks if the given user is an admin or a super admin.
//	 *
//	 * @param user The User object to be checked for admin or super admin roles.
//	 *             It is expected that the User object has a list of roles,
//	 *             each with a name that can be retrieved with getName().
//	 *             The role names are expected to be the same as the names
//	 *             of the enum constants in RoleName.
//	 * @return true if the user has either the ROLE_SUPER_ADMIN or ROLE_ADMIN role,
//	 *         false otherwise. If the user is null, it also returns false.
//	 */
//	public boolean isAdmin(User user) {
//		if (user != null) {
//			return user.getRoles()
//					.stream()
//					.anyMatch(role -> role.getName().equals(RoleName.ROLE_SUPER_ADMIN.name())
//							|| role.getName().equals(RoleName.ROLE_ADMIN.name())
//					);
//		}
//		return false;
//	}
//
//	/***
//	 *
//	 * @param channelId
//	 */
//	public void removeMessagerInfo(final String channelId) {
//		Assert.hasLength(channelId, "Invalid channel id passed in method");
//		Optional<User> channelUser= findByMessengerChannelId(channelId);
//		if(channelUser.isPresent()) {
//			User user= findByLoginId(channelUser.get().getLoginId());
//			Set<UserMessengerInfo> messengerInfo= user.getMessengerInfo();
//			if(CollectionUtils.isNotEmpty(messengerInfo)) {
//				Set<UserMessengerInfo> tmp= messengerInfo.stream()
//						.filter(p->!p.getChannelId().equals(channelId))
//						.collect(Collectors.toSet());
//				user.setMessengerInfo(tmp);
//				this.save(user);
//			}
//		}
//	}
//
//
//	public List<UserMessengerInfo> findMessengerInfo(String loginId, String channel) {
//		List<UserMessengerInfo> messengerInfo= AppCacheManager.getInstance().withCache(SecurityContextUtils.getLob(), CHANNEL_CACHE + loginId ,
//				key -> userRepository.getMessengerInfos(loginId, channel));
//		return (messengerInfo != null)?
//			 messengerInfo.stream().filter(f->f.getChannel().equals(channel)).collect(Collectors.toList()) : null;
//	}
//
//	public void saveNative(User user, List<String> fieldNames) {
//		nem.update(Arrays.asList(user),fieldNames);
//		clearCache(SecurityContextUtils.getLob(),user);
//	}
//
//	public Optional<String> getUserContext(String loginId) {
//		return Optional.ofNullable(AppCacheManager.getInstance().withCache(SecurityContextUtils.getLob(), CHANNEL_CACHE + loginId + NotificationTypeRegistry.FIREBASE.name() ,
//				key -> {
//					Optional<String> userContext = userRepository.getUserContext(loginId);
//					return userContext.orElse(null);
//		}));
//	}
//
//
//	/**
//	 * Get user context of provided login ids
//	 * @param loginIdList list of login ids
//	 * @return Map of login id to user context
//	 */
//	public Map<String, String> getUserContext(List<String> loginIdList){
//		return userRepository.getUserContexts(loginIdList).stream().collect(Collectors.toMap(UserNameAndContext::getUserName, UserNameAndContext::getContext));
//	}
//
//	private void clearChannelCache(String loginId) {
//		AppCacheManager.getInstance().removeByDomain(SecurityContextUtils.getLob(), CHANNEL_CACHE + loginId);
//		AppCacheManager.getInstance().removeByDomain(SecurityContextUtils.getLob(), CHANNEL_CACHE + loginId + NotificationTypeRegistry.FIREBASE.name());
//
//	}
//
//	@Transactional
//	public void removeToken(String token) {
//		var users = userRepository.findByUserContext(token);
//		users.forEach(user -> resetUserContext(user.getLoginId()));
//	}
//
//	public static boolean isVerified(User user) {
//		if(user.getVerified()!=null) {
//			return user.getVerified();
//		}
//		return true;
//	}
//
//	public User findByActiveMobile(String mobile) {
//			List<User> users= userRepository.findByMobile(mobile);
//			if(CollectionUtils.isEmpty(users)) {
//				if(propertyRegistry.getAsBoolean(PropertyDefinition.USERMETADATA_FOR_MULTIPLE_MOBILE)) {
//					return findByMobileInMetadata(mobile);
//				}
//				return null;
//			}else {
//				User userFromDb = getActiveUser(users);
//				if (!userFromDb.isActive()) {
//		            throw new AccessDeniedException(UNAUTHORIZED_USER);
//		        }else {
//		        	return userFromDb;
//		        }
//			}
//	}
//	private User findByMobileInMetadata(String mobile){
//		List<UserMetadata> metadataList=userMetadataService.getByTypeAndValue(UserMetadataType.MOBILE_NUMBER,mobile);
//		if(metadataList.isEmpty()) return null;
//		if(metadataList.size()>1) {
//			throw new MultipleRecordsFoundException("Multiple records found with same mobile number");
//		}
//		User userFromDb=findByLoginId(metadataList.get(0).getLoginId());
//		if(!userFromDb.isActive()){
//			throw new AccessDeniedException(UNAUTHORIZED_USER);
//		}
//		return userFromDb;
//	}
//	public User findByActiveEmail(String email) {
//		List<User> users= userRepository.findByEmail(email);
//		if(CollectionUtils.isEmpty(users)) {
//			return null;
//		}else {
//			User userFromDb = getActiveUser(users);
//			if (!userFromDb.isActive()) {
//				throw new AccessDeniedException(UNAUTHORIZED_USER);
//			}else {
//				return userFromDb;
//			}
//		}
//	}
//
//
//	public boolean isAdmin(JwtUser user) {
//		List<String> roles = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
//		return RoleName.isAdmin(roles);
//	}
//
//	public boolean isAnyRoleMatching(User user, List<String> roles) {
//		return roles.stream()
//				.anyMatch(user::hasRole);
//	}
//
//	public boolean isAnyDesignationMatching(User user, List<String> designations) {
//		return designations.stream()
//				.anyMatch(user::hasDesignation);
//	}
//
//	public Long countUsersByDesignation(String designation) {
//		return userRepository.countByDesignationIs(designation);
//	}

	public static String getNormalizedHierarchy(String hierarchy) {
		if(StringUtils.isBlank(hierarchy)){
			return hierarchy;
		}
		String normalizedHierarchy = NORMALIZED_CHARECTORS+Arrays.asList(hierarchy.split(","))
		.stream().map(h->Arrays.asList(h.split(" > "))).flatMap(List::stream)
		.collect(Collectors.toSet()).stream()
		.collect(Collectors.joining(NORMALIZED_JOINING_CHARECTORS))+NORMALIZED_CHARECTORS;
		return removeSpecialCharacters(normalizedHierarchy);
	}

	private static String removeSpecialCharacters(String normalizedHierarchy) {
		String exludedCharactors = getExludedCharactors();
		try {
			String str = normalizedHierarchy.replaceAll(exludedCharactors, "");
			return str;
		} catch (Exception e) {
			logger.error("Exception happend while removing special charactors {} in normalized hierarchy {}", exludedCharactors, normalizedHierarchy);
			return normalizedHierarchy;
		}
	}
//
	public static String getExludedCharactors() {
		return System.getProperty("excludeCharNormalizedHierarchy","[^a-zA-Z0-9>]");
	}
//
//
//
//
//	/**
//	 * Get loginId and verification data from user table for all the provided login Ids.
//	 * @param loginIdList list of login id
//	 * @return map of loginId to verification status
//	 */
//	public Map<String, Boolean> getVerificationData(List<String> loginIdList){
//		List<Map<String, Object>> userVerificationList = userRepository.getUserAndVerification(loginIdList);
//		return userVerificationList.stream()
//				.collect(Collectors.toMap(data -> data.get(LOGIN_ID).toString(), data ->  Boolean.valueOf(data.get("verified").toString()) ) );
//	}
//
	private void loadUserAssociationObjects(CkUser u) {
//		TimerUtils
//				.withTime("Time taken UserService Association load ", () -> {
//					if (u.getRoles() != null) u.getRoles().size();
//					if (u.getSupplierMetaData() != null) u.getSupplierMetaData().size();
//					if (u.getDesignation() != null) u.getDesignation().size();
//					if (u.getMessengerInfo() != null) u.getMessengerInfo().size();
////				});

	}
}
//
//	public static boolean isCurrentUserIsTestUser(){
//		return isTestUser(SecurityContextUtils.getPrincipal());
//	}
//
//	public static boolean isTestUser(String loginid){
//		if(loginid==null) {
//			return false;
//		}
//		else {
//			return loginid.toLowerCase().startsWith(TEST_USER_STARTS_WITH);
//		}
//	}
//	private CustomerAccountsService getCustomerAccountsService() {
//		return SpringContext.getBean(CustomerAccountsService.class);
//	}
//
//	public ResponseEntity<String> processRolesForUser(String loginId,String operation,String roleName) {
//		permissionEvaluator.assertResourcePermission(Role.class.getSimpleName(), ResourceOperation.READ,Constants.MESSAGE_UNAUTHORIZED) ;
//		Optional<Role> roleObj = roleService.getRole(roleName);
//		if(roleObj.isEmpty()) {
//			throw new BadRequestException("Could not find a role for role-name:" + roleName);
//		}
//		User user = findByLoginId(loginId,false,true);
//		if(user==null) {
//			throw new BadRequestException("Could not find a user with loginID:" + loginId);
//		}
//		updateRole(roleObj.get(),user,operation);
//		return ResponseEntityMapper.toResponseEntity("User role updated Successfully ", HttpStatus.OK);
//
//
//	}
//
//	private void  updateRole(Role role,User user,String operation) {
//		List<Role> userRoles = user.getRoles();
//		if("add".equalsIgnoreCase(operation)) {
//			if (!user.hasRole(role.getName())) {
//				userRoles.add(role);
//				save(user);
//				clearCache(user.getLob(), user.getLoginId());
//			}
//		}else if("delete".equalsIgnoreCase(operation)){
//			if(user.hasRole(role.getName())) {
//				userRoles.remove(role);
//				save(refresh(user));
//				clearCache(user.getLob(), user.getLoginId());
//			}
//		}else {
//			throw new BadRequestException("Illegal operation please check the operation:" + operation);
//		}
//	}
//
//	public User getActiveUser(List<User> users){
//        List<User> activeUsers = users.stream().filter(CommonDataModel::isActive).collect(Collectors.toList());
//
//        if(activeUsers.isEmpty()){
//            if(users.size() > 1){
//                //Found Multiple Inactive Users - Throw Exception & Slack Alert
//                NotifyEventAdapter.notify(NotifyEventChannel.CHANNELKART_NOTIFICATION, NotifyEventSeverity.LOW, ErrorNotificationConstants.LOGIN_FAILURE_SUBJECT,
//                        MULTIPLE_USER_FOUND ,null, NotifyEvent.Status.FAILURE, "");
//                throw new AccessDeniedException(DEFAULT_ERROR_MESSAGE);
//            }
//
//            //Else - Return First Inactive User from the list
//            return users.stream().findFirst().orElseThrow(() -> new AccessDeniedException(DEFAULT_ERROR_MESSAGE, UnificationErrorCodes.MISSING_OUTLET));
//        }
//
//        if(activeUsers.size() > 1){
//			List<User> activeRetailerUsers = activeUsers.stream().filter(this::isRetailer).collect(Collectors.toList());
//			if(activeRetailerUsers.size()==1){
//				return activeRetailerUsers.stream().findFirst().orElseThrow(() -> new AccessDeniedException(DEFAULT_ERROR_MESSAGE));
//			}
//            //More than one active user -> throw exception and slack alert
//            NotifyEventAdapter.notify(NotifyEventChannel.CHANNELKART_NOTIFICATION, NotifyEventSeverity.LOW, ErrorNotificationConstants.LOGIN_FAILURE_SUBJECT,
//                    MULTIPLE_USER_FOUND ,null, NotifyEvent.Status.FAILURE, "");
//            throw new AccessDeniedException(DEFAULT_ERROR_MESSAGE);
//        }
//
//        //Exactly One Active User Found
//        return activeUsers.stream().findFirst().orElseThrow(() -> new AccessDeniedException(DEFAULT_ERROR_MESSAGE));
//    }
//
//	private boolean isRetailer(User user){
//		return user.getDesignation().contains(RETAILER);
//	}
//
//	public void checkuserAllowedToLogin(User userFromDb) {
//    	if(isAppAccessCheckEnabled() && !StringUtils.isEmpty(propertyRegistry.getValue(PropertyDefinition.KEY_IN_EXTENDED_ATTRIBUTES_FOR_APP_ACCESS_USAGE)) && userFromDb.getExtendedAttributes() != null && (checkAccessCheck(userFromDb))) {
//				throw new AccessDeniedException("User does not have access to Login");
//		}
//		if(RoleName.isLoginBlocked(userFromDb.getRoles())){
//			throw new AccessDeniedException("Login access blocked for the user role");
//		}
//	}
//
//    private boolean checkAccessCheck(User userFromDb) {
//    	String valueCheck = userFromDb.getExtendedAttributes().has(propertyRegistry.getValue(PropertyDefinition.KEY_IN_EXTENDED_ATTRIBUTES_FOR_APP_ACCESS_USAGE)) ? userFromDb.getExtendedAttributes().get(propertyRegistry.getValue(PropertyDefinition.KEY_IN_EXTENDED_ATTRIBUTES_FOR_APP_ACCESS_USAGE)).asText() : null;
//    	if(!StringUtils.isEmpty(valueCheck)) {
//    		return valueCheck.trim().equalsIgnoreCase("N");
//    	}
//    	return false;
//	}
//
//	public boolean isAppAccessCheckEnabled() {
//        return propertyRegistry.getAsBoolean(PropertyDefinition.APP_ACCESS_ENABLED);
//    }
//
//
//	@Transactional
//	public void unblockUser(User user) {
//		if(user==null || user.getLoginId()==null){
//			return;
//		}
//		user.setBlocked(false);
//		userRepository.updateBlocked(user.getLoginId(),false,user.getHash());
//		clearCache(SecurityContextUtils.getLob(), user);
//		clearLastAttemtRecords(user);
//	}
//
//	@Transactional
//	public void updateVerifiedAndUnblockUser(User user)
//	{
//		userRepository.updateVerified(user.getLoginId(),user.getVerified(),user.getBlocked(),user.getHash());
//		clearCache(SecurityContextUtils.getLob(), user);
//		clearLastAttemtRecords(user);
//	}
//
//	public UserStatus clearLastAttemtRecords(User user) {
//		UserStatus userStatus = userStatusService.findByLoginIdSafe(user.getLoginId());
//		if (userStatus == null || userStatus.getExtendedAttributes() == null
//				|| ((ObjectNode) userStatus.getExtendedAttributes()).get(UserBlockingService.FAILED_LOGIN_ATTEMPTS) == null)
//		{
//			return null;
//		}
//		((ObjectNode) userStatus.getExtendedAttributes()).remove(UserBlockingService.FAILED_LOGIN_ATTEMPTS);
//		return userStatusService.save(userStatus);
//	}
//
//	@Transactional
//	public void blockUser(UserStatus userInfo,User user) {
//		userStatusService.save(userInfo);
//		user.setBlocked(true);
//		userRepository.updateBlocked(user.getLoginId(),true,user.getHash());
//		clearCache(SecurityContextUtils.getLob(), user);
//	}
//
//	@Transactional
//	public ResponseEntity<String> activateInactiveUser(String loginId){
//		userRepository.updateActiveStatusAndReason(loginId,ActiveStatus.ACTIVE,"active");
//		userStatusService.updateLastAppUsedDate(loginId,Calendar.getInstance().getTime());
//		clearCache(SecurityContextUtils.getLob(),loginId);
//		return ResponseEntityMapper.toResponseEntity("Updated Successfully",HttpStatus.OK);
//	}
//
//	public boolean isParentUser(String loggedInUserName,String jwtUser) {
//
//		if (hasAdminAccess()) {
//			return true;
//		}
//
//		User user = findByLoginId(loggedInUserName, true);
//		if (user == null) {
//			return false;
//		}
//
//		return Arrays.stream(user.getHierarchy().split(","))
//				.map(i -> Arrays.asList(i.split(" > "))).flatMap(List::stream)
//				.anyMatch(immId -> immId.equals(jwtUser));
//	}
//
//	private boolean hasAdminAccess() {
//		JwtUser jwtUser = SecurityContextUtils.getJWTUser();
//
//		return jwtUser != null ? jwtUser.isAdmin() : this.isAdmin(SecurityContextUtils.getPrincipal());
//	}
//
//	public Optional<String> findLoginIdByReferenceId(String externalReferenceId) {
//		return userRepository.findLoginIdByReferenceId(externalReferenceId);
//	}
//
//	public Set<String> getMappedUser(String loginId,boolean backward) {
//		List<UserInfo> users = hierarchySynchronizer.getUhm().getUsers(loginId, "all",backward,false);
//		return users.stream()
//				.map(UserInfo::getLoginId)
//				.collect(Collectors.toSet());
//	}
//
//    public void filterUserData( Map<String, String> requestParams,Set<String> mappedUser) {
//		List<String> userList = List.copyOf(mappedUser);
//		String filteredUser = "[" + String.join(",", userList) + "]";
//		String filter = requestParams.get(ApiFilterProcessorImpl.Filters.FILTER.getValue());
//			if(StringUtils.isBlank(filter)){
//				requestParams.put(ApiFilterProcessorImpl.Filters.FILTER.getValue(), "loginId[in]:".concat(filteredUser));
//			}else {
//				filter = filter.concat(" and loginId[in]:".concat(filteredUser));
//				requestParams.put(ApiFilterProcessorImpl.Filters.FILTER.getValue(), filter);
//			}
//	}
//	 public List<Map<String, String>> getDivision(String loginId,boolean backward) {
//		List<UserInfo> users = hierarchySynchronizer.getUhm().getUsers(loginId, "all",backward,false);
//		return users.stream().map(u->u.getDesignation().stream()
//				.map(d -> Map.of("designation",d,LOGIN_ID, u.getLoginId()==null?"":u.getLoginId(),"name", u.getName()==null?"":u.getName()))
//				.collect(Collectors.toList())
//		).flatMap(List::stream).collect(Collectors.toList());
//	}
//
//	public void updateReportPassword(String loginId, String reportPassword) {
//		userRepository.updateReportPassword(loginId,reportPassword);
//		distributedCache.clearCache(SecurityContextUtils.getLob(),CACHE_DOMAIN,loginId);
//	}

