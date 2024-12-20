package com.salescode.dataintegration.bundle;


import com.salescode.channelkart.models.User;
import com.salescode.channelkart.utils.StringUtils;
import com.salescode.dataintegration.etl.validation.AbstractValidationRule;
import com.salescode.dataintegration.etl.validation.RuleResult;
import com.salescode.dataintegration.etl.validation.ValidationResult;

public class SupplierInfoValidatorITCL extends AbstractValidationRule<User> {

    @Override
    public RuleResult apply(User cdm) {
        // TODO Auto-generated method stub
        StringBuilder ruleResult = new StringBuilder();
        if (cdm.getDesignation().contains("supplier")) {
            if (cdm.getName() == null || "".equals(cdm.getName())) {
                ruleResult.append("Supplier name can't be null or empty.");
            }

            if (cdm.getExtendedAttributes() != null && cdm.getExtendedAttributes().has("WDCode")) {
                if (StringUtils.isNullOrBlank(cdm.getExtendedAttributes().get("WDCode").asText())) {
                    ruleResult.append("WDCode can't be null or empty.");
                }
            } else {
                ruleResult.append("WDCode can't be null or empty.");
            }

            if (!cdm.getSupplierMetaData().isEmpty()) {
                if (cdm.getSupplierMetaData().get(0).getExtendedAttributes().has("vajraSFAVersion")) {
                    String vajraVersion = cdm.getSupplierMetaData().get(0).getExtendedAttributes()
                            .get("vajraSFAVersion").asText();
                    if (vajraVersion.equals("Vajra1") || vajraVersion.equals("Vajra2")
                            || vajraVersion.equals("Vajra3")) {
                    } else {
                        ruleResult.append("Given VajraSFAVersion does not matches the required strings.It should be one of Vajra1,Vajra2,Vajra3.");
                    }
                } else {
                    ruleResult.append(
                            "VajraSFAVersion should not be null or empty. It should be one of Vajra1,Vajra2,Vajra3.");
                }

            } else {
                ruleResult.append(
                        "VajraSFAVersion should not be null or empty. It should be one of Vajra1,Vajra2,Vajra3.");
            }

        }
        if (ruleResult.length() > 0) {
            return new RuleResult(ValidationResult.Status.ERROR, ruleResult.toString());
        } else {
            return RuleResult.OK;
        }
    }

}
