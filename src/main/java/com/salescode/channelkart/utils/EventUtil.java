package com.salescode.channelkart.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.channelkart.models.CommonDataModel;
import com.salescode.channelkart.models.EventListenerInfo;
import com.salescode.channelkart.services.enums.EntityOperation;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EventUtil {

    public static boolean isListenerConfigsPresent(CommonDataModel commonDataModel,
                                                   EntityOperation operation, List<EventListenerInfo> eventListenerInfos, Set<String> eventTopics) {
        if (CollectionUtils.isNotEmpty(eventTopics)) {
            return eventListenerInfos.stream()
                    .anyMatch(info -> eventTopics.contains(info.getTopic()));
        }
        return !findMatchingListenerConfig(commonDataModel, operation, eventListenerInfos).isEmpty();
    }

    public static List<EventListenerInfo> findMatchingListenerConfig(CommonDataModel dataModel,
                                                                     EntityOperation operation,  List<EventListenerInfo> eventListenerInfos) {
        return eventListenerInfos.stream()
                .filter(info -> EventUtil.isMatching(info, dataModel, operation))
                .collect(Collectors.toList());
    }

    public static boolean isMatching(EventListenerInfo info, CommonDataModel dataModel, EntityOperation operation) {
        JsonNode configurations = info.getConfigurations();
        if (isModelMatching(dataModel, configurations)) {
            String operations = configurations.get("operation").asText();
            return operations.contains(operation.name());
        }
        return false;
    }

    public static boolean isModelMatching(CommonDataModel dataModel, JsonNode configurations) {
        return configurations != null
                && configurations.has("cdm")
                && dataModel.getClass().getName().equals(configurations.get("cdm").asText());
    }
}
