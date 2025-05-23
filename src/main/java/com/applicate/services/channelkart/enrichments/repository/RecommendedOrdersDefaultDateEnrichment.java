package com.applicate.services.channelkart.enrichments.repository;


import com.applicate.services.channelkart.services.CustomerAccountsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.CustomerAccount;
import com.salescode.dim.jooq.generated.tables.pojos.RecommendedOrder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class RecommendedOrdersDefaultDateEnrichment extends AbstractEnrichment<RecommendedOrder> {

    final CustomerAccountsService customerAccountsService = (CustomerAccountsService) ServiceLocator.lookup(CustomerAccount.class);


    @Override
    public OperationResult.StepResult apply(RecommendedOrder cdm) {
        if (cdm.getStartDate() == null) {
            cdm.setStartDate(setDefaultStartDate());
        }
        if (cdm.getEndDate() == null) {
            cdm.setEndDate(setDefaultEndDate());
        }
        return OperationResult.StepResult.OK;
    }

    private LocalDateTime setDefaultStartDate() {

            ZonedDateTime currentClientTime = LocalDateTime.now().toLocalDate().withMonth(1).withDayOfMonth(1)
                                                    .atStartOfDay(getClientTimeZone());

            // Converting to UTC time zone
            ZonedDateTime currentUtcTime = currentClientTime.withZoneSameInstant(ZoneId.of("UTC"));

            // Returning the LocalDateTime in UTC
        return currentUtcTime.toLocalDateTime();
    }

    private LocalDateTime setDefaultEndDate() {
        LocalDateTime localDateTime = LocalDateTime.now().plusYears(1).toLocalDate().atTime(23, 59, 59);

        // Converting to the client's time zone
        ZonedDateTime currentClientTime = localDateTime.atZone(getClientTimeZone());

        // Converting to UTC time zone
        ZonedDateTime currentUtcTime = currentClientTime.withZoneSameInstant(ZoneId.of("UTC"));

        // Returning the LocalDateTime in UTC
        localDateTime = currentUtcTime.toLocalDateTime();

        return localDateTime;
    }


    private ZoneId getClientTimeZone() {
        String timeZoneStr = customerAccountsService.getTimeZone();
        timeZoneStr = timeZoneStr == null ? "Asia/Kolkata" : timeZoneStr;
        return ZoneId.of(timeZoneStr);
    }

}
