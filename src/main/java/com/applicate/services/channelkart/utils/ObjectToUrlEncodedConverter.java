///*
//*Copyright Applicate(2021) To Present
//*
//*All rights reserved
//*/
//package com.applicate.services.channelkart.utils;
//
//import com.bazaarvoice.jolt.JsonUtils;
//import com.fasterxml.jackson.annotation.JsonAnySetter;
//import org.springframework.http.HttpInputMessage;
//import org.springframework.http.HttpOutputMessage;
//import org.springframework.http.MediaType;
//import org.springframework.http.converter.HttpMessageConverter;
//import org.springframework.http.converter.HttpMessageNotReadableException;
//import org.springframework.http.converter.HttpMessageNotWritableException;
//
//import java.io.IOException;
//import java.io.UnsupportedEncodingException;
//import java.net.URLEncoder;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//
///**
// * The class ObjectToUrlEncodedConverter.
// *
// * @author  Manish Srivastava
// * @since   Mar 2021
// */
//public class ObjectToUrlEncodedConverter implements HttpMessageConverter{
//
//	/** The Constant Encoding. */
//	private static final String Encoding = "UTF-8";
//
//	/**
//	 * Can read.
//	 *
//	 * @param clazz the clazz
//	 * @param mediaType the media type
//	 * @return true, if successful
//	 */
//	@Override
//	public boolean canRead(Class clazz, MediaType mediaType)
//	{
//		return false;
//	}
//
//	/**
//	 * Can write.
//	 *
//	 * @param clazz the clazz
//	 * @param mediaType the media type
//	 * @return true, if successful
//	 */
//	@Override
//	public boolean canWrite(Class clazz, MediaType mediaType)
//	{
//		return getSupportedMediaTypes().contains(mediaType);
//	}
//
//	/**
//	 * Gets the supported media types.
//	 *
//	 * @return the supported media types
//	 */
//	@Override
//	public List<MediaType> getSupportedMediaTypes()
//	{
//		return Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED);
//	}
//
//	/**
//	 * Read.
//	 *
//	 * @param clazz the clazz
//	 * @param inputMessage the input message
//	 * @return the object
//	 * @throws HttpMessageNotReadableException the http message not readable exception
//	 */
//	@Override
//	public Object read(Class clazz, HttpInputMessage inputMessage) throws HttpMessageNotReadableException {
//		throw new UnsupportedOperationException();
//	}
//
//	/**
//	 * Write.
//	 *
//	 * @param o the o
//	 * @param contentType the content type
//	 * @param outputMessage the output message
//	 * @throws HttpMessageNotWritableException the http message not writable exception
//	 */
//	@Override
//	public void write(Object o, MediaType contentType, HttpOutputMessage outputMessage) throws HttpMessageNotWritableException{
//		if (o != null)
//		{
//			String body = JSONUtils.getObjectMapper().convertValue(o, UrlEncodedWriter.class).toString();
//
//			try
//			{
//				outputMessage.getBody().write(body.getBytes(Encoding));
//			}
//			catch (IOException e)
//			{
//				// if UTF-8 is not supporter then I give up
//			}
//		}
//	}
//
//	/**
//	 * The class UrlEncodedWriter.
//	 *
//	 * @author  Manish Srivastava
//	 * @since   22 Mar, 2021
//	 */
//	private static class UrlEncodedWriter
//	{
//
//		/** The out. */
//		private final StringBuilder out = new StringBuilder();
//
//		/**
//		 * Write.
//		 *
//		 * @param name the name
//		 * @param property the property
//		 * @throws UnsupportedEncodingException the unsupported encoding exception
//		 */
//		@JsonAnySetter
//		public void write(String name, Object property) throws UnsupportedEncodingException
//		{
//			if (out.length() > 0)
//			{
//				out.append("&");
//			}
//
//			out
//			.append(URLEncoder.encode(name, Encoding))
//			.append("=");
//
//			if (property != null){
//				if(property instanceof Map || property instanceof Collection) {
//					out.append(URLEncoder.encode(JsonUtils.toJsonString(property), Encoding));
//				}else {
//			   	    out.append(URLEncoder.encode(property.toString(), Encoding));
//				}
//			}
//		}
//
//		/**
//		 * To string.
//		 *
//		 * @return the string
//		 */
//		@Override
//		public String toString()
//		{
//			return out.toString();
//		}
//	}
//}
