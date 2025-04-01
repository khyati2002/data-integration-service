package com.salescode.dim.utils;
import com.applicate.services.channelkart.models.diff.Change;
import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import lombok.*;

import java.io.Serializable;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventListenerDTO implements Serializable {

    private String requestId;
    private String entityName;
    private String lob;
    private Set<Change<Serializable>> changes;
    private ActionType actionType;
    private String cdmId;
    private String loginId;
}
