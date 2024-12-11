package com.salescode.channelkart.client.properties;



import com.salescode.channelkart.models.enums.RoleName;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author : Jinu
 * Date    : 8/18/2021
 **/
public enum PropertyDefinition {

    REMOVE_TOKEN_ON_LOGOUT("remove.token.on.logout", "true",
            "If true, then system will remove the user token on logout"),

    ENCRYPT_TOKEN_NEW_TOKENS("encrypt.new.tokens", Constants.FALSE,
            "If true, then new tokens will be encrypted and share "),


    SAVE_TOKEN_IN_DB("save.token.in.db", Constants.FALSE,
            "If true, then new tokens will be persisted in db"),

    SHARE_SHORT_TOKEN("use.short.token", Constants.FALSE,
            "If true, then short token will be shared for each login"),

    ENABLE_PLAIN_TEXT_LOGIN_API("enable.plain.text.login.api", Constants.FALSE,
            "If true, then client can use plain text login api"),

    UNLIMITED_EXPIRY_ROLES("unlimited.expiry.roles", RoleName.ROLE_SUPER_ADMIN.name(),
            "Defined roles users will get unlimited expiry when they login. Multiple values can be separated by comma (example:ROLE_SUPER_ADMIN, ADMIN)"),

    UNLIMITED_EXPIRY_DESIGNATIONS("unlimited.expiry.designations", "",
            "Defined designation users will get unlimited expiry when they login. Multiple values can be separated by comma (example:admin, supplier)"),

    ENABLE_API_ENCRYPTION("enable.api.encryption", "False",
            "If enabled then the login request should have the secure token and all requests and response will be encrypted"),

    ENABLE_AES_GCM_KMS_ENCRYPTION("enable.aes.gcm.kms.encryption", "False",
            "If enabled then instead of aesEncryption aesGcmKmsEncryption will be applied on account info"),

    ENABLE_ANALYTICS_EVENTS("enable.analytics.events", Constants.FALSE, "If enabled then the analytics events will be triggered "),

    DISABLE_ANALYTICS("disable.analytics", Constants.FALSE, "If true all analytics calls will be ignored"),


    ENABLE_OUTLET_ANALYTICS_EVENTS("enable.outlet.analytics.events", Constants.FALSE, "If you want to convert daily_outletlevel_analytics and new_daily_outletlevel_analytics procedures to post event processing then please turn it to true and stop that procedures."),

    ENABLE_INTEGRATION("integration.enable", "True",
            "integration consumers will be started based on this", Constants.BOOLEAN),

    DELETE_FIREBASE_TOKEN_ON_PORTAL_LOGOUT("delete.firebase.token.on.portal.logout", Constants.FALSE,
            "If true then it will delete the current user firebase token from the db if the user try to logout from portal app"),

    RAISE_EVENT_LOG_ORDER("raise.event.log.order", Constants.FALSE,
            "If true then it will store the order event data into to the elastic data"),

    GET_SUPPLIER_SALES_BY_SUPPLIER_ID("get.supplier.sales.by.supplierid", Constants.FALSE,
            "If true then if loggedin user is supplier then supplier id will be used to fetch the sales data"),

    USE_DSRLOGINID_FOR_ORDER_LOGINID("use.dsr.loginid.for.order.loginid", Constants.FALSE,
            "If true then if loggedin user is DSR then order loginid will be evaluated with DSR loginid"),

    USE_DSRLOGINID_FOR_SALES_LOGINID("use.dsr.loginid.for.sales.loginid", Constants.FALSE,
            "If true then if loggedin user is DSR then sales loginid will be evaluated with DSR loginid"),

    USE_USER_ALT_ID_FOR_SALES("use.user.alternateId.sales", Constants.FALSE,
            "If true it replaces the loginid received in sales with the user_id corresponding to that loginid"),

    IGNORE_ORDER_DUPLICATE("ignore.order.duplicate.entry", Constants.FALSE,
            "If true then it will ignore the duplicate order and send status as success 200"),

    TEMPORARY_OUTLET_ORDER_ALLOWED("temporary.outlet.order.allowed", Constants.TRUE,
            "If false then temporary outlet will be blocked from ordering and send status as conflict 409"),

    IGNORE_ORDER_VALIDATE_API_VALIDATION_FAILURE("ignore.order.validateapi.validation.failure", Constants.FALSE,
            "If true then it will ignore the validation failure for order validate api and send status as success 200"),

    INTEGRATION_API_S3_LOG_ENABLED("integration.api.s3.log.allowed", Constants.FALSE,
            "If true then integration api /data request and response will be upload to s3"),

    REPORT_SERVICE_ENABLED("report.service.enabled", Constants.FALSE,
            "If true report request will forwarded to report service to generate report"),

    NEW_REPORT_SERVICE_ENABLED("channelkart.new.report.service", Constants.FALSE,
            "If true report request will forwarded to new report service to generate report"),

    NEW_STOCK_SERVICE_ENABLED("channelkart.new.stock.service", Constants.FALSE,
            "If true Saga Orchestrator steps for stock deduction will run."),

    APPLY_BETA_SCHEMES("apply.beta.schemes", Constants.FALSE,
            "If true then it will check account column of outlet detail if beta then add beta in extended attributes and run new scheme program"),

    TYPE_OF_SCHEME_APPLIED_FOR_UNNATI("type.of.scheme.applied.for.unnati", Constants.FALSE,
            "If it is true then the app specific format is applicable for unnati and if it is false then the old scheme format is applicable for unnati"),

    APP_ACCESS_ENABLED("app.access.enabled.for.user", Constants.FALSE,
            "If it is true then an additional check for app usage will be checked for the user from extendedAttributes"),

