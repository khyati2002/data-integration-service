/*
 * Copyright (c) 2021. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.converters;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Objects;

@Component
@Converter(autoApply = true)
public class ActiveStatusConverter implements AttributeConverter<ActiveStatus, String> {

    @Override
    public String convertToDatabaseColumn(ActiveStatus attribute) {
        return Objects.requireNonNullElse(attribute, ActiveStatus.INACTIVE).getStatus();
    }

    @Override
    public ActiveStatus convertToEntityAttribute(String dbData) {
        if (StringUtils.isNotBlank(dbData)) {
            return ActiveStatus.getRegistry(dbData);
        }
        return ActiveStatus.INACTIVE;
    }

}
