package com.salescode.channelkart.transformers;

public interface Transformer<S,T>{
	
	Object transform(S s);
	
	Class<S> sourceType();

}
