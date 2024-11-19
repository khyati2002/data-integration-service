package com.salescode.dataintegration.etl.cdm.services;


import com.salescode.dataintegration.etl.cdm.AbstractCDMService;
import com.salescode.jooq.generated.tables.pojos.CkCustomerAccount;

import org.springframework.stereotype.Service;

@Service
public class CustomerAccountsService extends AbstractCDMService<CkCustomerAccount> {
    public CustomerAccountsService() {
        super();
    }

//	private static final Logger log = LoggerFactory.getLogger(CustomerAccountsService.class);
//
//	private static final String DOMAIN_NAME = "customerAccountInfo";
//
//	private static final String ADMIN_NAME = "admin";
//
//	private static final String COUNTRY_NAME = "country";
//
//	private static final String APP_NAME = "appName";
//
//	private static final String APP_LOGO = "appLogo";
//
//	private CustomerAccountsRepository customerAccountsRepository;
//
//	private UserService userService;
//
//	private RoleService roleService;
//
//	private BCryptPasswordEncoder bCryptPasswordEncoder;
//
//	private ProfileService profileService;
//
//	private DatabaseProfileRegistry databaseProfileRegistry;
//
//	private StartupBooster startupBooster;
//
//	private ObjectMapper objectMapper;
//
//	private ApplicationEventPublisher applicationEventPublisher;
//
//	private Publisher publisher;
//
//	private HierarchyMetaDataService hierarchyMetaDataService;
//
//	private UserRepository userRepository;
//
//	private LocationService locationService ;
//
//	private MetaDataService metaDataService;
//
//    private MediaOperationServiceProvider mediaOperationServiceProvider;
//
//	public CustomerAccountsService(CustomerAccountsRepository repository, UserService userService, RoleService roleService, BCryptPasswordEncoder bCryptPasswordEncoder, ProfileService profileService, DatabaseProfileRegistry databaseProfileRegistry, StartupBooster startupBooster, ObjectMapper objectMapper, ApplicationEventPublisher applicationEventPublisher, Publisher publisher, HierarchyMetaDataService hierarchyMetaDataService, UserRepository userRepository, LocationService locationService, MetaDataService metaDataService, MediaOperationServiceProvider mediaOperationServiceProvider) {
//		super(repository);
//		this.customerAccountsRepository = repository;
//		this.userService = userService;
//		this.roleService = roleService;
//		this.bCryptPasswordEncoder = bCryptPasswordEncoder;
//		this.profileService = profileService;
//		this.databaseProfileRegistry = databaseProfileRegistry;
//		this.startupBooster = startupBooster;
//		this.objectMapper = objectMapper;
//		this.applicationEventPublisher = applicationEventPublisher;
//		this.publisher = publisher;
//		this.hierarchyMetaDataService = hierarchyMetaDataService;
//		this.userRepository= userRepository;
//		this.locationService=locationService;
//		this.metaDataService = metaDataService;
//		this.mediaOperationServiceProvider = mediaOperationServiceProvider;
//	}
//
//	public CustomerAccountInfo getCustomerAccountInfo(String lob) {
//
//		AppCacheManager cacheManager = AppCacheManager.getInstance();
//		return cacheManager.withCache(DOMAIN_NAME,lob, k ->customerAccountsRepository.findByLob(lob));
//	}
//
//	public SubscriptionPlan getSubscriptionPlan(String lob) {
//
//			CustomerAccountInfo customerAccountInfo = getCustomerAccountInfo(lob);
//			if(customerAccountInfo!=null) {
//				SubscriptionPlan sp = customerAccountInfo.getSubscriptionPlan();
//				if (sp == null) {
//					sp = SubscriptionPlan.STANDARD;
//				}
//				return sp;
//			}else{
//				return SubscriptionPlan.STANDARD;
//			}
//
//	}
//
//	public String getTimeZone() {
//
//		String timeZone = SecurityContextUtils.getTimeZone();
//
//		if(timeZone!=null)
//			return timeZone;
//
//		return getTimeZone(SecurityContextUtils.getLob());
//	}
//
//	public String getTimeZone(String lob) {
//		CustomerAccountInfo customerAccountInfo = getCustomerAccountInfo(lob);
//		if(customerAccountInfo == null) {
//			throw new IllegalArgumentException("Customer account not found for lob '"+lob+"'. Please check if customer account defined with non-empty column lob.");
//		}
//		return customerAccountInfo.getTimeZone();
//	}
//
//	@Transactional
//	public CustomerAccountInfo createCustomerAccount(CustomerAccountInfo customerAccount) throws JsonProcessingException {
//		String customerAccountStr = objectMapper.writeValueAsString(customerAccount);
//		fillValues(customerAccount);
//		return SecurityContextUtils.switchWithLOB(customerAccount.getLob(),
//				() -> createCustomerAccountInfo(customerAccount,customerAccountStr), true);
//
//	}
//
//	private void publishEvents(CustomerAccountInfo customerAccount,String customAccountStr){
//		NotificationEvent event = new NotificationEvent(this, customAccountStr, "registration");
//		applicationEventPublisher.publishEvent(event);
//		publishUserRegisteredEvent(customerAccount);
//		try {
//			LOBRegisterEvent cue = new LOBRegisterEvent();
//			cue.setLob(customerAccount.getLob());
//			SpringContext.getBean(DistributedCache.class).publishChangeEvent(cue);
//		}catch (Exception e){
//			log.error("Could not publish events",e );
//		}
//	}
//
//
//	private void publishUserRegisteredEvent(CustomerAccountInfo customerAccountInfo) {
//		AuthUser user = AuthUser.builder()
//				.addRole(RoleName.ROLE_ADMIN.name())
//				.setSubscriptionPlan(customerAccountInfo.getSubscriptionPlan().name())
//				.setUserName(customerAccountInfo.getAdmin().getLoginId())
//				.setId("")
//				.setLob(customerAccountInfo.getLob())
//				.build();
//		Event<CustomerRegisteredMessage> event = Event.create(EventTopic.CUSTOMER_REGISTERED, new CustomerRegisteredMessage(user));
//		publisher.publish(event);
//	}
//
//	private void fillValues(CustomerAccountInfo customerAccount) {
//		customerAccount.setId(UUID.randomUUID().toString());
//		customerAccount.getAdmin().setId(UUID.randomUUID().toString());
//		customerAccount.getAdmin().setName(Optional.ofNullable(customerAccount.getAdmin().getName()).orElse(customerAccount.getAdmin().getLoginId()));
//		customerAccount.getAdmin().setPassword(bCryptPasswordEncoder.encode(customerAccount.getAdmin().getPassword()));
//		customerAccount.getAdmin().setHierarchy(customerAccount.getAdmin().getLoginId());
//		customerAccount.getAdmin().setNormalizedHierarchy(UserService.getNormalizedHierarchy(customerAccount.getAdmin().getHierarchy()));
//		customerAccount.getAdmin().setDesignation(Set.of(ADMIN_NAME));
//		customerAccount.getAdmin().setActiveStatus(ActiveStatus.ACTIVE);
//		if (customerAccount.getSubscriptionPlan() == null) {
//			customerAccount.setSubscriptionPlan(SubscriptionPlan.STANDARD);
//		}
//		String lob = customerAccount.getLob().toLowerCase();
//		customerAccount.setLob(lob);
//	}
//
//	private Profile getUnAssignedLOB(String newLob) {
//		List<Profile> profiles = profileService.findByTypeFromDb("database");
//		Profile unAssignedProfile = profiles.stream()
//				.filter(profile -> profile.getLob() == null)
//				.filter(profile -> profile.getAttributes() != null)
//				.findFirst()
//				.orElseThrow(() -> new IllegalStateException("insufficient database resources to fulfil this request"));
//		unAssignedProfile.setLob(newLob);
//		return unAssignedProfile;
//	}
//
//
//	public CustomerAccountInfo createCustomerAccountInfo(CustomerAccountInfo customerAccountInfo, String customAccountInfoStr) {
//		if (hasAlreadyExist(customerAccountInfo)) {
//			throw new IllegalStateException("User already exists");
//		}
//		List<Role> roles = roleService.getRole(RoleName.ROLE_ADMIN.name(),true)
//				.map(List::of)
//				.orElseThrow(() -> new IllegalStateException("Could not find role:" + RoleName.ROLE_ADMIN.name()));
//		customerAccountInfo.getAdmin().setRoles(roles);
//		CustomerAccountInfo savedAccount = this.save(customerAccountInfo);
//		HierarchyMetaData hierarchyMetaData= new HierarchyMetaData();
//		hierarchyMetaData.setImmediateParent(savedAccount.getAdmin().getLoginId());
//		hierarchyMetaData.setHierarchy(savedAccount.getAdmin().getLoginId());
//		Location location = savedAccount.getAdmin().getLocationHierarchy();
//		hierarchyMetaData.setLocationHierarchy((location == null)?null : location.getLocationHierarchy());
//		hierarchyMetaData.setLob(savedAccount.getAdmin().getLob());
//		hierarchyMetaDataService.save(hierarchyMetaData);
//		userService.save(savedAccount.getAdmin());
//		log.info("Customer Account successfully created, for Lob:{}, user:{}",customerAccountInfo.getLob(), customerAccountInfo.getAdmin() );
//		try {
//			startupBooster.load(lobName -> lobName.equalsIgnoreCase(savedAccount.getLob()));
//			publishEvents(customerAccountInfo,customAccountInfoStr);
//		} catch (Exception e) {
//			log.error("Could not send Events", e);
//		}
//		return savedAccount;
//	}
//
//
//	private boolean hasAlreadyExist(CustomerAccountInfo customerAccountInfo) {
//		CustomerAccountInfo customerAccount = this.getCustomerAccountInfo(customerAccountInfo.getLob());
//		User admin = userRepository.findByLoginId(customerAccountInfo.getAdmin().getLoginId());
//		// if any of this object already exist then stop the registration
//		return customerAccount != null || admin != null;
//	}
//
//	public CustomerAccountInfo getCountry(ObjectNode node)  {
//		CustomerAccountInfo customerAccount = new CustomerAccountInfo();
//		try {
//			Profile profile = getUnAssignedLOB(node.get("lob").asText());
//			databaseProfileRegistry.registerProfile(profile);
//			Location locObj = new Location();
//			ObjectNode admin = (ObjectNode) node.get(ADMIN_NAME);
//			ObjectNode user = getDialCode(admin);
//			String country = user.get(COUNTRY_NAME).asText();
//			Locale l = new Locale("", country);
//			node.put("currency" , Currency.getInstance(l).toString());
//			node.put("currencySymbol", Currency.getInstance(l).getSymbol(l));
//			user.put("countryCode", country);
//			user.remove(COUNTRY_NAME);
//			locObj.setCountry(l.getDisplayCountry());
//			Location location = SecurityContextUtils.switchWithLOB(node.get("lob").asText(),
//					() -> {
//						try {
//							return locationService.findLocationOrPersistLocation(locObj);
//						} catch (Exception e) {
//							log.error("Could not save location", e);
//						}
//						return null;
//					},true);
//			JsonNode userNode = JSONUtils.getObjectMapper().readTree(user.toString());
//			node.put(ADMIN_NAME, userNode);
//		     customerAccount = JSONUtils.getObjectMapper().convertValue(node, CustomerAccountInfo.class);
//			 customerAccount.getAdmin().setLocationHierarchy(location);
//			 customerAccount.getAdmin().setPassword(user.get("password").asText());
//		} catch (Exception e) {
//			throw new GenericOperationFailure(
//					"Exception happenned while saving the lacation "+e.getLocalizedMessage(),
//					HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//
//		return saveMetaData(customerAccount);
//	}
//
//	public ObjectNode getDialCode(ObjectNode node) {
//		String mobileNumber = node.get("mobile").asText();
//		String countryCode = node.get(COUNTRY_NAME).asText();
//		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
//		try {
//			Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(mobileNumber, countryCode);
//			node.put("mobile", String.valueOf(phoneNumber.getNationalNumber()));
//			node.put("dialCode", String.valueOf(phoneNumber.getCountryCode()));
//		} catch (Exception e) {
//			log.error("NumberParseException was thrown: {}" , e.toString());
//		}
//		return node;
//	}
//
//	public CustomerAccountInfo refresh(CustomerAccountInfo cdmObject,String id) {
//
//			CustomerAccountInfo dbRecord = findById(id);
//			if (dbRecord != null){
//				EntityUtils.copyProperties(cdmObject, dbRecord, "version", ADMIN_NAME);
//				return dbRecord;
//			}
//			return cdmObject;
//	}
//
//	public CustomerAccountInfo findByName(String name) {
//		return customerAccountsRepository.findByName(name);
//	}
//
//	public ResponseEntity<OperationResponse<CustomerAccountInfo>> update(CustomerAccountInfo customerAccountInfo,String id){
//
//			CustomerAccountInfo customerAccountInfoFmDb=this.findById(id);
//			if (customerAccountInfoFmDb != null) {
//				CustomerAccountInfo copyProp = this.refresh(customerAccountInfo,id);
//				String timeZone = copyProp.getTimeZone();
//				this.validateTimeZone(timeZone);
//				customerAccountInfoFmDb=this.save(copyProp);
//				OperationResponse<CustomerAccountInfo> or = new OperationResponse<>();
//				or.setStatus(OperationStatus.Success);
//				or.setFeature(customerAccountInfoFmDb);
//				return new ResponseEntity<>(or, HttpStatus.OK);
//
//			} else {
//				throw new ResourceNotFoundException("could not find customer account for given id :" + id);
//			}
//
//
//	}
//
//	public String getAdminLoginId(){
//		String lob = SecurityContextUtils.getLob();
//		return getCustomerAccountInfo(lob).getAdmin().getLoginId();
//	}
//
//	public String getAdminHierarchy(String inUser){
//		return inUser+ " > " +getAdminLoginId();
//	}
//
//	public List<HierarchyMetaData> getAdminHierarchyMetadata(){
//		StringToHierarchyMetaDataConverter sth = new StringToHierarchyMetaDataConverter();
//		return sth.convert(getAdminLoginId());
//	}
//
//	public boolean validateTimeZone(String timeZone){
//		try {
//			 ZoneId.of(timeZone);
//				return true;
//		}catch(Exception e){
//			log.error("stacktrace", e);
//			throw new IllegalArgumentException("Please provide valid timeZone");
//		}
//	}
//
//	/**
//	 * Gets the time zone offset.
//	 *
//	 * @param lob the lob
//	 * @return the time zone offset
//	 */
//	public String getTimeZoneOffset(String lob) {
//		String timeZone= getTimeZone(lob);
//		return parseTimeZoneOffset(timeZone);
//	}
//
//	public static String parseTimeZoneOffset(String timeZone) {
//		LocalDateTime dt = LocalDateTime.now();
//		ZoneId zone = ZoneId.of(timeZone);
//		ZoneOffset off = zone.getRules().getOffset(dt);
//		DateTimeFormatter offsetFormatter = DateTimeFormatter.ofPattern("xxx");
//		return offsetFormatter.format(off);
//	}
//
//	public CustomerAccountInfo saveMetaData(CustomerAccountInfo customerAccountInfo) {
//		return SecurityContextUtils.switchWithLOB(customerAccountInfo.getLob(),
//				() -> {
//					try {
//						MetaData metaData =  metaDataService.fetchByValue("clientconfig", "config");
//						if(metaData != null) {
//							ArrayNode domain = metaData.getDomainValues();
//							ObjectNode domainValues = (ObjectNode) domain.get(0);
//							if(customerAccountInfo.getExtendedAttributes().has(APP_NAME)) {
//								domainValues.put(APP_NAME, customerAccountInfo.getExtendedAttributes().get(APP_NAME).asText());
//								((ObjectNode)customerAccountInfo.getExtendedAttributes()).remove(APP_NAME);
//							}
//							if(customerAccountInfo.getExtendedAttributes().has(APP_LOGO)) {
//								String blob = createBlob(customerAccountInfo.getExtendedAttributes().get(APP_LOGO).asText());
//								domainValues.put(APP_LOGO, blob);
//								((ObjectNode)customerAccountInfo.getExtendedAttributes()).remove(APP_LOGO);
//							}
//							domain.remove(0);
//							domain.insert(0, domainValues);
//							metaData.setDomainValues(domain);
//							 metaDataService.save(metaData);
//							 return customerAccountInfo;
//						}
//					} catch (Exception e) {
//						throw new GenericOperationFailure("Could not save MetaData "+e.getLocalizedMessage(),
//								HttpStatus.INTERNAL_SERVER_ERROR);
//					}
//					return null;
//				},true);
//
//	}
//
//	public String createBlob(String encodedValue) throws ConfigurationException {
//			String[] strings = encodedValue.split(",");
//			 byte[] data = DatatypeConverter.parseBase64Binary(strings[1]);
//		MediaOperation	media =  mediaOperationServiceProvider.getByProfile("images");
//		return media.upload("AppLogo.png", new ByteArrayInputStream(data), Long.valueOf(encodedValue.length()));
//
//	}
//


}
