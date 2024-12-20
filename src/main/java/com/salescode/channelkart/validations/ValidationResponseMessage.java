package com.salescode.channelkart.validations;

public final class ValidationResponseMessage {
	
	public static final String INVALIDFORMAT = "The input is not as expected.";
	public static final String NOTNULL ="Field cannot be null";
	public static final String FOUND_NULL = "{} argument found NULL";
	public static final String FOUND_SPACES = "{} argument should not contain spaces";
	public static final String NOTBLANK ="Field cannot be blank. It must contain some value.";
	public static final String NOT_NULLBLANK_KEY = "{}: cannot be null or empty.";
	public static final String LENGTH ="Length of data should be less than 200";
	public static final String SKU_REGEX = "Can only contain [a-z][A-Z][0-9] and special characters '&' '-' '_'  .Space cannot be used in skuCode";
	public static final String SPLIT_KEY_VALIDATION = "Field {} must have some non-empty value.";
	public static final String NO_LOCATION = "Location field {} not present for {} . Please provide data first.";
	public static final String LOCATION_NOTFOUND = "Location details not found while getting data for splitkeys.";
	public static final String SUPPLIER_NOTFOUND = "Supplier details not found while getting data for splitkeys.";
	public static final String SUPPLIER_NOTFETCHED = "Cannot fetch supplier details from object";
	public static final String SKU_NOT_FOUND = "No Product found with given itemID.";
	public static final String INVALID_JSON = "JSON Object could not be evaluated.";
	public static final String INVALID_SPLITKEY = "Check for invalid splitkey in {} of class {}.";
	public static final String SPLITKEY_NOT_FOUND = "SplitKey configuration with name {} not present. Provide a configuration before uploading data.";
	public static final String SPLITKEY_KEY_ERROR = "key {} not found in splitkeys configuration.";
	public static final String FINDKEY_KEY_ERROR = "Key : {} not found in table for class type : {}. Please check findkey configuration.";
	public static final String FINDKEY_DATA_ERROR = "{} details found null or empty for entity {}, value {}";
	public static final String EMPTY_DIVISION = "No result found for Division. Please provide Division data first.";
	public static final String INCORRECT_SHEET_NAME = "Provided sheet name {} instead of {}. Please provide sheet name similar to {}";
	public static final String EXCEL_READ_ERROR = "Cannot read excel due to some issues with excel sheet. Remove any formatting and try again.";
	public static final String RECORD_ID_NOTFOUND = "Record with id {} not found in {}";
	public static final String MISSING_SPLITKEY_CONFIGURATION = "Please verify and reconfigure split key configuration {} for {}.";
	public static final String MISSING_SPLITKEY_CONFIGURATION_CLASS = "Splitkey configuration missing for class {}. Please add a configuration.";
	public static final String INVALID_DATE_ERROR = "Date {} is in invalid format. Correct Format is : yyyy-MM-dd HH:mm:ss";
	public static final String ERROR_TRANSFORMING = "Unable to transform input with target object : {}";
	public static final String INVALID_INPUT_FOR_TRANSFORMER = "Unable to transform input data {}. Check that data is in correct format for column {}";
	public static final String INVALID_USER_ROLE = "Role {} is invalid. Please check again.";
	public static final String NULL_TRANSFORMATION_RESULT = "Failed to transform data due to mismatch transformer and header keys. Make sure correct template or correct transformer is being used.";
	public static final String DIVISION_USEREXIST_VALIDATION_MSG = "User hierarchy '{}' cannot be removed untill linked users migrates to some other division. Please migrate them first and retry.";
	public static final String DIVISION_IS_PARENT_OF_OTHER_DIVISION = "User hierarchy '{}' is already defined as parent for {}. You cannot remove user hierarchy untill you remove its dependency";
	public static final String MDM_OPERATION_FAILURE = "Couldn't save data due to internal failure. Contact Administrator.";
	public static final String OUTLET_NONRETAILER_ON_RETAILER = "Loginid : '{}' already present in system with non-retailer designation. Looks like you uploading invalid record as retailer.";


	/* Error messages while Uploading File */
	public static final String BLANK_FILENAME = "Blank file name detected. Please rename the file and try again.";
	public static final String INVALID_FILENAME_SEQUENCE = "Filename contains invalid sequence '..' Remove the sequence and try again. ";
	public static final String FILE_NOT_STORED = "Could not store file. Please try again! Reason: {}";
	
	//EntityApproval validation error massages
	
	public static final String REFERENCEID_NOT_EMPTY  ="ReferenceId cannot be empty ";
	public static final String ENTITYTYPE_NOT_EMPTY = "EntityType is cannot be empty";
	public static final String REFERENCEID_NOT_FOUND = "Reference Id not found in the target Table {} for id : {}";
	public static final String APPROVERID_NOT_FOUND = "ApproverId is empty , check in the database any user present for this role {}";
	public static final String UPDATEDATA_IS_EMPTY = "No data found to update to target table";
	public static final String INVALID_TARGET_TABLE = "Target table not found {} ";
	
	private ValidationResponseMessage() {
		throw new AssertionError();
	}
	

}
