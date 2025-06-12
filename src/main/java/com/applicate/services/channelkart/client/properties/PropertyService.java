package com.applicate.services.channelkart.client.properties;

import com.applicate.services.channelkart.services.MetaDataService;
import com.applicate.services.channelkart.utils.JSONUtils;

import com.salescode.dim.jooq.generated.tables.pojos.Metadata;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PropertyService {

	private static final String DOMAIN_NAME = "client";

	private static final String DOMAIN_TYPE = "properties";

	private final MetaDataService metaDataService;
	public PropertyService(MetaDataService metaDataService) {
		this.metaDataService = new MetaDataService();
	}
	public Set<Property> findAll() {
		Set<Property> storedProperties = findMetaData().map(this::toClientPropertySet).orElseGet(HashSet::new);
		Arrays.stream(PropertyDefinition.values()).forEach(definition -> {
			Property prop = Property.fromDefinition(definition);
			storedProperties.add(prop);
		});
		return storedProperties;
	}
	private Set<Property> toClientPropertySet(Metadata metaData) {
		ArrayNode domainValues = (ArrayNode) metaData.getDomainValues();
		if (domainValues == null) {
			return Set.of();
		}
		return JSONUtils.stream(domainValues)
				.map(Property::new)
				.collect(Collectors.toSet());
	}
	private Optional<Metadata> findMetaData() {
		return Optional.ofNullable(metaDataService.fetchByValue(DOMAIN_NAME, DOMAIN_TYPE));
	}

}