    GET_LIST_OF_CHARACTOR_EXCLUDE_FOR_NORMALIZED_HIERARCHY("list.charactor.exclude.for.normalizedhierarchy", "[^a-zA-Z0-9>]",
            "List of special charactors to be removed from normalized hierarchy"),

    SEND_CONFLICT_IF_PRODUCT_NOT_FOUND("sendconflict.if.product.notfound", Constants.FALSE,
            "If true then enrichment result will will be conflict instead of error"),

    JWT_SECRET_FROM_AWS_SECRET_MANAGER("jwt.secret.aws.secret.manager", Constants.FALSE,
            "If enabled then the secret key from aws secrete manager will be used"),

    USE_RANDOM_SUPPLIER_OF_OUTLET("use.random.supplier.outlet.multipleSupplierCase", Constants.FALSE,
            "If true then of outlet has multiple supplier then any random supplier will be used"),

    ORDER_AMOUNT_ROUND_OFF_ENABLED("order.amount.roundOff.enabled", Constants.TRUE,
            "If true then order amount calculation will be done on 2 digit decimal point round off"),

    GET_ORDER_HISTORY_DATA("get.order.history.data", Constants.TRUE,
            "If set to false, it will not include order history in get orders API "),

    OVERRIDE_DUPLICATE_UI_SKU_DETAILS("override.duplicate.ui.sku.details", Constants.FALSE,
            "If true then will override the previous sku details with latest in the same request body if duplicate sku present"),

    ORDER_GET_USE_HIERARCHY_LIKE("order.get.use.Hierarchy.like", Constants.FALSE,
            "If enabled it will use hierarchy like operation to get order"),

    SALES_GET_USE_HIERARCHY_LIKE("sales.get.use.Hierarchy.like", Constants.TRUE,
            "If enabled it will use hierarchy like operation to get sales"),

    ORDER_GET_USE_NORMALIZEDHIERARCHY("order.get.use.normalizedHierarchy", Constants.FALSE,
            "If enabled it will use normalized hierarchy to get order"),

    SALES_GET_USE_NORMALIZEDHIERARCHY("sales.get.use.normalizedHierarchy", Constants.FALSE,
            "If enabled it will use normalized hierarchy to get sales"),

    SHOULD_COMPARE_FINAL_ORDERINFO_QTY("compare.final.orderinfo.quantity", Constants.TRUE,
            "If false then it will skip quantity validation for final order info"),

    USE_PRODUCT_METADATA_DOUBLE_PRICE("use.productmetadata.price.doubletarnsient", Constants.FALSE,
            "If true then price will used from the product details metadata Double Transient field casePtrD,PiecePtrD"),

    FINAL_AMOUNT_VALIDATE_ROUND_OFF_PRECISION("finalAmount.validate.roundOff.precision", "2",
            "This is the order final amount decimal number precision to validate"),

    ORDER_UPDATE_TOKEN_EXPIRY("order.update.token.expiry.days", "7",
            "The token will expire after 7 days of creation date","int"),

    FINAL_AMOUNT_VALIDATE_DIFFERENCE_ALLOWED("finalAmount.validate.difference.allowed", "0",
            "This is the final amount difference allowed comparing with ui amount"),

    BIG_DECIMAL_SCHEME_SCALE_LIMIT("unnati.app.specific.scheme.order.calculation", "5",
            "This will set the BigDecimal scale for all the claculations regarding order and scheme for app specific scheme in unnati"),

    API_MAX_SIZE_LIMIT("api.size.param.max.limit", "10000", "Maximum number of records that api can give"),

    INTEGRATION_LOG_TIMEOUT("integration.logging.timeout.minutes", "5",
            "This is the time period for timeout in minutes for integration api logging to close file upload for each groupId"),

    AWS_CURRENT_SECRET_CACHE_TIMEOUT("aws.secret.cache.timeout.minutes", "1440",
            "This is the time period for timeout in minutes for aws current secret cache timeout"),

    FIND_SINGLE_SUPPLIER_IN_HIERARCHY_METADATA("find.single.supplier.in.hierarchymetadat", Constants.FALSE,
            "If this is enables then we will only take first supplier from the each hierarchy metadata and ignore supplier loginId is equal to outletCode"),

    REPLACE_CREATIONDATE_WITH_STARTDATE("replace.creationdate.with.startdate", Constants.FALSE,
            "If true then it will replace first creationDate with startDate and  second creationDate with endDate"),

    LOG_CATALOGUE_SUPPLIER("log.catalogue.supplier", Constants.FALSE,
            "Print logs for catalogue supplier info"),

    OVERRIDE_RECOMMENDED_ORDER_AMOUNT("recommendedorder.overrideAmount", Constants.FALSE,
            "If true then it will recalculate the amount from latest price info"),

    ALLOW_USER_SIGNUP("allow.user.signup", Constants.FALSE, "If false then it does not allow user to be created through sign API"),

    OVERRIDE_RECOMMENDED_ORDER_IGNORE_START_DATE("recommendedorder.ignoreStartDate", Constants.FALSE,
            "If true then it will ignore the start date from recommended order query"),

    DEFAULT_MDM_TIMEOUT("default.mdm.upload.timeout", "120",
            "The default mdm timeout in minutes. This parameter will use for find out the active mdm uploads in the system." +
                    " Also if the any of the mdm task exceeding this limit the system may mark it as completed or failed"),

    KPI_JOB_NAME("kpi.job.name", "channelkart-cli-command",
            "The name of the kpi job. This is used to invoke the kpi job after executing the aggregations"),

    MERGE_SUGGESTION_ENABLE("merge.suggestion.enable", Constants.TRUE,
            "The merge suggestion will merge the suggestion of sellina ai intent text and sellina query suggestion text"),

