package com.applicate.services.channelkart.services;

import org.jooq.DSLContext;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.tables.CkUserdesignation.CK_USERDESIGNATION;

public class UserdesignationService {

	private final DSLContext dsl;

	// You must pass DSLContext when constructing this service
	public UserdesignationService(DSLContext dsl) {
		this.dsl = dsl;
	}

	/**
	 * Fetches all unique designations for the given loginId from CK_USERDESIGNATION table.
	 *
	 * @param loginId the user login ID
	 * @return a Set of designations
	 */
	public Set<String> getDesignationsByLoginId(String loginId) {
		return dsl.select(CK_USERDESIGNATION.DESIGNATION)
				.from(CK_USERDESIGNATION)
				.where(CK_USERDESIGNATION.LOGIN_ID.eq(loginId))
				.fetch(CK_USERDESIGNATION.DESIGNATION)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}
}
