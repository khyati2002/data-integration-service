package com.applicate.services.channelkart.utils;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

/**
 * @author : Jinu
 * Date    : 7/14/2020
 **/
@Component
public class HttpClient {

   private final RestTemplate restTemplate;

   public HttpClient() {
      CloseableHttpClient httpClient = HttpClients.custom()
              .setSSLHostnameVerifier(new NoopHostnameVerifier())
              .build();
      HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
      requestFactory.setHttpClient(httpClient);
      restTemplate = new RestTemplate(requestFactory);
      requestFactory.setConnectTimeout(3000);
      requestFactory.setReadTimeout(3000);
      restTemplate.getMessageConverters().add(new ObjectToUrlEncodedConverter());
   }

   public <T> ResponseEntity<T> getForEntity(String url, Class<T> clazz) {
      return restTemplate.getForEntity(URI.create(url), clazz);
   }

   public <T> ResponseEntity<T> postForEntity(String url, Object request, Class<T> responseType) {
      return restTemplate.postForEntity(url, request, responseType);
   }
   public <T> ResponseEntity<T> postObject(String url, Object request, Class<T> responseType,HttpHeaders headers) {
	   HttpEntity<Object> entity= new HttpEntity<>(request,headers);
	   return restTemplate.postForEntity(url, entity, responseType);
   }
   public <T> ResponseEntity<T> postObject(String url, Object request, Class<T> responseType,HttpHeaders headers,Map<String,Object> uriVariables) {
	   HttpEntity<Object> entity= new HttpEntity<>(request,headers);
	   return restTemplate.postForEntity(url, entity, responseType, uriVariables);
   }
  
}