    INTENT_PARAM_KEY("intent.paramKey", "outletcode",
            "Intent param key"),

    CONVERT_DATE_FILTER_TO_UTC("convert.date.filter.to.utc", Constants.FALSE,
            "If true then it will assume incmoing date filter value in client timezone and convert incoming date filter to UTC."),

    CONVERT_DATE_FILTER_TO_UTC_FOR_ORDER_GET("convert.date.filter.to.utc.for.order.get", Constants.FALSE,
            "If true then it will assume incmoing date filter value in client timezone and convert incoming date filter to UTC for order get api."),

    CONVERT_DATE_FILTER_TO_UTC_FOR_SALES_GET("convert.date.filter.to.utc.for.sales.get", Constants.FALSE,
            "If true then it will assume incmoing date filter value in client timezone and convert incoming date filter to UTC for sales get api."),

    UNBLOCK_ON_RESET_PASSWORD("unblock.on.reset.password", Constants.FALSE,
            "If true, then user will be unblocked on resetting the password"),

    OLD_PASSWORD_SAVE_CHECK("old.password.save.check", Constants.FALSE,
            "If true, then few old passwords will be saved in  the database "),

    NUMBER_OF_OLD_PASSWORDS_SAVED("old.saved.password.count", "4",
            "Count of the old passwords that are to be saved in database"),

    SET_MOBILE_NUMBER_IN_FORGOT_PASSWORD_IF_NULL("set.mobile.number.in.forgot.password.if.null", Constants.FALSE,
            "turn this on if you want to add set mobile number from token in forgot password API only if passed null or empty"),

    UNBLOCK_ON_OTP_VERIFICATION("unblock.on.otp.verification", Constants.TRUE,
            "If true, then user will be unblocked on verifying himself with OTP"),

    FILTER_TEST_DATA("order.filter.admin.test.users", Constants.TRUE,
            "If true, then admin users except 'TestAdmin' will not get testing data for Orders", Constants.BOOLEAN),

    FILTER_TEST_DATA_FOR_SALES("sales.filter.admin.test.users", Constants.TRUE,
            "If true, then admin users except 'TestAdmin' will not get testing data for sales", Constants.BOOLEAN),

    DISABLE_SMART_TRIGGER_LOGS("smart.trigger.log.disable", Constants.TRUE,
            "If true, then the smart trigger status logs will not pushed in to task table", Constants.BOOLEAN),

    DISABLE_EVENT_LOGS("event.log.disable", Constants.FALSE,
            "If true, then the event logs will not pushed in to the task table", Constants.BOOLEAN),

    OUTLET_CACHE_FOR_SALESREP("salesrep.outlets.cached", Constants.FALSE,
      "Cache outlets for sales rep", Constants.BOOLEAN),


    IS_PRINCIPLE_LOB("principle.lob", Constants.FALSE,
            "If true, then this lob will be treated as principle lob and the extra check for user subscriptions will be applied while authorizing the request", Constants.BOOLEAN),

    NUMBER_OF_DAYS_ALLOWED_TO_DELETE_RECOMMENDED_ORDER_DATA("number.of.days.allowed.to.delete.recommended.order.data", "7",
	            "7 incidates only past/future 7 days data from current day can be deleted", "int"),

	MAX_RANGE_OF_DATE_ALLOWED_TO_DELETE_RECOMMENDED_ORDER_DATA("max.range.of.date.allowed.to.delete.recommended.order.data", "7",
            "7 incidates max difference between a date range while deleting the data", "int"),

    ONLY_ACTIVE_USER_HIERARCHY_UPDATE("active.user.hierarchy.update", Constants.FALSE,
            "If true, then user_hierarchy will update only for active users only", Constants.BOOLEAN),

    STOCK_BASED_ON_WAREHOUSE("stock.deduction.on.warehouse", Constants.FALSE,
            "If true, then stocks will be deducted/added based on ware house ", Constants.BOOLEAN),

	ALLOW_NEGETIVE_STOCK("allow.negetive.stock", Constants.FALSE,
            "If true, then we can store negetive stock qty in database ", Constants.BOOLEAN),

    ALLOW_ADDITION_OF_STOCK_ON_UPDATE("allow.addition.of.stock.on.update", Constants.FALSE,
            "If true, then stocks can be added or deducted while an order has been edited ", Constants.BOOLEAN),

    USE_PRODUCT_METADATA_MRP("use.productmetadata.mrp", Constants.FALSE,
            "If true then mrp will used from the product metadata"),

    USE_OUTLET_PRODUCT_INFO_MRP("use.outletproductInfo.mrp", Constants.FALSE,
            "If true then mrp will used from the outlet product info"),

    USE_PRODUCT_METADATA_CASE_MRP("use.productmetadata.case.mrp", Constants.FALSE,
            "If true then caseMrp will used from the product metadata"),
    MULTIPLE_UOM_ENABLED("mutiple.uom.enabled", Constants.FALSE,
            "If true then multiple uom can be placed in same orderDetail"),

    MULTIPLE_UOM_ENABLED_FOR_DISCOUNT_QUANTIY_CALCULATION("mutiple.uom.enabled.for.discount.quantity.calculation", Constants.FALSE,
            "If true then multiple uom can be placed in same orderDetail discount"),
    USE_NATIVE_RANGE_PROGRAM("use.native.range.program", Constants.FALSE,
            "If true then the range program controller will use native query to get the range programs"),

    ENABLE_CONVERTION_OF_FREE_ITEM("convertion_of_free_item", Constants.FALSE,
            "If true then free item quantity will convert to case and piece according to eligible free items."),

