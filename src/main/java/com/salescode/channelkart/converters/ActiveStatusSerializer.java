/*
 * Copyright (c) 2021. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */
package com.salescode.channelkart.converters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.salescode.channelkart.models.enums.ActiveStatus;

import java.io.IOException;

public class ActiveStatusSerializer extends StdSerializer<ActiveStatus> {

    private static final long serialVersionUID = 1369782913423989328L;

    public ActiveStatusSerializer() {
        super(ActiveStatus.class);
    }

    public ActiveStatusSerializer(Class t) {
        super(t);
    }

    public void serialize(ActiveStatus activeStatus, JsonGenerator generator, SerializerProvider provider)
            throws IOException {
        generator.writeString(activeStatus.getStatus());
    }


}