package com.applicate.services.channelkart.response;

import java.util.ArrayList;
import java.util.Collection;

public class ReadResponse<T> {
	
	private Collection<T> features;
	
	public ReadResponse(Collection<T> features) {
		
		this.features=features;
	}
	public ReadResponse(T feature) {
		Collection<T> rc = new ArrayList<>();
		rc.add(feature);
		this.features=rc;
	}
	
	public Collection<T> getFeatures() {
		return features;
	}

	public void setFeatures(Collection<T> features) {
		this.features = features;
	}

}