    CALCULATE_PERCENTAGE_SCHEME_ON_INITIALAMOUT("CALCULATE_PERCENTAGE_SCHEME_ON_INITIALAMOUT", Constants.FALSE,
            "It will calculate multiple percentage scheme on its initial price only."),

    APPLICATION_CATETORY("application.category", "",
            "Category of the application like SFA, RETAIL etc", Constants.STRING),
    DAYS_BEFORE_DELIVERY("days.before.delivery", "0", "Days Before Delivery", Constants.STRING),

    KEY_IN_EXTENDED_ATTRIBUTES_FOR_APP_ACCESS_USAGE("key.in.extended.attributes.for.app.access.usage", "",
            "Key to be defined in extended Attributes that should be used for app access usage the value of which should be Y or N"),

    ADD_ACTIVE_STATUS_FILTER_FOR_RECOMMENDATIONS("add.active.status.filter.for.recommendations", Constants.FALSE,
            "If true then the active status filter would be added to recommendation order get query"),

    ENABLE_TYPE_CHECK_IN_FAQ("enable.faq.type.check", Constants.FALSE,
            "If true then the additional type checking will be performed while saving the FAQ record"),

    ENCRYPT_REPORT_GENERATION("encrypt.all.report.generation", Constants.FALSE,
            "If true then report Excel and PDF except CSV will be encrypted"),

    USE_DISCOUNTINFO_AS_FREEITEMINFO("use.discountInfo.as.freeitemInfo", Constants.FALSE,
            "If true then for that client it will look into disCountInfo in order body and if available will copy that into freeItemInfo"),

    ORDER_REMARKS_REPLACE_SPECIAL_CHARACTERS("order.remarks.replace.special.char", "[-@+|=():/~`';\\\\]",
            "In this property we defined a special character to be replaced in the order remarks"),

    SET_OUTLETCODE_AS_ORDER_LOGINID("set.outletcode.as.order.loginid", Constants.FALSE,
            "If true then set outletCode as order loginId"),

    TINY_URL_ENABLED("tiny.url.enabled", Constants.FALSE,
        "Enable tiny url"),
    CALCULATE_TAX_ON_SCHEME("calculate.tax.on.scheme", Constants.FALSE,
            "calculate tax in each aggregatedScheme from ck_tax"),

    // ---- configurations for calculations logic for new scheme module start

    CUSTOM_INDEX_ON_SCHEME_OUTLET_QUERY("custom.index.on.scheme.outlet.query", Constants.FALSE,
            "to use it for adding custom index usage in query for outlet"),
    ROLLBACK_RECURRENCE_TYPE("rollback.recurrence.type", "ULR,OCL", "recurrence types to be rolled back on order"),
    COUPON_ROLLBACK_ORDER_STATUS("coupon.rollback.order.status", "0", "rollback discounts on order status expired or rejected"),
    CONSIDER_TIME_RANGE_COUPON_ROLLBACK("consider.time.range.coupon.rollback", Constants.FALSE, "rollback discounts on order status expired or rejected"),

    SCHEME_FETCH_FREE_PRODUCT_LIST("scheme.fetch.free.product.list", Constants.FALSE,
            "if true then the list of free products will be fetched in the range program"),

    EXCLUSION_OF_APPLIED_SCHEME_ON_ITEM("exclusion.of.applied.scheme.on.item", Constants.FALSE,
            "to not consider batchcode in group scheme on which scheme is already applied"),

    SORT_SCHEME_ON_PRIORITY("sort.scheme.on.priority", Constants.FALSE,"to sort schemes on priority before applying"),
    SCHEME_ORDER_REJECTION_STRING("scheme.order.rejection.string","REJECTED","it is the status value for the rejected order"),
    SCHEME_SHOW_WELCOME_AFTER_REJECTION("scheme.show.welcome.after.rejection", Constants.FALSE,"if true it applies the welcome order coupon if the order status is rejected"),

    CONSIDER_NEAREST_SLAB("consider.nearest.slab", Constants.FALSE,"if scheme does not lie in scheme slabs, consider the" +
            "nearest eligible slab"),

    IS_FREE_PRODUCT_PRICED("is.free.product.priced", Constants.FALSE,"is free product priced"),

    PROPORTIONATE_GROUP_DISCOUNT("proportionate.group.discount", Constants.TRUE,"to proportionate group discount as per bought quantity"),

    USE_NEW_SCHEME_ENDPOINT("use.new.scheme.endpoint", Constants.FALSE,"to use new scheme endPoint while calling old endpoint"),

    CALCULATE_PERCENTAGE_DISCOUNT_ON_MRP("calculate.percentage_discount_on_map", Constants.FALSE,"to calculate percentage discount on mrp"),

    TO_APPLY_ONLY_TOP_PRIORITY_SCHEME("to.apply.only.top.priority.scheme", Constants.FALSE,"whether to check condition for applying scheme for top Priority case" ),

    BLOCKING_INDICATOR("check.outletDivision.for.blockIndicator.for.specific.client", Constants.FALSE , "if true the outletDivision column will be checked and if 1 scheme will not be delivered"),

    PRODUCT_DETAILS_CACHE("product.details.cached", Constants.FALSE,
            "if true , enables product details cache", Constants.BOOLEAN),
    CONSIDER_STOCK_WHILE_FETCHING_PRODUCTS("consider.stock.while.fetching.products", Constants.TRUE , "if false then it will not consider stock while fetching products"),
    MUST_BUY_ON_PRODUCT_ATTRIBUTES("must.buy.on.product.attributes", Constants.FALSE , "if true then it will take all product attributes for must buy"),
    COUPON_VISIBILITY_AFTER_USAGE("coupon.visibility.after.usage", Constants.FALSE, "if true then coupons will not be visible after its usage"),

