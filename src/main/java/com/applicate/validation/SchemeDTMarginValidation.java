package com.applicate.validation;
import com.applicate.services.channelkart.utils.NullUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.SchemeDefination;
import com.applicate.services.channelkart.utils.StringUtils;
import org.apache.flink.shaded.netty4.io.netty.util.internal.StringUtil;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.Date;


public class SchemeDTMarginValidation extends AbstractValidationRule<SchemeDefination> {

    @Override
    public OperationResult.StepResult apply(SchemeDefination scheme) {
        if(NullUtils.isNotNull(scheme.getProgramLevel()) && scheme.getProgramLevel().equalsIgnoreCase("dt_discount") && isEndDateExpired(Date.from(scheme.getEndDate().toInstant(ZoneOffset.of("UTC"))))){
            String errors = "End Date before the current Date while saving the DT discount object";
            return OperationResult.StepResult.ERROR;
        }
        return OperationResult.StepResult.OK;
    }

    private boolean isEndDateExpired(Date endDate){
        return NullUtils.isNotNull(endDate) && endDate.before(new Date());
    }
}