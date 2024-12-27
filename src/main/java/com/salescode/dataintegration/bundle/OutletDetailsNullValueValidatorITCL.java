package com.salescode.dataintegration.bundle;


import com.salescode.channelkart.models.OutletDetails;
import com.salescode.channelkart.services.SpringContext;
import com.salescode.channelkart.services.UserService;
import com.salescode.channelkart.validations.AbstractRule;
import com.salescode.channelkart.validations.RuleResult;
import com.salescode.channelkart.validations.Status;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class OutletDetailsNullValueValidatorITCL extends AbstractRule<OutletDetails> {

    final UserService userService = (UserService) SpringContext.getBean(UserService.class);

    @Override
    public RuleResult apply(OutletDetails cdm) {

        // TODO Auto-generated method stub
        List<String> ruleResult = new ArrayList<String>();
        if (cdm.getOutletName() == null || "".equals(cdm.getOutletName())) {
            ruleResult.add("OutletName can not be null");
        }

        if (cdm.getOutletCode() == null || "".equals(cdm.getOutletCode())) {
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
            return new RuleResult(Status.ERROR, StringUtils.join(ruleResult, ", "));
        } else {
            return RuleResult.OK;
        }
    }

}

