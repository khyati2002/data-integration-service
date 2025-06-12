package com.applicate.services.channelkart.client.properties;

import com.applicate.services.channelkart.services.MetaDataService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.jooq.generated.tables.pojos.Metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PropertyRegistry {

	private static PropertyService service;

	private static PropertyRegistry instance;
	private static final Map<String, PropertyHolder> properties = new ConcurrentHashMap<>();

	public PropertyRegistry(PropertyService service) {
		this.service = new PropertyService((MetaDataService) ServiceLocator.lookup(Metadata.class));
	}

	public static PropertyRegistry getInstance(PropertyService propertyService){
		if(instance== null){
			instance = new PropertyRegistry(propertyService);
		}
	  return instance;
	}

	public static PropertyRegistry getInstance(){
		if(instance == null){
			throw new RuntimeException("Instance not initialized yet");
		}
		return instance;
	}

	public static boolean getAsBoolean(PropertyDefinition definition) {
		return Boolean.parseBoolean(getValue(definition));
	}

	public static String getValue(PropertyDefinition definition) {
		return getOrFail(definition.getName()).getValue();
	}

	public static Property getOrFail(String name) {
		return getProperty(name)
				.orElseThrow(() -> new RuntimeException("Could not find property with name:" + name));
	}

	public static Optional<Property> getProperty(String name) {
		return getPropertyHolder().get(name);
	}

	private static PropertyHolder getPropertyHolder() {
		String lob = getLob();
		return getPropertyHolder(lob);
	}

	private static PropertyHolder getPropertyHolder(String lob) {
		return properties.computeIfAbsent(lob, key -> {
			Set<Property> all = service.findAll();
			return new PropertyRegistry.PropertyHolder(all);
		});
	}

	private static String getLob() {
		String lob = SecurityContextUtils.getLob();
		return lob;
	}

	private static class PropertyHolder {

		private final Map<String, Property> properties;

		public PropertyHolder(Set<Property> clientProperties) {
			this.properties = clientProperties.stream()
					.collect(Collectors.toMap(Property::getName, property -> property, (i, j) -> j));
		}

		public Optional<String> getProperty(String name) {
			Property property = properties.get(name);
			if (property != null) {
				return Optional.of(property.getValue());
			}
			return Optional.empty();
		}

		public Optional<Property> get(String name) {
			return Optional.ofNullable(this.properties.get(name));
		}

		public String getOrDefault(String name, String defaultValue) {
			return getProperty(name).orElse(defaultValue);
		}

		public Map<String, String> toMap() {
			return this.properties.entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, p -> p.getValue().getName()));
		}

		public List<Property> getProperties() {
			return new ArrayList<>(this.properties.values());
		}


	}
}
