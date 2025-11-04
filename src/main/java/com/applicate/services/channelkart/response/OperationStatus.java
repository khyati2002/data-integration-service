package com.applicate.services.channelkart.response;

public enum OperationStatus {

	Success, Failure,PREPROCESS_PIPELINE_FAILURE,INTERNAL_SERVER_FAILURE,DATABASE_FAILURE;

	public static OperationStatus from(boolean isSuccess) {
		return isSuccess ? Success : Failure;
	}

	public boolean isSuccess() {
		return this == Success;
	}

}
