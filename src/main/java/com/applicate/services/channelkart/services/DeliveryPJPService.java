package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.utils.JSONUtils;

import com.salescode.dim.jooq.generated.tables.pojos.DeliveryPjp;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DeliveryPJPService extends AbstractCDMService<DeliveryPjp> {
    private static final String DAY_TAG = "day";
    private static final String FREQUENCY_TAG = "frequency";

    public void addDayAndFrequency(DeliveryPjp pjp) {

        ObjectNode dayAndFrequency = JSONUtils.getObjectMapper().createObjectNode();

        LocalDateTime pjpDate = pjp.getPjpDate();

        Date date = Date.from(pjpDate.atZone(ZoneId.systemDefault()).toInstant());

        Calendar currDate = Calendar.getInstance();
        currDate.setTime(date);

        dayAndFrequency.put(DAY_TAG,
                currDate.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH).toLowerCase());
        dayAndFrequency.put(FREQUENCY_TAG, currDate.get(Calendar.WEEK_OF_MONTH));
        pjp.setDayAndFrequency(JSONUtils.getObjectMapper().createArrayNode().add(dayAndFrequency));
        pjp.setMonth(currDate.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH).toLowerCase());
        pjp.setYear(currDate.get(Calendar.YEAR) + "");
    }

}
