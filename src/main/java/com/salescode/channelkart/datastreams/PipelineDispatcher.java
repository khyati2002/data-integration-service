package com.salescode.channelkart.datastreams;

import com.salescode.channelkart.cache.DistributedCache;
import com.salescode.channelkart.enrichments.EnrichmentResult;
import com.salescode.channelkart.exceptions.CustomRuntimeException;
import com.salescode.channelkart.exceptions.TransformationException;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.pojo.MdmOperationResponse;
import com.salescode.channelkart.pojo.TaskAttributeRequestTemplate;
import com.salescode.channelkart.response.OperationResponse;
import com.salescode.channelkart.response.OperationStatus;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.services.CommonDataModelService;
import com.salescode.channelkart.services.PreProcessPipelineService;
import com.salescode.channelkart.services.ServiceLocator;
import com.salescode.channelkart.services.SpringContext;
import com.salescode.channelkart.services.enums.OperationType;
import com.salescode.channelkart.transformers.CdmTransformerService;
import com.salescode.channelkart.utils.EntityUtils;
import com.salescode.channelkart.utils.StringUtils;
import com.salescode.channelkart.utils.TimerUtils;
import com.salescode.channelkart.validations.EntityValidationResult;
import com.salescode.channelkart.validations.RuleResult;
import com.salescode.channelkart.validations.Status;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class PipelineDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(PipelineDispatcher.class);

    private static int activeThreadCount = Integer.parseInt(Optional.ofNullable(System.getenv("activeThreads")).orElse(System.getProperty("activeThreads", "5")));

    public static final String INTEGRATION_EVENT_KEY_STORE="IntegrationEvents";


    private static ArrayBlockingQueue<Runnable> abq = new ArrayBlockingQueue<Runnable>(
            Integer.parseInt(System.getProperty("activeQueue", "100"))) {

        private static final long serialVersionUID = 5750467066110283923L;

        @Override
        public boolean offer(Runnable e) {
            try {
                this.put(e);
            } catch (InterruptedException e1) {
                logger.error(e1.getLocalizedMessage(),e1);
                e1.printStackTrace();
            }
            return true;
        }
    };


    @Autowired
    private DistributedCache distributedCache;


    private final ThreadPoolExecutor cte = new ThreadPoolExecutor(activeThreadCount, activeThreadCount, 120,
            TimeUnit.SECONDS, abq);

    private CdmTransformerService cdmTransformerService;

    private PreProcessPipelineService preProcessPipelineService;

    private static final String ENRICHMENT_ERROR= "Enrichment Failed : {}";
    private static final String ENTITY_VALIDATION_ERROR= "EntityValidation Failed : {}";
    private static final String VALIDATION_ERROR= "Validation Failed : {}";
    private static final String SAVE_ERROR= "Error while saving record. Reason: {}";
    private static final String SEPARATOR= ",";

    public PipelineDispatcher(CdmTransformerService cdmTransformerService,
                              PreProcessPipelineService preProcessPipelineService) {
        this.cdmTransformerService = cdmTransformerService;
        this.preProcessPipelineService = preProcessPipelineService;
    }

    public Future<MdmOperationResponse> dispatch(MdmOperationResponse input,
                                                 Collection<TaskAttributeRequestTemplate.TransformerInfo> infos) {
        String lob = SecurityContextUtils.getLob();
//        final UserContext uc = new UserContext(SecurityContextUtils.getPrincipal(), lob);
        return cte.submit(new Callable<MdmOperationResponse>() {
            @Override
            public MdmOperationResponse call() throws Exception {
                try {
//                    return SecurityContextUtils.switchWithUser(uc,()->execute(input, infos));
                    return execute(input, infos);
                } catch (Exception e) {
                    throw new CustomRuntimeException(e);
                }
            }
        });
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MdmOperationResponse execute(MdmOperationResponse operation,
                                        Collection<TaskAttributeRequestTemplate.TransformerInfo> infos) {

        String lob = SecurityContextUtils.getLob();
        Map<String, Object> input= operation.getInput();
        Map<String,Object> shallow= new LinkedHashMap();
        shallow.putAll(input);
        operation.setInput(shallow);
        Map<Class, Set<CommonDataModel>> dataset = getCollector(infos);
        List<String> errorList= new ArrayList<>();
        List<String> successMessageList= new ArrayList<>();
        TimerUtils.withTime("Time taken to process record ", () -> {
            AtomicBoolean anyMessage=new AtomicBoolean(false);
            for (TaskAttributeRequestTemplate.TransformerInfo transformerinfo : infos) {
                String entityName = transformerinfo.getEntityName();
                Optional<String> preprocessValidationExcludeGroup=Optional.ofNullable(transformerinfo.getPreprocessValidationExcludeGroup());
                Class entityClass = EntityUtils.get().getEntityClass(entityName);
                CommonDataModelService<CommonDataModel> cdmService = SpringContext
                        .getBean(ServiceLocator.lookup(entityClass).getClass());
                try {
                    List<CommonDataModel> cdms = TimerUtils.withTime("Time taken to transform request ",
                            () -> StringUtils.isNotBlank(transformerinfo.getTransformerId())
                                    ? cdmTransformerService.execute(input, entityName, lob, transformerinfo.getTransformerId())
                                    : cdmTransformerService.toCDM(input, entityName));


                    cdms.forEach(tempcdm -> {
                        boolean isDuplicated=false;
                        String id = cdmService.getKey(tempcdm);
                        if(id==null) {
                            id= UUID.randomUUID().toString();
                        }else{
                            String messageHash = transformerinfo.getMessageLevelHash();
                            String messageLevelKey=transformerinfo.getTransformerId()+":"+entityName+":"+id;
                            transformerinfo.setMessageLevelKey(messageLevelKey);

                            if(transformerinfo.getMessageHashSupported() && messageHash!=null && cdms.size()==1){
                                String[] value= (String[]) distributedCache.get(lob, INTEGRATION_EVENT_KEY_STORE,messageLevelKey,true);
                                transformerinfo.setMessageLevelKey(messageLevelKey);
                                transformerinfo.setCachedArtifact(value);
                                if(value!=null && messageHash.equals(value[0])){
                                    logger.info("Duplicate record with key {}",id);
                                    isDuplicated=true;
                                }
                            }
                        }
                        if(!isDuplicated) {
                            anyMessage.set(true);
                            synchronized (id.intern()) {
                                CommonDataModel cdm = TimerUtils.withTime("Time taken to refresh record ", tempcdm,
                                        t -> cdmService.refresh(t));
                                if(transformerinfo.isSkipPreprocessing() || (transformerinfo.getOperationType()!=null && OperationType.delete.equals(transformerinfo.getOperationType()))){
                                    logger.info("Skip Preprocessing");
                                    dataset.get(entityClass).add(cdm);
                                } else if(transformerinfo.isSkipPersist()){
                                    anyMessage.set(false);
                                    dataset.get(entityClass).add(cdm);
                                    operation.setResponse(CompletableFuture.supplyAsync(()->dataset));
                                    logger.info("Skip Persist lob {}, entityName {}, transformerId {}", lob,entityName, transformerinfo.getTransformerId());
                                } else {
                                    TimerUtils.withTime("Time taken to validate record ", () -> {
                                        OperationResponse response = preProcessPipelineService
                                                .process(cdm, preprocessValidationExcludeGroup,true);
                                        if (response.getStatus().equals(OperationStatus.Failure)) {
                                            if (response.getEnrichment() != null
                                                    && response.getEnrichment().getStatus()
                                                    .equals(com.salescode.channelkart.enrichments.Status.ERROR)) {
                                                List<EnrichmentResult> enrichmentResult = response.getEnrichment()
                                                        .getEnrichmentResults();
                                                enrichmentResult.forEach(en -> logger.info("{} {}{}",en,en.getStatus(),en.getMessage()));
                                                String error = org.apache.commons.lang.StringUtils
                                                        .join(enrichmentResult.stream().map(EnrichmentResult::getMessage).collect(Collectors.toList()), SEPARATOR);
                                                errorList.add(StringUtils.format(ENRICHMENT_ERROR, error));
                                            }
                                            if (response.getEntityValidation() != null && response.getEntityValidation()
                                                    .getStatus()
                                                    .equals(Status.ERROR)) {
                                                EntityValidationResult enrichmentResult = response.getEntityValidation();
                                                List<RuleResult> ruleresult =
                                                        response.getValidation() != null ? response.getValidation()
                                                                .getViolations()
                                                                : new ArrayList<>();
                                                ruleresult.forEach(en ->
                                                        logger.info("{} status:{}, reason:{}",
                                                                en.getRule().getImplementation(),en.getStatus(),en
                                                                        .getMessage())
                                                );
                                                errorList.add(
                                                        StringUtils
                                                                .format(ENTITY_VALIDATION_ERROR, enrichmentResult.getMessage()));
                                            }
                                            if (response.getValidation() != null
                                                    && response.getValidation().getStatus().equals(Status.ERROR)) {
                                                List<RuleResult> ruleresult = response.getValidation().getViolations();
                                                ruleresult.forEach(en ->
                                                        logger.info("{} status: {}, reason:{}",
                                                                en.getRule().getImplementation(),en.getStatus(),en
                                                                        .getMessage())
                                                );
                                                String error = org.apache.commons.lang.StringUtils
                                                        .join(ruleresult.stream().map(RuleResult::getMessage
                                                        ).collect(Collectors.toList()), SEPARATOR);
                                                errorList.add(StringUtils.format(VALIDATION_ERROR, error));
                                            }

                                        }
                                        else if (response.getStatus().equals(OperationStatus.Success)) {
                                            handelValidationSuccessMessage(response,successMessageList,dataset,entityClass);
                                        }
                                        else {
                                            dataset.get(entityClass).addAll(response.getEnrichment().getEnrichedData());
                                        }
                                    });
                                }

                            }
                        }

                    });
                }catch(TransformationException tex) {
                    tex.printStackTrace();
                    logger.error("Transformation exception {} for {}. Please check input data ",tex.getMessage(),operation.getInput());
                    errorList.add(tex.getLocalizedMessage());
                }catch(Exception ex) {
                    ex.printStackTrace();
                    logger.error("Exception {}. Please check input data ",ex.getMessage());
                    errorList.add(ex.getLocalizedMessage());
                }

            }

            if (errorList.isEmpty() && anyMessage.get()) {
                try {
                    operation.setResponse(MDMDispatcher.dispatch(dataset, infos));
                } catch (Throwable th) {
                    th.printStackTrace();
                    errorList.add(StringUtils.format(SAVE_ERROR, ExceptionUtils.getRootCause(th).getMessage()));
                }
            }
            if(ObjectUtils.isNotEmpty(errorList)) {
                operation.setError(org.apache.commons.lang.StringUtils.join(errorList,SEPARATOR));
            }else{
                operation.setStatus(OperationStatus.Success);
                operation.setError(org.apache.commons.lang.StringUtils.join(successMessageList,SEPARATOR));
            }
        });
        return operation;
    }

    @SuppressWarnings("rawtypes")
    private Map<Class, Set<CommonDataModel>> getCollector(
            Collection<TaskAttributeRequestTemplate.TransformerInfo> infos) {
        Map<Class, Set<CommonDataModel>> dataset = new LinkedHashMap<>();
        infos.stream().forEach(element -> {
            Class entityClass = EntityUtils.get().getEntityClass(element.getEntityName());
            Set<CommonDataModel> data = dataset.get(entityClass);
            if (data == null) {
                data = new HashSet<>();
                dataset.put(entityClass, data);
            }
        });
        return dataset;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void handelValidationSuccessMessage(OperationResponse response,List<String> successMessageList,Map<Class,
            Set<CommonDataModel>> dataset,Class entityClass){
        if(response.getValidation() != null && response.getValidation().getStatus()!=null
                && response.getValidation().getStatus().equals(Status.OK) && response.getValidation().getSuccessMessages()!=null && !response.getValidation().getSuccessMessages().isEmpty()){
            List<RuleResult> ruleResult = response.getValidation().getSuccessMessages();
            String successMessage = org.apache.commons.lang.StringUtils
                    .join(ruleResult.stream().map(RuleResult::getMessage
                    ).collect(Collectors.toList()), SEPARATOR);
            successMessageList.add(StringUtils.format(successMessage));
        }
        dataset.get(entityClass).addAll(response.getEnrichment().getEnrichedData());
    }

}
