package com.salescode.channelkart.datastreams;

import com.salescode.channelkart.exceptions.CustomRuntimeException;
import com.salescode.channelkart.exceptions.EntitySaveException;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.pojo.TaskAttributeRequestTemplate;
import com.salescode.channelkart.security.SecurityContextUtils;
import com.salescode.channelkart.services.SpringContext;
import com.salescode.channelkart.utils.TimerUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.JDBCException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;
import java.util.concurrent.*;

public class MDMDispatcher {
	private static Logger logger = LoggerFactory.getLogger(MDMDispatcher.class);

	private static int activeThreadCount = Integer.parseInt(Optional.ofNullable(System.getenv("activeThreads")).orElse(System.getProperty("activeThreads", "5")));
	private static boolean disableLocalRetry = Boolean.parseBoolean(Optional.ofNullable(System.getenv("disableLocalRetry")).orElse("false"));

	private static final List<String> RETRY_SQL_STATES= List.of("23000");
	
	private static ArrayBlockingQueue<Runnable> abq =  new ArrayBlockingQueue<Runnable>(Integer.parseInt(System.getProperty("activeQueue", "100"))) {
		@Override
		public boolean offer(Runnable e) {
			try {
				this.put(e);
			} catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
				e1.printStackTrace();
			}
			return true;
		}
	};

	private static final ThreadPoolExecutor cte = new ThreadPoolExecutor(activeThreadCount,activeThreadCount,120,TimeUnit.SECONDS,abq);

	@SuppressWarnings("rawtypes")
	public static Future<Map<Class, Set<CommonDataModel>>> dispatch(Map<Class,Set<CommonDataModel>> dataset, Collection<TaskAttributeRequestTemplate.TransformerInfo> infos){
		String lob = SecurityContextUtils.getLob();
		return cte.submit(new Callable<Map<Class,Set<CommonDataModel>>>() {
			@Override
			public Map<Class,Set<CommonDataModel>> call() throws Exception {
				int c=0;
	    		while(c<3) {
		    		try {
		    			final boolean isToRefresh = c!=0;
							return SpringContext.getBean(MDMActor.class).save(dataset, infos, isToRefresh);
		    		}
		    		catch(CustomRuntimeException e1) {
						throw e1;
					}
		    		catch(Exception e) {
						c++;
						if(c>=3 || disableLocalRetry) {
							throw e;
						}else{
							evaluateMDMFailureRetry(e, c);
						}
					}
	    		}
	    		throw new EntitySaveException("unknown error while saving records");
			}
		});
	}

	private static void evaluateMDMFailureRetry(Throwable e,int count) throws InterruptedException {
			if(ExceptionUtils.indexOfThrowable(e, DataIntegrityViolationException.class) != -1) {
				Throwable th = ExceptionUtils.getRootCause(e);
				if (th instanceof SQLIntegrityConstraintViolationException && RETRY_SQL_STATES.contains(((SQLIntegrityConstraintViolationException) th).getSQLState())) {
					Thread.sleep((long)count * 200);
				} else {
					throw new EntitySaveException(th, "Error while processing record to database. {}", th.getLocalizedMessage());
				}
			}else if (ExceptionUtils.indexOfThrowable(e, JDBCException.class) != -1){
				logger.error("Error while saving records. Reason {}. Retrying...",e.getMessage());
				Thread.sleep((long)count * 200);
			}
			else {
				Thread.sleep((long)count * 200);
			}
	}
}
