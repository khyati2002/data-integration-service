/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.applicate.services.channelkart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The class ComprehensiveLogger.
 *
 * @author Manish Srivastava
 * @since  Oct 2020
 */
public class ComprehensiveLogger {
	
	/** The container. */
	private List<Log> container= new CopyOnWriteArrayList<>();
	
	/**
	 * Adds the.
	 *
	 * @param th the th
	 * @return the error logger
	 */
	public ComprehensiveLogger add(Throwable th) {
		if(th != null) {
			Log log= new Log();
			container.add(log.setData(th));
		}
		return this;
	}
	

	/**
	 * Gets the.
	 *
	 * @return the error logger
	 */
	public ComprehensiveLogger get() {
		return this;
	}
	
	/**
	 * Gets the logs.
	 *
	 * @return the logs
	 */
	public List<Log> getLogs() {
		return container;
	}
	
	/**
	 * The class Log.
	 *
	 * @author Manish Srivastava
	 * @since  2020
	 */
	public static class Log{
		
		/** The summary. */
		private String summary= "{} {}";
		
		/** The description. */
		private String description= "{}";
		
		/** The formatter. */
		@JsonIgnore
		private SimpleDateFormat dateFormatter= new SimpleDateFormat("'['dd-MM-yyyy 'at' HH:mm:ss z']'");
		
		public Log() {
			setTimeZone();
		}
		
		/**
		 * Gets the summary.
		 *
		 * @return the summary
		 */
		public String getSummary() {
			return summary;
		}

		/**
		 * Sets the summary.
		 *
		 * @param summary the summary
		 * @return the log
		 */
		public Log setSummary(String summary) {
			this.summary = StringUtils.format(this.summary, dateFormatter.format(new Date(System.currentTimeMillis())), summary);
		    return this;
		}

		/**
		 * Gets the description.
		 *
		 * @return the description
		 */
		public String getDescription() {
			return description;
		}

		/**
		 * Sets the description.
		 *
		 * @param description the description
		 * @return the log
		 */
		public Log setDescription(String description) {
			this.description = StringUtils.format(this.description, description);
			return this;
		}
       
		/**
		 * Sets the data.
		 *
		 * @param th the th
		 * @return the log
		 */
		public Log setData(Throwable th) {
			setSummary(th.getMessage());
			Throwable root= getRootCause(th);
			setDescription(ExceptionUtils.getStackTrace(root));
			return this;
		}
		
		/**
		 * Gets the root cause.
		 *
		 * @param result the result
		 * @return the root cause
		 */
		private Throwable getRootCause(Throwable result) {
		    Throwable cause = null; 
		    while(null != (cause = result.getCause())  && (result != cause) ) {
		        result = cause;
		    }
		    return result;
		}
        
		public static Log newInstance() {
			return new Log();
		}

		@Override
		public String toString() {
			return "Log [summary=" + summary + ", description=" + description + "]";
		}
		
		/**
		 * Sets the time zone.
		 */
		private void setTimeZone() {
			dateFormatter.setTimeZone(TimeZone.getDefault());
		}
	}
	
	public static ComprehensiveLogger newInstance() {
		return new ComprehensiveLogger();
	}
	
	/**
	 * Convert to string.
	 *
	 * @return the string
	 */
	public String convertToString() {
		return JSONUtils.stringify(container);
	}
}