    BIFURCATE_SCHEME_BY_SLAB_IN_FETCH_API("bifurcate.scheme.by.slab.in.fetch.api", Constants.TRUE,"to bifurcate scheme in multiple entries if same schemeId have multiple slabs"),

    CALCULATE_PRIORITY_ON_ATTRIBUTES("calculate.priority.on.attributes", Constants.FALSE,"to use outlletcode and itemcode to get which scheme to apply"),
    GET_ROUNDED_OFF_BENEFIT("get.rounded.off.benefit", "0", "Get Rounded off value upto n decimal places", Constants.STRING),
    GET_SCHEMES_NATIVE_QUERY("get.schemes.native.query", Constants.FALSE, "get the schemes from the native query", Constants.BOOLEAN),
    SCHEME_CUSTOM_OUTLET_JOIN_QUERY("scheme.custom.outlet.join.query", "0", "will use a custom inner join for scheme query ", Constants.STRING),
    OUTLET_HAS_MULTIPLE_SUPPLIER_SCHEMES("outlet.has.multiple.supplier.schemes", Constants.FALSE, "if true schemes " +
            "will be fetched and applied for all the suppliers",
            Constants.BOOLEAN),
    TOTAL_ORDER_COUPON_ON_NET_AMOUNT("total.order.coupon.on.net.amount", Constants.FALSE, "the total_order coupon will be applied on net amount after calculating all discounts"),

    FREE_PRODUCT_FROM_ITEM_ID("free.product.from.item.id", Constants.FALSE, "to get free product from item_id from generic scheme transformer"),

    DYNAMIC_SCHEME_ID_HANDLING("handling.schemeId.dynamically", Constants.FALSE,"this config update if there an id changed in schemes tables"),
    ROUND_OFF_PERCENTAGE_DISCOUNT("it.round.off.dissount.till.8.deciaml.places", Constants.TRUE,"it roundOff percentage discount till 8 decimal places"),

    CHECK_MARKET_DATES("check.expiration.on.market.details", Constants.FALSE,"this property verify the start and end date in market data for schemes"),
    MINIMUM_AMOUNT_ONLY_ON_HEADER_LEVEL("minimum.amount.only.on.header.level", Constants.FALSE, "to send ILC discount info only on header level in minimum amount case"),
    MULTIPLY_BENEFIT_ON_MUST_BUY_GROUP("multiply.benefit.on.must.buy.group", Constants.FALSE, "to multiply the benefit on must buy grouping"),
    GET_FREE_PRODUCT_DETAILS_BY_SKU_CODE("get.free.product.details.by.sku.code", Constants.TRUE, "this config toggle to get free batchCode productDetails form skuCode or batchCode"),
    GET_SCHEME_CALCULATION_TYPE("get.scheme.calculation.type", Constants.FALSE,"to get scheme calculation type in extended attributes"),
    GET_SCHEME_NAMING("get.scheme.naming", Constants.FALSE, "to get modified scheme scheme description in exteneded attributes"),


    OUTLET_LEVEL_QUERY_FILTER_IN_SFA("outlet.level.query.filter.in.sfa", Constants.FALSE,"to add outlet corresponding to salesrep filter in query for sfa"),

    BUDGET_PERCENTAGE_VALUE("budget.percentage.value","20","to give budget percentage", Constants.STRING),
    PRIORITY_ON_EACH_ORDER_ITEM("priority.on.each.order.item", Constants.FALSE, "to return scheme with highest priority on a single sku"),

    SLAB_BASED_FOC("slab.based.foc", Constants.FALSE, "to consider slab while giving free product (different slabs having different FOC products)"),
    // ---- configurations for calculations logic for new scheme module end


    USE_NORMALISED_HIERARCHY_FOR_OUTLET("use.normalised.hierarchy.for.outlet", Constants.FALSE,
            "If true then for that client we will check make normalised hierarchy check when finding outlets by hierarchy"),
    USE_HIERARCHY_FROM_HIERARCHY_METADATA("use.hierarchy.from.hierarchy.metadata", Constants.FALSE,
            "If true then for that client we will check make hierarchy check from hierarchy_metadata table when finding outlets by hierarchy. Note: please make sure that USE_NORMALISED_HIERARCHY_FOR_OUTLET is false,a both features conflicts"),
    SALES_PERSON_DESIGNATION("salesrep.designation", "salesrep",
            "Corresponding value will be treated as salesperson designation"),

    ANALYTICS_REPORT_API_ACCESS("analytics.report.api.access.designation", "all",
            "Here the designation who has access to AnalyticsReportController"),

    IGNORE_OUTLET_CONTROLLER_ROW_COUNT("ignore.outlet.controller.rowcount", Constants.FALSE,
            "if true then the even though the pagination present in the request body the total row count will be ignored"),

    IGNORE_SECONDARY_PRODUCT_ROW_COUNT("ignore.secondary.product.rowcount", Constants.FALSE,
            "if true then row count for secondary products will be ignored"),

    IGNORE_GENERIC_ENTITY_CONTROLLER_ROW_COUNT("ignore.generic.entity.controller.rowcount", Constants.FALSE,
            "if true then the even though the pagination present in the request body the total row count will be ignored"),

    DISABLE_PRICE_VALIDATION("disable.price.validation", Constants.FALSE,
            "If true, then it will disable price validation for order", Constants.BOOLEAN),

    IGNORE_ORDER_RETURN_SKU_PRICE("ignore.order.return.price.validation", Constants.FALSE,
            "If true will ignore order sku price validation"),

