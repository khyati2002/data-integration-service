package com.salescode.channelkart.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.channelkart.event.queue.message.SerializableEventData;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.services.enums.EntityOperation;
import com.salescode.channelkart.utils.JSONUtils;
import com.salescode.channelkart.utils.JsonNodeBuilder;
import com.salescode.channelkart.utils.ReflectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CdmEventData implements SerializableEventData {

    private EntityOperation operation;

    private List<String> cdmIdentifiers;

    private List<CommonDataModel> models;

    private Class<CommonDataModel> cdmClazz;

    public EntityOperation getOperation() {
        return operation;
    }

    public CdmEventData setOperation(EntityOperation operation) {
        this.operation = operation;
        return this;
    }

    public List<String> getCdmIdentifiers() {
        if (this.cdmIdentifiers == null) {
            return getModels().stream().map(CommonDataModel::getId).collect(Collectors.toList());
        }
        return Collections.unmodifiableList(this.cdmIdentifiers);
    }

    public CdmEventData setCdmIdentifiers(List<String> cdmIdentifiers) {
        this.cdmIdentifiers = cdmIdentifiers;
        return this;
    }

    public List<CommonDataModel> getModels() {
        return this.models == null ? Collections.emptyList() : Collections.unmodifiableList(this.models);
    }

    public CdmEventData setModels(List<CommonDataModel> models) {
        this.models = models;
        return this;
    }

    public Class<CommonDataModel> getCdmClazz() {
        return cdmClazz;
    }

    public CdmEventData setCdmClazz(Class<CommonDataModel> cdmClazz) {
        this.cdmClazz = cdmClazz;
        return this;
    }

    @Override
    public String serialize() {
        return new JsonNodeBuilder()
                .with("operation", this.operation.name())
                .with("cdmIdentifiers", JSONUtils.toJsonString(getCdmIdentifiers()))
                .with("models", JSONUtils.toJsonString(getModels()))
                .with("cdmClass", getCdmClazz().getName())
                .build()
                .toString();
    }

    @Override
    public void deserialize(String payload) {
        JsonNode input = JSONUtils.parse(payload);
        this.operation = EntityOperation.valueOf(input.get("operation").asText());
        this.cdmIdentifiers = JSONUtils.toList(input.get("cdmIdentifiers").asText(), String.class);
        this.cdmClazz = ReflectionUtils.loadClass(input.get("cdmClass").asText());
        this.models = JSONUtils.toList(input.get("models").asText(), this.cdmClazz);
    }
}

