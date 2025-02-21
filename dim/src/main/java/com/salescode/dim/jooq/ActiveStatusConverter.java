package com.salescode.dim.jooq;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import org.jooq.Converter;

import java.util.Objects;

public class ActiveStatusConverter implements Converter<String, ActiveStatus> {

    private static final long serialVersionUID = -5471448745262321256L;

    @Override
    public ActiveStatus from(String databaseObject) {
        // Convert database value (String) to ActiveStatus enum
        return databaseObject != null ? ActiveStatus.getRegistry(databaseObject) : ActiveStatus.INACTIVE;
    }

    @Override
    public String to(ActiveStatus userObject) {
        // Convert ActiveStatus enum to database value (String)
        return Objects.requireNonNullElse(userObject, ActiveStatus.INACTIVE).getStatus();
    }

    @Override
    public Class<String> fromType() {
        // Database type: String
        return String.class;
    }

    @Override
    public Class<ActiveStatus> toType() {
        // User type: ActiveStatus
        return ActiveStatus.class;
    }
}