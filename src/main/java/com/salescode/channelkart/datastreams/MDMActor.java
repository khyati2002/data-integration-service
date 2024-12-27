package com.salescode.channelkart.datastreams;

import com.salescode.channelkart.exceptions.CustomRuntimeException;
import com.salescode.channelkart.exceptions.EntitySaveException;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.pojo.TaskAttributeRequestTemplate;
import com.salescode.channelkart.services.CommonDataModelService;
import com.salescode.channelkart.services.ServiceLocator;
import com.salescode.channelkart.services.SpringContext;
import com.salescode.channelkart.services.enums.OperationType;
import com.salescode.channelkart.utils.TimerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MDMActor {
	
	private Logger logger = LoggerFactory.getLogger(MDMActor.class);
	
	@SuppressWarnings({"unchecked" ,"rawtypes"})
	public Map<Class,Set<CommonDataModel>> save(Map<Class,Set<CommonDataModel>> dataset, Collection<TaskAttributeRequestTemplate.TransformerInfo> infos, boolean refresh) throws EntitySaveException {
		try {
			Map<Class,Set<CommonDataModel>> collector= new LinkedHashMap<>();
		    Iterator<Class> iterator= dataset.keySet().iterator();
		    while(iterator.hasNext()) {
		    	Class clazz = iterator.next();
		    	CommonDataModelService<CommonDataModel> cdmService = SpringContext
		    					.getBean(ServiceLocator.lookup(clazz).getClass());
		    	OperationType opType= infos.stream().filter(elem->elem.getEntityName().equalsIgnoreCase(clazz.getSimpleName()))
		    	     .map(TaskAttributeRequestTemplate.TransformerInfo::getOperationType).findFirst().orElse(null);
		    	Iterator<CommonDataModel> iter= dataset.get(clazz).iterator();
		    	Set<CommonDataModel> savedset= new HashSet<>();
		    	while(iter.hasNext()) {
		    		CommonDataModel cdm = iter.next();
	    	    	String id = cdmService.getKey(cdm);
	    	    	if(id==null) {
	    	    		id=UUID.randomUUID().toString();
	    	    	}
	    	    	process(id,clazz,opType,cdm,cdmService,savedset,refresh);
		    	}
		    	collector.put(clazz, savedset);
		    }
		    return collector;
		}
		catch(CustomRuntimeException ex) {
			throw ex;
		}
		catch(Exception th) {
			throw th;
		}
	}
	
	@SuppressWarnings("rawtypes")
	private void process(String id, Class clazz, OperationType opType, CommonDataModel cdm, CommonDataModelService<CommonDataModel> cdmService,
			Set<CommonDataModel> savedset, boolean refresh) {
		logger.debug("{} id -> {}",clazz.getName(),id);
		synchronized(id.intern()) { 
			if (opType!=null && OperationType.delete.equals(opType)) {
				final String cdm_id = id;
				TimerUtils.withTime("Time taken to delete CDM record type " + clazz.getName(), () -> cdmService.deleteById(cdm_id,true));
				savedset.add(cdm);
			} else {
				if (refresh) {
					cdm = cdmService.refresh(cdm);
				}
				List<CommonDataModel> cdms = TimerUtils.withTime("Time taken to save CDM record type " + clazz.getName(), cdm, cd -> cdmService.saveForList(cd, opType));
				savedset.addAll(cdms);
			}
		}
	}
	
}