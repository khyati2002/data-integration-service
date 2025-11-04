package com.applicate.services.channelkart.exceptions;
public class EnrichmentFailException extends RuntimeException {

	private static final long serialVersionUID = -6656924970905878563L;

	private final String errorCode;


	public EnrichmentFailException() {
		super();
		this.errorCode = null;
	}

	public EnrichmentFailException(String message) {
		super(message);
		this.errorCode = null;
	}

	public EnrichmentFailException(String message,String errorCode) {
		super(message);
		this.errorCode = errorCode;
	}

	public EnrichmentFailException(String message, Throwable cause) {
		super(message, cause);
		this.errorCode = null;
	}

	public EnrichmentFailException(Throwable cause) {
		super(cause);
		this.errorCode = null;
	}


	protected EnrichmentFailException(String message, Throwable cause, boolean enableSuppression,
									  boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.errorCode = null;
	}

	public String getErrorCode() {
		return errorCode;
	}


}