    ALLOW_DUCLICATE_SKU_ORDER("allow.duplicate.sku.in.single.order", Constants.FALSE,
            "If true it will accept duplicate sku same batchcode in same order, while order and order return sku are same"),

    BYPASS_DEVICE_CHECK("bypass.device.check", Constants.FALSE,
            "If true then device check will not be performed when sending firebase notifications."),

    ADD_ONLY_DATA_KEY_FIREBASE("add.only.data.key.firebase", Constants.FALSE,
            "If true then will send data key when app type is null when sending firebase notifications."),

    ROUNDOFF_NATIVECDMMAPPER("roundoff.nativeCdmMapper", Constants.TRUE,
            "if trueroundOff will be done in native cdm mapper else roundoff will be disabled"),
    USE_NEW_FIREBASE_MODULE("use.new.firebase.module", Constants.FALSE,
            "If true, program will also set firebase token in device info table. Firebase token will be saved for each device"),

    FEATURE_ON_PROD("feature.on.prod", Constants.FALSE,
            "If true, program will work on prod env"),
    CALCULATE_ON_MIN_QTY("calculate.on.min.qty", Constants.FALSE,
            "if true, program will check if the order qualifies the minimum quantity of product for scheme to be applicable."),
    GET_WEEK_VALUE_YEAR_WISE("get.year.wise.week.value", Constants.FALSE,
            "if true , the week value will be generated year wise"),

    UPDATE_OUTLET_HIERARCHY_BASED_ON_PARENT("update.outlet.hierarchy.for.parents", Constants.FALSE,
            "if true, it will try to update outlet hierarchies based on the parent data. (Works only for Vistaar)"),

    REGISTER_THROUGH_OTP_ALLOWED("register.through.otp.allowed", Constants.FALSE,
            "allow only if you want to create user's and outlet's new entry in db if it's not there."),

    CHECK_STOCK_IN_PRICING("check.stock.in.pricing", Constants.FALSE,
            "if true, we will get the pricing for only the products for which the stock is available."),

    CHECK_UF_IN_PRICING("check.uf.in.pricing", "",
            "if true, we will get the UF Products in pricing response"),

    IS_REDIRECTION_ENABLED("is.redirection.enabled", Constants.FALSE,
            "if yes then the notification payload will not contain the notification object, it will just contain the data object"),

    CHECK_MOV_TYPE("check.mov.type", "",
            "the value will decide on which factor mov will work, if empty will use default order config value"),

    FILEULOAD_ALLOWED_MAGIC_BYTES("fileupload.allowed.magicbytes.inhex", "[{\"o\": \"0\", \"b\": [\"504b0304\"]},{\"o\": \"0\", \"b\": [\"89504e470d0a1a0a\"]},{\"o\": \"0\", \"b\": [\"ffd8\"]},{\"o\": \"0\", \"b\": [\"474946383761\",\"474946383961\"]},{\"o\": \"0\", \"b\": [\"424d\"]},{\"o\": \"0\", \"b\": [\"504b0304\"]},{\"o\": \"0\", \"b\": [\"d0cf11e0a1b11ae1\"]},{\"o\": \"0\", \"b\": [\"25504446\"]},{\"o\": \"0\", \"b\": [\"d0cf11e0a1b11ae1\"]},{\"o\": \"0\", \"b\": [\"ffd8ff\"]},{\"o\": \"4\", \"b\": [\"66747970\"]},{\"o\": \"0\", \"b\": [\"52494646\"]},{\"o\": \"0\", \"b\": [\"00000100\"]},{\"o\": \"0\", \"b\": [\"504b0304\"]},{\"o\": \"0\", \"b\": [\"504b0304\"]}]",
            "Here we keep all allowed files magic byte in hexadecimal"),

    OUTLET_PRODUCT_MINIMAL_RESPONSE("get.outlet.product.minimal.response", Constants.FALSE,
            "if true, it will fetch and return only column required"),

    USE_ROUNDOFF("use.roundoff", Constants.TRUE,"if true , the value will be roundoff else roundoff will be disabled and value will come upto 2 digits after decimal."),

    ENABLE_ENHANCED_NAMING("enhancedNaming", Constants.FALSE,"if true , it will enable enhanced product naming"),

    ENHANCED_NAMING_SEQUENCE("namingSequence","brand,sub_category,flavour,packSize,packType","if true , it will enable enhanced product naming"),

    QUERY_ADAPTER_READ_REPLICA_ENABLED("query.adapter.read.replica.enabled", Constants.FALSE, "If true the sql adapters will execute all queries in read replica"),

	LOYALTY_CALCULATION_ENABLED("loyalty.module.enabled", Constants.FALSE,"If true , it will enable calcualation for loyalty module ."),

	LOYALTY_IMPLEMENTATION_QUALIFIED_CLASS_NAME("loyalty.module.classpath","com.applicate.marsbel.rewardprogram.RewardProgramImpl","We have defined here fully qualified class path for reward program calculation."),
    CHECK_FOR_DATA_SOURCE_ORDER("check.for.data.source", Constants.FALSE,"if true it will check for data source for orders if it coming from any other source than applicate while sending order data"),

