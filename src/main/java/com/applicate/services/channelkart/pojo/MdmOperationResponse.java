package com.applicate.services.channelkart.pojo;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.response.OperationStatus;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

public class MdmOperationResponse {

	private Map<String,Object> input;

	private int retryCount= 0;

	private OperationStatus status= OperationStatus.Failure;

	@SuppressWarnings("rawtypes")
	private Future<Map<Class,Set<CommonDataModel>>> response;

	private String error="";

	private int attempt= 0;

	@SuppressWarnings("rawtypes")
	public Future<Map<Class, Set<CommonDataModel>>> getResponse() {
		return response;
	}

	@SuppressWarnings("rawtypes")
	public void setResponse(Future<Map<Class, Set<CommonDataModel>>> response) {
		this.response = response;
	}

	/**
	 * @return the input
	 */
	public Map<String, Object> getInput() {
		return input;
	}

	/**
	 * @param input the input to set
	 */
	public void setInput(Map<String, Object> input) {
		this.input = input;
	}

	/**
	 * @return the error
	 */
	public String getError() {
		return error;
	}

	/**
	 * @param error the error to set
	 */
	public void setError(String error) {
		this.error = error;
	}

	/**
	 * @return the preProcessStatus
	 */
	public OperationStatus getStatus() {
		return status;
	}

	/**
	 * @param preProcessStatus the preProcessStatus to set
	 */
	public void setStatus(OperationStatus status) {
		this.status = status;
	}

	/**
	 * @return the retryCount
	 */
	public int getRetryCount() {
		return retryCount;
	}

	/**
	 * @param retryCount the retryCount to set
	 */
	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

	/**
	 * @return the attempt
	 */
	public int getAttempt() {
		return attempt;
	}

	/**
	 * @param attempt the attempt to set
	 */
	public void setAttempt(int attempt) {
		this.attempt = attempt;
	}

	@Override
	public String toString() {
		return "MdmOperationResponse{" +
				"input=" + input +
				", retryCount=" + retryCount +
				", status=" + status +
				", response=" + response +
				", error='" + error + '\'' +
				", attempt=" + attempt +
				'}';
	}
}
