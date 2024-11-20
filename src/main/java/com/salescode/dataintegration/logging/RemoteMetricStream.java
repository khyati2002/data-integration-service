//package com.salescode.dataintegration.logging;
//
////import com.applicate.analytics.exception.SystemRuntimeException;
////import com.applicate.services.channelkart.integration.kafka.constants.IKafkaConstants;
////import com.applicate.services.channelkart.integration.kafka.publisher.KafkaIntegrationPublisher;
////import com.applicate.services.channelkart.models.Profile;
////import com.applicate.services.channelkart.profiles.ProfileRegistry;
////import com.applicate.services.channelkart.security.SecurityContextUtils;
////import com.applicate.services.channelkart.utils.JSONUtils;
////import com.applicate.services.channelkart.utils.MutableString;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import com.salescode.channelkart.models.diff.EventMetrics;
//import com.salescode.channelkart.models.diff.RequestMetrics;
////import org.apache.commons.codec.binary.Base64;
////import org.apache.commons.collections4.map.HashedMap;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.env.Environment;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
////import javax.annotation.PostConstruct;
//import java.nio.charset.StandardCharsets;
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//import java.util.concurrent.ArrayBlockingQueue;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
///**
// * Type : elasticsearch,name:event-store
// * {
// *   serverUrl:server_url,
// *   username:username
// *
// *   password:password
// * }
// */
//@Service
//public class RemoteMetricStream {
//    private final  Logger logger = LoggerFactory.getLogger(this.getClass());
//
//    private ArrayBlockingQueue<RequestMetrics> queue = new ArrayBlockingQueue<>(5000);
//    private ArrayBlockingQueue<EventMetrics> eventQueue = new ArrayBlockingQueue<>(5000);
//
//    private ArrayBlockingQueue<EventMetrics> auditLog = new ArrayBlockingQueue<>(5000);
//
//    private static final String AUDIT_EVENT_STORE="audit-event-store";
//
//    private ExecutorService service =null;
//    private ExecutorService eventBusService =null;
//
//    private ExecutorService auditLogService =null;
//
//    private String 	eventIndexName="events";
//
//
//    private int numWorkers=2;
//
//    @Autowired
//    Environment environment;
//
//    @Value("${logging.cloud.elasticsearch.audit.url:}")
//    private String 	serverUrl;
//
//    @Value("${logging.cloud.elasticsearch.index}")
//    private String 	indexName;
//
//    private static final String HTTP = "http://";
//
//    private static final String DOC_REF="/_doc/";
//
//    private boolean isESLoggingEnabled = true;
//    private boolean isEventLoggingEnabled = true;
//
//    private boolean isAuditEventLoggingEnabled = true;
//
//
//
//    private String esUserName=null;
//    private String esPassword=null;
//
//
//    KafkaIntegrationPublisher publisher;
//
//    KafkaIntegrationPublisher metrixPublisher;
//
//    @PostConstruct
//    public void init(){
//        isESLoggingEnabled = !Boolean.valueOf(environment.getProperty("logging.cloud.elasticsearch.disabled","false"));
//        isEventLoggingEnabled = !Boolean.valueOf(environment.getProperty("logging.cloud.elasticsearch.events.disabled","false"));
//        isAuditEventLoggingEnabled = !Boolean.valueOf(environment.getProperty("logging.cloud.elasticsearch.audit.events.disabled","true"));
//        if(isESLoggingEnabled || isEventLoggingEnabled ) {
//            publisher =  KafkaIntegrationPublisher.getInstance();
//            String kafkaUrl = environment.getProperty("logging.cloud.elasticsearch.kafkaUrl");
//            if(kafkaUrl==null){
//                metrixPublisher = publisher;
//                logger.info("No custom api logging kafka url found for api logging");
//            }else{
//                logger.info("Found custom custom api logging kafka url for api logging {}",kafkaUrl);
//                metrixPublisher =  KafkaIntegrationPublisher.getNewInstance(kafkaUrl);
//            }
//        }
//
//        service = Executors.newFixedThreadPool(numWorkers);
//        eventBusService = Executors.newFixedThreadPool(numWorkers);
//        auditLogService = Executors.newFixedThreadPool(numWorkers);
//        startRequestMetricStream();
//        startEventBusMetricStream();
//        startAuditLogMetricStream();
//
//        esUserName=environment.getProperty("logging.cloud.elasticsearch.audit.username");
//        esPassword=environment.getProperty("logging.cloud.elasticsearch.audit.password");
//
//    }
//
//    private void startRequestMetricStream(){
//        for(int i=0;i<numWorkers;i++){
//            service.submit(()->{
//                while(true){
//                    try {
//                        startRequestStreamWork();
//                    }catch (Exception e){
//                        String errorMessage = "metric stream worker failed".concat(getLob()).concat(" with error: ");
//                        logger.error(errorMessage, e);
//                    }
//                }
//            });
//        }
//    }
//
//    private void startEventBusMetricStream(){
//        for(int i=0;i<numWorkers;i++){
//            eventBusService.submit(()->{
//                while(true){
//                    startWork();
//                }
//            });
//        }
//    }
//
//    private void startAuditLogMetricStream(){
//        for(int i=0;i<numWorkers;i++){
//            auditLogService.submit(()->{
//                while(true){
//                    startAuditLogStreamWork();
//                }
//            });
//        }
//    }
//
//    private void startAuditLogStreamWork(){
//        try {
//            EventMetrics rms = auditLog.take();
//            if(isAuditEventLoggingEnabled) {
//                submitAuditLog(rms);
//            }else{
//                JsonNode node=        JSONUtils.toJsonNode(rms);
//                String postBody = node.toString();
//                logger.debug(postBody);
//            }
//        }catch (InterruptedException e){
//            String errorMessage = "Remote metric streaming failed".concat(getLob());
//            logger.error(errorMessage, e);
//            Thread.currentThread().interrupt();
//        }catch (Exception e){
//            String errorMessage = "remote metric streaming failed".concat(getLob());
//            logger.error(errorMessage, e);
//        }
//    }
//
//    private void startRequestStreamWork(){
//        try {
//            RequestMetrics rm = queue.take();
//            String postBody = JSONUtils.getObjectMapper().writeValueAsString(rm);
//            if(isESLoggingEnabled) {
//                publishMetricsToKafka(rm.getStreamId(),postBody);
//
//            }else{
//                logger.debug(postBody);
//            }
//        }catch (InterruptedException e){
//            String errorMessage = "request stream failed".concat(getLob()).concat(" with error ");
//            logger.error(errorMessage, e);
//            Thread.currentThread().interrupt();
//        }catch (Exception e){
//            String errorMessage = "Request stream failed".concat(getLob()).concat(" with error ");
//            logger.error(errorMessage, e);
//        }
//    }
//
//    private void startWork(){
//        try {
//            EventMetrics rms = eventQueue.take();
//            ObjectNode node= (ObjectNode) JSONUtils.toJsonNode(rms);
//            node.put("isEvent",true);
//            String postBody = node.toString();
//            if(isEventLoggingEnabled) {
//                publishToKafka(rms.getId()!=null?rms.getId(): UUID
//                        .randomUUID().toString(),postBody);
//            }else{
//                logger.debug(postBody);
//            }
//        }catch (InterruptedException e){
//            String errorMessage = "Remote event metric streaming failed".concat(getLob());
//            logger.error(errorMessage, e);
//            Thread.currentThread().interrupt();
//        }catch (Exception e){
//            String errorMessage = "remote event metric streaming failed".concat(getLob());
//            logger.error(errorMessage, e);
//        }
//    }
//
//    private String getIndexName(EventMetrics rms){
//        String evIndex= "audit".equalsIgnoreCase(rms.getType())?"audit":eventIndexName;
//        return rms.getLob()!=null && !rms.getLob().isBlank()?rms.getLob().toLowerCase()+"-"+evIndex:evIndex;
//    }
//
//    private Profile getProfile(String lob,String type){
//        List<Profile> profiles = ProfileRegistry.INSTANCE.get(lob, "elasticsearch", type);
//        if(!profiles.isEmpty()){
//            return profiles.get(0);
//        }
//        return null;
//    }
//
//    private HttpHeaders prepareHeadersAndServerURL(String lob,MutableString serverEventURL,String type){
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        Profile profile = getProfile(lob,type);
//        if(profile!=null){
//            String serverURL = profile.getAttributes().get("serverUrl").asText().trim();
//            if(!(serverURL.startsWith(HTTP)||serverURL.startsWith("https://"))){
//                serverURL= HTTP + serverURL;
//            }
//            if(!serverURL.endsWith("/")){
//                serverURL=serverURL+"/";
//            }
//            serverEventURL.setValue(serverURL);
//            setAuth(headers,profile);
//        }else {
//            setAuth(headers,null);
//        }
//        return headers;
//    }
//    private void submitAuditLog(EventMetrics rms) {
//        JsonNode node=        JSONUtils.toJsonNode(rms);
//        String postBody = node.toString();
//        MutableString serverEventURL = new MutableString(serverUrl);
//        String lobIndexName = getIndexName(rms);
//        HttpHeaders headers = prepareHeadersAndServerURL(rms.getLob(), serverEventURL,AUDIT_EVENT_STORE);
//        RestTemplate restTemplate = new RestTemplate();
//        HttpEntity<String> request =
//                new HttpEntity<>(postBody, headers);
//        ResponseEntity<String> responseEntityStr = (rms.getId() == null) ? restTemplate.
//                postForEntity(serverEventURL + lobIndexName + DOC_REF, request, String.class) :
//                restTemplate.exchange(serverEventURL + lobIndexName + DOC_REF + rms.getId(),
//                        HttpMethod.PUT, request, String.class);
//        try{
//            if (!responseEntityStr.getStatusCode().is2xxSuccessful()) {
//                logger.info(responseEntityStr.getBody());
//                rms.setRetryCount(rms.getRetryCount()+1);
//                if(rms.getRetryCount()<=3){
//                    eventQueue.put(rms);
//                }
//            }
//        }catch (InterruptedException e){
//            String errorMessage = "request stream failed".concat(getLob()).concat("  with error ");
//            logger.error(errorMessage, e);
//            Thread.currentThread().interrupt();
//        }
//    }
//
//    private void publishToKafka(String id,String rms){
//        try {
//
//            publisher.publish(IKafkaConstants.getEventsStreamName(), id,rms,5);
//        }catch (Exception e){
//            String errorMessage = "Failed to push to kafka publisher".concat(getLob());
//            logger.error(errorMessage);
//        }
//    }
//
//    private void publishMetricsToKafka(String id,String rms){
//        try {
//            metrixPublisher.publish(IKafkaConstants.getMetricStreamName(), id,rms,5);
//        }catch (Exception e){
//            String errorMessage = "Failed to push to kafka metrics publisher".concat(getLob());
//            logger.error(errorMessage);
//        }
//    }
//
//
//
//
//    private  void setAuth(HttpHeaders headers,Profile profile) {
//        if(environment==null)
//            return;
//        String username =(profile==null|| !profile.getAttributes().has("username"))?esUserName:
//                profile.getAttributes().get("username").asText();
//        String password =(profile==null ||  !profile.getAttributes().has("password"))?esPassword:
//                profile.getAttributes().get("password").asText();
//        if(username!=null && !username.isBlank()) {
//            String auth = username + ":" + password;
//            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.US_ASCII));
//            String authHeader = "Basic " + new String(encodedAuth);
//            headers.set("Authorization", authHeader);
//        }
//    }
//
//    public void sendAsync(RequestMetrics rm){
//        boolean offered = queue.offer(rm);
//        if(!offered){
//            String errorMessage = "Failed to push message due to the queue overflow".concat(getLob());
//            logger.error(errorMessage);
//        }
//    }
//
//    public void sendAsyncEvent(String type, Map<Object,Object> attributes){
//        var metrics = new EventMetrics();
//        metrics.setType(type);
//        metrics.setCreatedBy(SecurityContextUtils.getPrincipal());
//        metrics.setLob(SecurityContextUtils.getLob());
//        metrics.setEventTime(new Date());
//        metrics.setPayload(JSONUtils.toJsonNode(attributes));
//        sendAsyncEvent(metrics);
//    }
//    public void sendAsyncEvent(EventMetrics rm){
//        boolean offered = eventQueue.offer(rm);
//        if(!offered){
//            String errorMessage = "Failed to push message due to the event queue overflow".concat(getLob());
//            logger.error(errorMessage);
//        }
//    }
//
//    public void sendAuditLog(EventMetrics rm){
//        boolean offered = auditLog.offer(rm);
//        if(!offered){
//            String errorMessage = "Failed to push audit log message due to the event queue overflow".concat(getLob());
//            logger.error(errorMessage);
//        }
//    }
//
//    public String readIndexData(String index,int from,int size,String filter, String fromDate, String toDate){
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        RestTemplate restTemplate = new RestTemplate();
//        String serverEventURL = serverUrl;
//        List<Profile> profiles = ProfileRegistry.INSTANCE.get(SecurityContextUtils.getLob(), "elasticsearch", AUDIT_EVENT_STORE);
//        if(!profiles.isEmpty()){
//            Profile profile = profiles.get(0);
//            serverEventURL = profile.getAttributes().get("serverUrl").asText().trim();
//            if(!(serverEventURL.startsWith(HTTP)||serverEventURL.startsWith("https://"))){
//                serverEventURL= HTTP + serverEventURL;
//            }
//            if(!serverEventURL.endsWith("/")){
//                serverEventURL=serverEventURL.concat("/");
//            }
//            setAuth(headers,profile);
//        }else {
//            setAuth(headers,null);
//        }
//        Map<String, String> dates = new HashedMap<>();
//        dates.put("gte",fromDate);
//        dates.put("lte",toDate);
//        Map<String, Object> eventTime = new HashedMap<>();
//        eventTime.put("eventTime",dates);
//        Map<String, Object> range = new HashedMap<>();
//        range.put("range",eventTime);
//        Map<String, Object> query = new HashedMap<>();
//        query.put("query",range);
//        JsonNode body=JSONUtils.toJsonNode(query);
//        HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);
//
//        String url = serverEventURL + index+"/_search?size="+size+"&from="+from;
//        if(filter!=null){
//            url+="&q="+filter;
//        }
//
//        ResponseEntity<String> response = restTemplate.exchange(url
//                , HttpMethod.POST, request, String.class);
//
//        if (response.getStatusCode().is2xxSuccessful()) {
//            return response.getBody();
//        }else{
//            throw new SystemRuntimeException(response.getStatusCode()+response.getBody());
//        }
//    }
//
//    private String getLob(){
//        return SecurityContextUtils.getLob()==null?"":" lob : ".concat(SecurityContextUtils.getLob());
//    }
//}
//