    GET_STOCK_AS_PER_OUTLET("get.stock.as.per.outlet", Constants.FALSE,"it will bring stock as per outlet if this property is enabled"),
    GET_MANUFACTURING_DATE_FROM_STOCK_IN_PRICING("manufacturing.date.in.pricing", Constants.FALSE, "If true, will get manufacturing date from STOCK in Pricing"),
    ENABLE_VONAGE_OTP("enable.vonage.otp", Constants.FALSE, "If true, vonage otp service provider will be used, otherwise gupshup would be used"),
    USE_EVENT_LISTENER_FOR_STOCK_DEDUCTION("use.event.listener.for.stock.deduction", Constants.FALSE, "If true, stock deduction will happen via event listener"),
    SALES_REP_HAS_SUPPLIER("salesRep.has.supplier", Constants.TRUE, "if the given salesRep has supplier then set it true, this is used for schemes SFA"),
    RECOMMENDED_ORDER_MINIMAL_RESPONSE("recommended.order.minimal.response", Constants.FALSE, "If true, than it will add minimal response key true to request params getting in api"),
    RECOMMENDED_ORDER_ADD_DATE_CHECK("recommended.order.add.date.check", Constants.TRUE, "If false, than it will not check start date and end date while fetching data"),
    EXCLUDE_APIS_FROM_OTP_SECURITY("exclude.apis.from.otp.security", "outletDetailsPost", "if an api name is added, then otp security will be removed from that api"),
    FILTER_BY_SUPPLIER_IN_BARCODE("filter.by.supplier.in.barcode", Constants.FALSE ,"If true, it will filter the product barcode based on supplier"),
    TURN_OFF_CREATE_HIERARCHY_IN_USER_UPDATE_EVENT_LISTENER("turn.off.create.hierarchy", Constants.FALSE,"If true, create hierarchy won't be called and synchronise job will pass rebuildAll as false"),
    USE_LOGIN_ID_FOR_RECOMMENDATION("use.loginid.for.recommendation", Constants.FALSE,"If true than it will take recommendations as per loginId for compliance listener"),
    LOGS_FOR_ANALYTICS("logs.for.analytics", Constants.FALSE, "If true, than it will log the timings for controller, publish method, receiving and writing to DB v1/analytics"),

    USERMETADATA_FOR_MULTIPLE_MOBILE("usermetadata.for.multiple.mobile", Constants.FALSE,"If true then we check for mobile number in user metadata during login"),

    CREDIT_STATUS_UPDATE("credit.status.update", Constants.FALSE,"If true then, will check credit status of that order."),
    USE_PRODUCT_METADATA_CASE_TO_PIECE_QUANTITY("use.productmetadata.case.to.piece.quantity", Constants.FALSE,
            "If true then caseToPieceQuantity will used from the product metadata"),
    IS_CREDIT_IS_OUTLET_WISE("credit.outlet.wise", Constants.FALSE,"Credit is outlet wise or not."),

    ALLOW_ORDER_WITH_INVALID_PARENT_USER("allow.order.with.invalid.parent.user", Constants.FALSE,"if true order with invalid parent user will be allowed to place and no error will be thrown"),
    FILTER_SUPPLIER_BY_OUTLET("filter.supplier.by.outlet", Constants.FALSE,"it will bring supplier as per outlet if this property is enabled"),

    CUSTOM_EXPIRY_FOR_DELIVERYDATE_ENALBED("custom.expiry.for.deliverydate.enabled", Constants.FALSE,"Cutoff time while calculating delivery date"),
    HOLIDAYS_CONFIG_FOR_DELIVERYDATE("holiday.config.for.deliverydate","IGNORE" , "If INCREMENT then holidays counted in lead time, if SKIP then holidays not counted in lead time, else IGNORE for ignoring this configuration"),
    UNIQUE_MEDIA_FILE_NAME("unique.media.file.name", Constants.TRUE,"If true all media files uploaded to cloud storage will have a unique string appended(except for zip)"),

    BATCH_SIZE_FOR_SHIP_TO_DETAILS("batch.size.for.ship.to.details","100","Batch size of outlets to add shipto details"),

    CHECK_DD_USER_WHILE_STOCK("pass.stock.check.for.dd.user", Constants.FALSE,"does not apply stock check for dd users."),
    SHORT_TOKEN_LENGTH("short.token.length", "10", "Based on this length new short tokens will be created. The value should be in the range of 10 - 100"),

    TOKEN_CACHE_TTL("token.cache.ttl", "24", "Distributed cache token ttl in hours "),
    LEADERBOARD_KPI_NAME("leaderboard.kpi.name","kpi_rewards","Kpi name for leaderboard can be configured here"),
    LEADERBOARD_KPI_NAME_V2("leaderboard.kpi.name.v2","kpi_rewards_2","Kpi name for leaderboard v2"),
    USER_MASK_TRANSFORMER("user.mask.transformer", "", "If you want to enable user masking please provide the transformer name"),
    IS_NOTIFICATION_ENABLED("notification.enabled", "true", "if true it will trigger slack notification"),
    PORTAL_SESSION_DURATION("portal.session.duration", "0", "Set the duration in seconds. If it's 0 then default duration will be provided"),
    APP_SESSION_DURATION("app.session.duration", "0", "Set the duration in seconds. If it's 0 then default duration will be provided"),
    USE_INTERNAL_OTP_SERVICE("use.internal.otp.service", Constants.FALSE,"Use otp service from channelkart to generate and verify otp, requires custom vendor profile to send otp"),
    OTP_EXPIRY_TIME("otp.expiry.time","15","minutes after which otp will expiry when using internal otp service"),
    USER_OTP_LENGTH("user.otp.length","6","length of otp when using internal otp service"),
    ENABLE_SINCH_OTP("enable.sinch.otp", Constants.FALSE, "If true, sinch otp service provider will be used, otherwise gupshup would be used"),
    IS_FIELD_WISE_HOLIDAY("is.field.wise.holiday", Constants.FALSE, "If true, the field wise holiday is considered, otherwise the holiday config in business calender is used"),
    IS_DEMO_OTP("is.demo.otp", Constants.FALSE, "If true, demo otp will be generated and will be send to the demo number"),
    ENABLE_NEW_OTP_SERVICE("enable.new.otp.service", Constants.FALSE, "If true, new otp service will be executed"),
    ENABLE_OTP_SERVICE("enable.otp.service", "gupshup", "Specify the otp service that you want to enable for your client. Available otp services are: gupshup, internalOtp, sinch, vonage"),
    BYPASS_APPROVAL_ID_CHECK("bypass.approval.id.check", Constants.FALSE, "for existing outlet approval if true then only checks for the role"),
    UNIQUE_DESIGNATION_IN_HIERARCHY("unique.designation.in.hierarchy", Constants.FALSE, "Will keep only first user of each designation in hierarchy"),
    ORDER_FREEZE_TIME_RANGE("freeze.order","{\"enabled\": false,\n" +
            "    \"startTime\": \"10:00\",\n" +
            "    \"endTime\": \"18:00\"}","order cannot be placed in provided time between"),
    
