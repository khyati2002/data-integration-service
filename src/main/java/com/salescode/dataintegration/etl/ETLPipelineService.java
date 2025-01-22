package com.salescode.dataintegration.etl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.applicate.services.channelkart.datastreams.PipelineDispatcher;
import com.applicate.services.channelkart.dto.StreamingRawData;
import com.applicate.services.channelkart.dto.StreamingRawData.Response;
import com.applicate.services.channelkart.integration.kafka.publisher.KafkaIntegrationPublisher;
import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.models.IntegrationHistory;
import com.applicate.services.channelkart.pojo.MdmOperationResponse;
import com.applicate.services.channelkart.pojo.TaskAttributeRequestTemplate.TransformerInfo;
import com.applicate.services.channelkart.response.OperationStatus;
import com.applicate.services.channelkart.services.IntegrationHistoryService;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.JSONUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ETLPipelineService {

    public static final String ERROR_STR = "Exception";
    private static final String INTEGRATION_APP_ID = "integration";
    private static final String INTEGRATION_RAW_KEYS = "dataKey";
    private static final String INTERRUPTED_MESSAGE = "Thread Interrupted!";
    private final PipelineDispatcher pipelineDispatcher;
    private final boolean isMessageLevelHashing = true;
    private final IntegrationHistoryService ihs;
    private final Environment env;
    ObjectMapper objectMapper = JSONUtils.getObjectMapper();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public static final List<String> DISABLED_INT_HISTORY_LOBS = List.of(Optional.ofNullable(System.getenv("disabledIntHistory")).orElseGet(()->"unnati").split(","));

    private int retryCount;
    private KafkaIntegrationPublisher publisher = null;

    public ETLPipelineService(PipelineDispatcher dispatcher, IntegrationHistoryService ihs, Environment env) {
        this.pipelineDispatcher = dispatcher;
        this.ihs = ihs;
        this.env = env;
        publisher = KafkaIntegrationPublisher.getInstance();
    }

    @SneakyThrows
    public List<CommonDataModel> executeBatch(List<String> messages) {
        List<CompletableFuture<List<CommonDataModel>>> futures = messages.stream()
            .map(message -> CompletableFuture.supplyAsync(() -> {
                try {
                    return execute(message);
                } catch (Exception e) {
                    log.error("Failed to process message", e);
                    return new ArrayList<CommonDataModel>();
                }
            }, executorService))
            .collect(Collectors.toList());
        return futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    @SneakyThrows
    public List<CommonDataModel> execute(String message) {
        log.info("Executing etl pipeline");
        StreamingRawData streamingRawData = objectMapper.readValue(message, StreamingRawData.class);
        ArrayNode features = streamingRawData.getFeatures();
        if (features.isEmpty()) {
            throw new IllegalArgumentException("Features cannot be empty");
        }
        List<Future<MdmOperationResponse>> response = process(streamingRawData);
        streamingRawData.setResponse(response);
        Map<Class, TransformerInfo> transformerMap = new HashMap<>();
        streamingRawData.getTransformerInfo().stream().forEach(t -> {
            Class ec = EntityUtils.get().getEntityClass(t.getEntityName());
            transformerMap.put(ec, t);
        });
        List<StreamingRawData.Response> responses = new ArrayList<>();
        List<IntegrationHistory> integrationDataList = new ArrayList<>();
        final boolean allSuccess = processResponse(streamingRawData, integrationDataList, responses, transformerMap);
        processFinalStatus(streamingRawData, integrationDataList, responses, allSuccess);
        return new ArrayList<>();
    }

    public List<Future<MdmOperationResponse>> process(StreamingRawData data) {
        List<Future<MdmOperationResponse>> outputFutures = new ArrayList<>();
        try {
            ArrayNode features = data.getFeatures();
            List<TransformerInfo> dataList = data.getTransformerInfo();
            features.forEach(j -> outputFutures.add(submitRequest(j, dataList)));
        } catch (Exception e) {
            log.error("stacktrace", e);
        }
        return outputFutures;
    }

    public Future<MdmOperationResponse> submitRequest(JsonNode json, List<TransformerInfo> transformers) {
        MdmOperationResponse operation = new MdmOperationResponse();
        operation.setInput(objectMapper.convertValue(json, new TypeReference<Map<String, Object>>() {
        }));
        return pipelineDispatcher.dispatch(operation, transformers);
    }

    private boolean processResponse(StreamingRawData sdr, List<IntegrationHistory> integrationDataList, List<Response> responses, Map<Class, TransformerInfo> transformerMap) {
        boolean allSuceess = true;
        for (Future<MdmOperationResponse> mdmOperationResponseFuture : sdr.getResponse()) {
            log.info("processing request -> {} for lob {}", sdr.getRequestId(), sdr.getLob());
            try {
                MdmOperationResponse resp = mdmOperationResponseFuture.get();
                if (resp != null) {
                    if (resp.getStatus().equals(OperationStatus.Success)) {
                        boolean cStatus = processSuccessStatus(sdr, integrationDataList, resp, transformerMap);
                        if (!cStatus) {
                            responses.add(processFailureStatus(sdr, integrationDataList, resp, transformerMap));
                        }
                        allSuceess = allSuceess && cStatus;
                    } else {
                        responses.add(processFailureStatus(sdr, integrationDataList, resp, transformerMap));
                        allSuceess = false;
                    }
                } else {
                    log.info("response not found {}", sdr.getRequestId());
                    allSuceess = false;
                }
            } catch (Exception e) {
                responses.add(processErrorStatus(sdr, integrationDataList, transformerMap, e));
            }
        }
        return allSuceess;

    }

    private boolean processSuccessStatus(StreamingRawData sdr, List<IntegrationHistory> integrationDataList, MdmOperationResponse resp, Map<Class, TransformerInfo> transformerMap) {
        try {
            log.info("Request Successful -> {} for lob {}", sdr.getRequestId(), sdr.getLob());
            Map<Class, Set<CommonDataModel>> response = resp.getResponse() != null ? resp.getResponse().get() : null;
            if (response != null && isMessageLevelHashing) {
                response.forEach((k, v) -> v.stream().forEach(cdm -> {
                    if (cdm != null) {
                        integrationDataList.add(getIntegrationHistory(sdr, transformerMap.get(k), cdm, OperationStatus.Success));
                    } else {
                        log.error("Invalid object status detected while proceesing record {} for lob {}", JSONUtils.toJsonString(sdr), sdr.getLob());
                    }
                }));
            } else {
                log.info("sresponse not found {} for lob {}", sdr.getRequestId(), sdr.getLob());
                for (TransformerInfo tinfo : transformerMap.values()) {
                    integrationDataList.add(getIntegrationHistory(sdr, tinfo, null, OperationStatus.INTERNAL_SERVER_FAILURE));
                }
            }
        } catch (InterruptedException e) { // Compliant; the interrupted state is restored
            log.error(INTERRUPTED_MESSAGE, e);
            Thread.currentThread().interrupt();
            resp.setStatus(OperationStatus.INTERNAL_SERVER_FAILURE);
            resp.setError(e.getMessage());
            return false;
        } catch (Exception e) {
            log.error(ERROR_STR, e);
            resp.setStatus(OperationStatus.INTERNAL_SERVER_FAILURE);
            resp.setError(e.getMessage());
            return false;
        }
        return true;
    }

    private Response processFailureStatus(StreamingRawData sdr, List<IntegrationHistory> integrationDataList, MdmOperationResponse resp, Map<Class, TransformerInfo> transformerMap) {
        log.info("Request Failed ->{} for lob {}", sdr.getRequestId(), sdr.getLob());
        for (TransformerInfo tinfo : transformerMap.values()) {
            integrationDataList.add(getIntegrationHistory(sdr, tinfo, null, resp.getStatus()));
        }
        Response response = new Response();
        response.setStatue(resp.getStatus().name());
        response.setMessage(resp.getError());
        return response;
    }

    private Response processErrorStatus(StreamingRawData sdr, List<IntegrationHistory> integrationDataList, Map<Class, TransformerInfo> transformerMap, Exception e) {
        log.info("Error, Request Failed ->{} for lob {}", sdr.getRequestId(), sdr.getLob());
        for (TransformerInfo tinfo : transformerMap.values()) {
            integrationDataList.add(getIntegrationHistory(sdr, tinfo, null, OperationStatus.Failure));
        }
        Response response = new Response();
        response.setStatue(OperationStatus.Failure.name());
        response.setMessage(e.getMessage());
        return response;
    }

    private IntegrationHistory getIntegrationHistory(StreamingRawData sdr, TransformerInfo trinfo, CommonDataModel cdm, OperationStatus status) {
        IntegrationHistory ind = new IntegrationHistory();
        ind.setLob(sdr.getLoginId());
        if (trinfo.getMessageLevelKey() != null) ind.setId(trinfo.getMessageLevelKey());
        ind.setRequestId(sdr.getRequestId());
        ind.setGroupId(sdr.getGroupId());
        if (cdm != null) {
            if (cdm.getId() == null) {
                String message = JSONUtils.stringify(cdm);
                log.error("cdm object with out id {}", message);
                throw new IllegalStateException("cdm object without id " + message);
            }
            ind.setEntityId(cdm.getId());
            ind.setAction("update");
        } else {
            ind.setAction("skip");
        }
//        ind.setOffset(Double.parseDouble(sdr.getOffset()));
        ind.setEntityName(trinfo.getEntityName());
        ind.setMessageKey(trinfo.getMessageLevelKey());
        ind.setMessageHash(trinfo.getMessageLevelHash());
        if (sdr.getAppId() == null) {
            ind.setAppId(INTEGRATION_APP_ID);
        } else {
            ind.setAppId(sdr.getAppId());
        }

//        setRawKeys(sdr,ind);
//        ind.setOffset(Double.parseDouble(sdr.getOffset()));
        ind.setTimestamp(System.currentTimeMillis());
        ind.setStatus(status.name());
        try {
            MdmOperationResponse response = sdr.getResponse().get(0).get();
            if (response.getError().equalsIgnoreCase("")) {
                ind.setDescription("Success");
            } else {
                ind.setDescription(response.getError());
            }
        } catch (InterruptedException e) { // Compliant; the interrupted state is restored
            log.error(INTERRUPTED_MESSAGE, e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error(ERROR_STR, e);
        }
        return ind;
    }

    private void processFinalStatus(StreamingRawData sdr, List<IntegrationHistory> integrationDataList, List<StreamingRawData.Response> responses, boolean allSuceess) {
        log.info("processing final status as {} for request {} and lob {}", allSuceess, sdr.getRequestId(), sdr.getLob());
        if (allSuceess) {
            sdr.setStatus(OperationStatus.Success.name());
            updateStatus(sdr, integrationDataList);
        } else {
            sdr.setStatus(OperationStatus.Failure.name());
            sdr.setResponse(null);
            sdr.setResponses(responses);
            if (!publisher.publishRec(getFailureTopic(sdr), sdr)) {
                integrationDataList.forEach(s -> s.setDescription(String.valueOf(s.getDescription()).concat("~Failed to publish")));
            }
            if (sdr.isPreserveOnFailure()) {
                updateStatus(sdr, integrationDataList);
            }
        }
    }


    private void updateStatus(StreamingRawData sdr, List<IntegrationHistory> integrationDataList) {
        if(!DISABLED_INT_HISTORY_LOBS.contains(sdr.getLob())) {
        if (isIntegration(sdr) && !integrationDataList.isEmpty()) {
            try {
                ihs.update(integrationDataList);
            } catch (Exception e) {
                log.info("Exception while saving record for request ->{} ", sdr.getRequestId(), e);
                addFailureToUpdate(sdr, integrationDataList, e.getMessage());
            }
        }
        } else {
            log.info("Skipping update for integration history {}", sdr.getRequestId());
        }

    }

    private void addFailureToUpdate(StreamingRawData sdr, List<IntegrationHistory> integrationDataList, String errorDescription) {
        try {
            ihs.createFailureRecord(integrationDataList, errorDescription);
        } catch (Exception e) {
            log.info("Exception while saving urecord for request ->{}", sdr.getRequestId(), e);
        }
    }

    private boolean isIntegration(StreamingRawData sdr) {
        return sdr.getAppId() == null || INTEGRATION_APP_ID.equalsIgnoreCase(sdr.getAppId());
    }


    private String getFailureTopic(StreamingRawData sdr) {
        boolean isIntegration = isIntegration(sdr);
        if (isIntegration) {
            String responseStream = ""; // stores the resultant stream to return
            for (StreamingRawData.Response res : sdr.getResponses()) {
                if (res.getStatue().contains("INTERNAL_SERVER_FAILURE") || res.getMessage().contains("Internal Server Error")) {
                    // In case of Internal Server Error, check for retry
                    if (sdr.getRetryCount() < retryCount) {
                        //increment count
                        sdr.incrementRetryCount();
                        responseStream = sdr.getLob() + "-integration-streams";
                    } else {
                        // After retry return to failure stream
                        responseStream = sdr.getLob() + "-int-failure-streams";
                    }
                } else {
                    // If not an internal error, return to failure stream
                    responseStream = sdr.getLob() + "-int-failure-streams";
                }
            }
            return responseStream;
        }
        return sdr.getLob() + "-" + sdr.getAppId() + "-failure-streams";
    }
}
