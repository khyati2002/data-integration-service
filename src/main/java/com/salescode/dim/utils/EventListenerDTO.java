package com.salescode.dim.utils;
import com.applicate.services.channelkart.models.diff.Change;
import com.applicate.services.channelkart.models.enums.ActionType;
import lombok.*;

import java.io.Serializable;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventListenerDTO {

    private String requestId;
    private String entityName;
    private String lob;
    private Set<Change<Serializable>> changes;
    private ActionType actionType;
}
