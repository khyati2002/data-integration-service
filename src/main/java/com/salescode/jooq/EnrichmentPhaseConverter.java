package com.salescode.jooq;

import com.salescode.channelkart.converters.EnrichmentPhase;
import org.jooq.Converter;

import java.util.Objects;

public class EnrichmentPhaseConverter implements Converter<String, EnrichmentPhase> {

    @Override
    public EnrichmentPhase from(String databaseObject) {
        // Convert database value (String) to EnrichmentPhase enum
        return EnrichmentPhase.valueOf(databaseObject);
    }

    @Override
    public String to(EnrichmentPhase userObject) {
        // Convert EnrichmentPhase enum to database value (String)
        return Objects.requireNonNull(userObject).name();
    }

    @Override
    public Class<String> fromType() {
        // Database type: String
        return String.class;
    }

    @Override
    public Class<EnrichmentPhase> toType() {
        // User type: EnrichmentPhase
        return EnrichmentPhase.class;
    }
}