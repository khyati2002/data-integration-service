package com.applicate.services.channelkart.pojo;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.response.OperationStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

public class MdmOperationResponse {

    @Setter
    @Getter
    private Map<String,Object> input;

    @Setter
    @Getter
    private int retryCount= 0;

    @Setter
    @Getter
    private OperationStatus status= OperationStatus.Failure;
	
	@SuppressWarnings("rawtypes")
	private Future<Map<Class,Set<CommonDataModel>>> response;

    @Setter
    @Getter
    private String error="";

    @Setter
    @Getter
    private int attempt= 0;
	
	@SuppressWarnings("rawtypes")
	public Future<Map<Class, Set<CommonDataModel>>> getResponse() {
		return response;
	}

	@SuppressWarnings("rawtypes")
	public void setResponse(Future<Map<Class, Set<CommonDataModel>>> response) {
		this.response = response;
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
