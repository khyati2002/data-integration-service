package com.salescode.channelkart.validations;

import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.scanner.ExternalRegistryScanner;
import com.salescode.channelkart.utils.TimerUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;

public class RuleLogicEngine {

	private static final Logger log = LoggerFactory.getLogger(RuleLogicEngine.class);

	private static final String ERROR_MESSAGE = "Some error occurred while validating input";

	public static final RuleLogicEngine INSTANCE = new RuleLogicEngine();
	
	public RuleResult execute(CommonDataModel cdm, RuleInfo rule) {
		RuleResult result=null;
		try {				
			AbstractRule ar = getRule(rule);			
			result= TimerUtils.withTime("RuleLogicEngine "+getImple(rule),()->ar.apply(rule,cdm));
		}catch (Exception e) {
			log.error("Rule failed for cdm class:{} and rule is {}", cdm.getClass(), rule, e);
			result= new RuleResult(Status.ERROR,ERROR_MESSAGE,ERROR_MESSAGE);
			cdm.addPreProcessPipelineException(ExceptionUtils.getStackTrace(e));
			result.setRule(rule);
			StringWriter sw = new StringWriter();
			result.setException(sw.toString());
			result.setRule(rule);
			return result;
		}
		result.setRule(rule);
		return result;
	}

	private String getImple(RuleInfo ar) {
		return ar !=null ? ar.getImplementation() : "Implementation is null";
	}

	private AbstractRule getRule(RuleInfo rule) throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		if(ExternalRegistryScanner.getInstance().isProxyRule(rule.getImplementation())){
			return ExternalRegistryScanner.getInstance().getValidationProxyRule();
		}else {
			AbstractRule ar = (AbstractRule) Class.forName(rule.getImplementation()).newInstance();
			ar.setRuleInfo(rule);
			return ar;
		}
	}
	
}
