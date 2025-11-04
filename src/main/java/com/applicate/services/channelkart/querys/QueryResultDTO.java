package com.applicate.services.channelkart.querys;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.applicate.services.channelkart.response.OperationStatus;

public class QueryResultDTO<E>  implements Serializable {


	private static final long serialVersionUID = 196266035671441438L;

	/** The query name. */
	private String queryName;

	/** The query type. */
	@JsonIgnore
	private QueryType queryType= QueryType.single;

	/** The data. */
	private List<E> data;

	/** The current page number. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Integer page;

	/** The current page size. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Integer pageSize;

	/** The total pages. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Integer totalPage;

	/** The total elements. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Long    totalElements;

	/** For holding the final query useful for logging */
	@JsonIgnore
	private String finalQuery;

	@JsonIgnore
	private OperationStatus status = OperationStatus.Success;

	@JsonIgnore
	private Exception exception;

	/**
	 * Gets the query name.
	 *
	 * @return the queryName
	 */
	public String getQueryName() {
		return queryName;
	}

	/**
	 * Sets the query name.
	 *
	 * @param queryName the queryName to set
	 * @return the query result DTO
	 */
	public QueryResultDTO<E> setQueryName(String queryName) {
		this.queryName = queryName;
		return this;
	}

	/**
	 * Gets the data.
	 *
	 * @return the data
	 */
	public List<E> getData() {
		return data;
	}

	/**
	 * Sets the data.
	 *
	 * @param data the data to set
	 * @return the query result DTO
	 */
	public QueryResultDTO<E> setData(List<E> data) {
		this.data = data;
		return this;
	}

	/**
	 * Creates the.
	 *
	 * @return the query result DTO
	 */
	@JsonIgnore
	public static QueryResultDTO create() {
		return new QueryResultDTO();
	}

	/**
	 * @return the queryType
	 */
	public QueryType getQueryType() {
		return queryType;
	}

	/**
	 * @param queryType the queryType to set
	 */
	public QueryResultDTO<E> setQueryType(QueryType queryType) {
		this.queryType = queryType;
		return this;
	}

	/**
	 * @return the totalElements
	 */
	public Long getTotalElements() {
		return totalElements;
	}

	/**
	 * @param totalElements the totalElements to set
	 */
	public QueryResultDTO<E> setTotalElements(Long totalElements) {
		this.totalElements = totalElements;
		return this;
	}

	/**
	 * @return the page
	 */
	public Integer getPage() {
		return page;
	}

	/**
	 * @param page the page to set
	 */
	public QueryResultDTO<E> setPage(Integer page) {
		this.page = page;
		return this;
	}

	/**
	 * @return the pageSize
	 */
	public Integer getPageSize() {
		return pageSize;
	}

	/**
	 * @param pageSize the pageSize to set
	 */
	public QueryResultDTO<E> setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
		return this;
	}

	public Exception getException() {
		return exception;
	}

	public QueryResultDTO<E> setException(Exception exception) {
		this.exception = exception;
		return this;
	}

	/**
	 * @return the totalPage
	 */
	public Integer getTotalPage() {
		return totalPage;
	}

	/**
	 * @param totalPage the totalPage to set
	 */
	public QueryResultDTO<E> setTotalPage(Integer totalPage) {
		this.totalPage = totalPage;
		return this;
	}

	public String getFinalQuery() {
		return finalQuery;
	}

	public QueryResultDTO<E> setFinalQuery(String finalQuery) {
		this.finalQuery = finalQuery;
		return this;
	}

	public OperationStatus getStatus() {
		return status;
	}

	public QueryResultDTO<E> setStatus(OperationStatus status) {
		this.status = status;
		return this;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public Map<String, Object> toDataMap() {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("queryName", queryName);
		List<E> currentData = getData();
		List convertedData = new ArrayList<>();
		if (currentData != null) {
			for (E datum : currentData) {
				if (datum instanceof QueryResultDTO) {
					convertedData.add(((QueryResultDTO) datum).toDataMap());
				} else {
					convertedData.add(datum);
				}
			}
		}
		response.put("data", convertedData);
		return response;
	}
	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "QueryResultDTO [queryName=" + queryName + ", queryType=" + queryType + ", data=" + data + ", page="
				+ page + ", pageSize=" + pageSize + ", totalPage=" + totalPage + ", totalElements=" + totalElements + ", status=" + status
				+ "]";
	}


}

