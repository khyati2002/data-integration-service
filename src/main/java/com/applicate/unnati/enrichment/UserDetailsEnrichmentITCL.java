
package com.applicate.unnati.enrichment;


import com.applicate.services.channelkart.services.*;
import com.github.jknack.handlebars.internal.lang3.StringUtils;
import com.applicate.services.channelkart.converters.DateToClientTimeZoneStringConverter;
import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.EnrichmentResult;
import com.applicate.services.channelkart.enrichments.Status;
import com.applicate.services.channelkart.models.CustomerAccountInfo;
import com.applicate.services.channelkart.models.HierarchyMetaData;
import com.applicate.services.channelkart.models.SupplierMetaData;
import com.applicate.services.channelkart.models.User;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.security.SecurityContextUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * The class UserEnrichment.
 *
 * @author Athresh KS
 */
public class UserDetailsEnrichmentITCL extends AbstractEnrichment<User> {

    /** The userservice. */
    final UserService userservice= (UserService) SpringContext.getBean(UserService.class);

//    /** The sso service. */
//    final SSOService ssoService= (SSOService) SpringContext.getBean(SSOService.class);

    /**
     * Apply.
     *
     * @param cdm the cdm
     * @return the enrichment result
     */
    @Override
    public EnrichmentResult apply(User cdm) {

        if(NullUtils.isNotNull(cdm)) {
            if(cdm.getPassword() == null) {
                cdm.setPassword(userservice.getDefaultEncryptedUserPassword());
            }

            String lob = cdm.getLob();
            if(lob==null){
                lob = SecurityContextUtils.getLob();
            }
            CustomerAccountsService customerService = (CustomerAccountsService) ServiceLocator.lookup(CustomerAccountInfo.class);
            CustomerAccountInfo customerAccount = customerService.getCustomerAccountInfo(lob);

            if(cdm.getLocationHierarchy() == null) {
                cdm.setLocationHierarchy(customerAccount.getAdmin().getLocationHierarchy());
            }
            if(cdm.getSupplierMetaData() != null && !cdm.getSupplierMetaData().isEmpty()) {
                SupplierMetaData supplierMetaData=cdm.getSupplierMetaData().get(0);
                int min = supplierMetaData.getMin() == null ? 0 : supplierMetaData.getMin().intValue();
                int max = supplierMetaData.getMax() == null ? 0 : supplierMetaData.getMax().intValue();
                if(min<=0 && max<=0 && supplierMetaData.getType()==null)
                    cdm.setSupplierMetaData(new ArrayList<>());
            }

            if(cdm.getActiveStatus() == null || cdm.getActiveStatus().equals(ActiveStatus.INACTIVE)) {
                cdm.setActiveStatus(ActiveStatus.INACTIVE);
                if(StringUtils.isBlank(cdm.getActiveStatusReason()) ||
                        !cdm.getActiveStatusReason().startsWith("Deactivated")) {
                    cdm.setActiveStatusReason("Deactivated by "+SecurityContextUtils.getPrincipal()+
                            " on "+ new DateToClientTimeZoneStringConverter().convert(new Date()));
                }
            }else if(cdm.getActiveStatus().equals(ActiveStatus.ACTIVE)) {
                cdm.setActiveStatus(ActiveStatus.ACTIVE);
                if(StringUtils.isBlank(cdm.getActiveStatusReason()) ||
                        cdm.getActiveStatusReason().startsWith("Deactivated")) {
                    cdm.setActiveStatusReason(ActiveStatus.ACTIVE.name());
                }
            }

//            if(cdm.getSsoId() == null || cdm.getSsoId().equals("none")) {
//                Optional<SSOConfiguration> ssoconfig= ssoService.getConfigurationByUserDesignation(cdm);
//                if(ssoconfig != null && ssoconfig.isPresent()) {
//                    cdm.setSsoId(ssoconfig.get().getRegistrationId());
//                }
//            }

            if(cdm.getDesignation().contains("supplier")) {
                if(ObjectUtils.isEmpty(cdm.getImmediateParent())) {
                    User admin= customerAccount.getAdmin();
                    if(!cdm.getLoginId().equalsIgnoreCase(admin.getLoginId())) {
                        HierarchyMetaDataService hierarchyMetaDataService = (HierarchyMetaDataService) ServiceLocator
                                .lookup(HierarchyMetaData.class);
                        Collection<HierarchyMetaData> adminMetaData = hierarchyMetaDataService
                                .findByImmediateParent(admin.getLoginId());
                        cdm.setImmediateParent(adminMetaData.stream().collect(Collectors.toList()));
                        String hierarchy = customerService.getAdminHierarchy(cdm.getLoginId());
                        cdm.setHierarchy(hierarchy);
                    }
                }
            }

            return new EnrichmentResult(Status.OK,"Data enriched successfully");
        }
        return new EnrichmentResult(Status.ERROR,"Enrichment error: User not found null");
    }

}