    PRODUCT_DETAILS_MASTER_CODE("product.details.master.code","itemClass","It will set mastercode by default it will set to itemClass"),
    IGNORE_ORDER_TYPE("ignore.order.type","[]", "ignoring these order type for orderDiscount enrichment"),
    LOGS_FOR_NULL_LOCATION("logs.for.null.location", Constants.FALSE,"If true, will print logs for null location"),
    ORDER_DELIVERY_DATE_AS_PER_PJP_DATE("order.delivery.date.as.per.pjp_date", Constants.FALSE,"order delivery date " +
            "will be calculated on the basis of pjp date"),
    FILTER_REROUTE_OUTLETS("filter.reroute.outlets", Constants.FALSE,"If true,it will filter out rerouted outlets"),

    SECURITY_STRICT_ACCESS_CHECK_ENABLED("security.strict.access.check.enabled", Constants.TRUE, "If true it will restrict the access of invalid data access"),
    SECURITY_STRICT_TOKEN_CHECK_ENABLED("security.strict.token.check.enabled", Constants.FALSE, "If true it will restrict the access of invalid token access if we enable only short token and encrypted token then it will only allow those token which are in the list"),
    SECURITY_REJECT_QUERY_API_REQUESTS("security.reject.query.api.requests", Constants.FALSE, "If true it will not allow non admin  users to call the query api or check for designation in query info"),
    SECURITY_CUSTOM_REPORT_CHECK("security.custom.report.check", Constants.FALSE, "If true it will check for custom report access"),
    USE_BASKET_ID_FOR_RECOMMENDATION("use.basketid.for.recommendation", Constants.FALSE,"If true than it will take recommendations as per basketid for compliance listener"),
    ALLOW_OUTLET_CREATION_WITH_INVALID_PARENT_USER("allow.outlet.creation.with.invalid.parent.user", Constants.FALSE,"if true outlet creation with invalid parent user will be allowed and no error will be thrown"),
    CREATE_GRN_FOR_INVOICE("create.grn.for.invoice", Constants.FALSE,"if true grn will be created for invoice"),
    ENABLE_TRANSFORMATION_IN_SALES_STREAM("enable.transformation.in.sales.stream", Constants.FALSE,"If true than it will transform the data as per the operation given in the config map"),
    CREATE_ORDER_DETAILS_FROM_FINALORDER_INFO("create.order.details.from.final.order.info", Constants.FALSE,"if true order details will be created from final order info if order details is null and final order info is not null"),
    MULTIPLE_UOM_ENABLED_ON_SALES("mutiple.uom.enabled.on.sales", Constants.FALSE,
            "If true then multiple uom can be placed in same salesDetail"),
    USE_KAFKA_FOR_PROCESSING_ORDER_STATUS_BATCH_UPDATE("use.kafka.for.order.status.batch.processing", Constants.FALSE,"If true order status batch api update will happen through kafka"),
    THROW_ORDER_DATA_ALREADY_EXISTS_IF_EMPTY_ORDER_DETAILS_WITH_EXISTING_REFERENCE_NUMBER("throw.409.if.order.with.given.reference.number.exists.but.order.details.is.empty", Constants.FALSE,"if true it will throw 409 in case of order has an empty order details but order with provided reference number already exists"),
    CHECK_PJP_IN_PRICING("check.pjp.in.pricing", Constants.FALSE, "If true it will check pjp in pricing"),
    USE_TRANSFORMER_FOR_QUERY_RESPONSE("use.transformer.for.query.response", Constants.FALSE, "If set to true, it will check the transformer info for a transformer with the same name as the query name and use that to transform the query result."),
    OUTLET_MINIMAL_RESPONSE("get.outlet.minimal.response", Constants.FALSE, "if true, it will fetch and return only column required");


    PropertyDefinition(String name, String defaultValue, String description) {
        this(name, defaultValue, description, null);
    }

    PropertyDefinition(String name, String defaultValue, String description, String type) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.description = description;
        this.type = type;
    }

    private final String name;

    private final String defaultValue;

    private final String description;

    private final String type;

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public static Optional<PropertyDefinition> findByName(String name) {
        return Optional.ofNullable(Registry.getByName(name));
    }

    /**
     * Added this class for improving the lookup performance
     */
    private static class Registry {

        private static final Map<String, PropertyDefinition> DEFINITION_MAP;

        static {
            DEFINITION_MAP = Arrays.stream(PropertyDefinition.values())
                    .collect(Collectors.toMap(PropertyDefinition::getName, item -> item));
        }

        public static PropertyDefinition getByName(String name) {
            return DEFINITION_MAP.get(name);
        }
    }

    private static class Constants {
        public static final String FALSE = "false";
        public static final String TRUE = "true";
        public static final String BOOLEAN = "boolean";
        public static final String STRING = "String";
    }
}
