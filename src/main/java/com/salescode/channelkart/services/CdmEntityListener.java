package com.salescode.channelkart.services;

import com.salescode.channelkart.dto.StreamingEventData;
import com.salescode.channelkart.models.CommonDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CdmEntityListener {

    private static final Logger log = LoggerFactory.getLogger(CdmEntityListener.class);


    public void notifyStreamingEvent(StreamingEventData<List<CommonDataModel>> eventData){
        getCdmPostEventService().ifPresent(cdmPostEventService -> cdmPostEventService.onPostEvent(eventData));
    }

    private Optional<CdmPostEventService> getCdmPostEventService() {
        try {
            return Optional.of(SpringContext.getBean(CdmPostEventService.class));
        } catch (Exception e) {
            log.error("CdmPostEventService is not yet initialized... post event service will be ignored until it initialize");
            return Optional.empty();
        }
    }
}
