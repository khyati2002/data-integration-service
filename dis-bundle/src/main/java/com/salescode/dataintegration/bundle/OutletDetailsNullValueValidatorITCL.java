package com.salescode.channelkart.validations.impl;


import com.salescode.channelkart.services.SpringContext;
import com.salescode.channelkart.services.UserService;
import com.salescode.dataintegration.etl.validation.AbstractValidationRule;
import com.salescode.dataintegration.etl.validation.RuleResult;
import com.salescode.dataintegration.etl.validation.ValidationResult;
import com.salescode.jooq.generated.tables.pojos.CkOutletDetails;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class OutletDetailsNullValueValidatorITCL extends AbstractValidationRule<CkOutletDetails> {

    final UserService userService = (UserService) SpringContext.getBean(UserService.class);

    @Override
    public RuleResult apply(CkOutletDetails cdm) {

        // TODO Auto-generated method stub
        List<String> ruleResult = new ArrayList<String>();
        if (cdm.getOutletName() == null || "".equals(cdm.getOutletName())) {
            ruleResult.add("OutletName can not be null");
        }

        if (cdm.getOutletcode() == null || "".equals(cdm.getOutletcode())) {
            ruleResult.add("OutletName can not be null");
        }

        if (cdm.getChannel() == null || "".equals(cdm.getChannel())) {
            ruleResult.add("Channel can not be null");
        }

        if (cdm.getExtendedAttributes() != null) {
            if (!cdm.getExtendedAttributes().has("supplierMapping")) {
                ruleResult.add("custID can not be null, outletSIFYID can not be null");
            }
        } else {
            ruleResult.add(
                    "custID can not be null, outletSIFYID can not be null");
        }

        if (ruleResult.size() > 0) {
            return new RuleResult(ValidationResult.Status.ERROR, StringUtils.join(ruleResult, ", "));
        } else {
            return RuleResult.OK;
        }
    }

}
