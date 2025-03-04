
package com.applicate.unnati.enrichment;




import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.*;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.CustomerAccount;
import com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.User;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

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
    final UserService userservice= (UserService) ServiceLocator.lookup(User.class);

//    /** The sso service. */
//    final SSOService ssoService= (SSOService) SpringContext.getBean(SSOService.class);

    /**
     * Apply.
     *
     * @param cdm the cdm
     * @return the enrichment result
     */
    @Override
    public OperationResult.StepResult apply(User cdm) {

        if(cdm != null) {
            if(cdm.getPassword() == null) {
                cdm.setPassword(userservice.DEFAULT_ENCODED_PASSWORD);
            }

            String lob = cdm.getLob();
            if(lob==null){
                lob = "ckunnatiuat";
            }
            CustomerAccountsService customerAccountsService = (CustomerAccountsService) ServiceLocator.lookup(CustomerAccount.class);
//            CustomerAccount customerAccount = customerAccountsService.getCustomerAccountInfo(lob);

            if(cdm.getLocationHierarchy() == null) {
                cdm.setLocationHierarchy(customerAccountsService.getAdminInfo().getLocationHierarchy());
            }
            if(cdm.getSupplierMetaData() != null && !cdm.getSupplierMetaData().isEmpty()) {
                SupplierMetadata supplierMetaData=cdm.getSupplierMetaData().get(0);
                int min = supplierMetaData.getMin() == null ? 0 : supplierMetaData.getMin().intValue();
                int max = supplierMetaData.getMax() == null ? 0 : supplierMetaData.getMax().intValue();
                if(min<=0 && max<=0 && supplierMetaData.getType()==null)
                    cdm.setSupplierMetaData(new ArrayList<>());
            }

            if(cdm.getActiveStatus() == null || cdm.getActiveStatus().equals(ActiveStatus.INACTIVE)) {
                cdm.setActiveStatus(ActiveStatus.INACTIVE);
                if(StringUtils.isBlank(cdm.getActiveStatusReason()) ||
                        !cdm.getActiveStatusReason().startsWith("Deactivated")) {
//                    cdm.setActiveStatusReason("Deactivated by "+SecurityContextUtils.getPrincipal()+
//                            " on "+ new DateToClientTimeZoneStringConverter().convert(new Date()));
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
                    com.salescode.dim.jooq.generated.tables.pojos.User admin= customerAccountsService.getAdminInfo();
                    if(!cdm.getLoginid().equalsIgnoreCase(admin.getLoginid())) {
                        HierarchyMetadataService hierarchyMetaDataService = (HierarchyMetadataService) ServiceLocator
                                .lookup(HierarchyMetadata.class);
                        Collection<HierarchyMetadata> adminMetaData = hierarchyMetaDataService
                                .findByImmediateParent(admin.getLoginid());
                        cdm.setImmediateParent(adminMetaData.stream().collect(Collectors.toList()));
                        String hierarchy = customerAccountsService.getAdminHierarchy(cdm.getLoginid());
                        cdm.setHierarchy(hierarchy);
                    }
                }
            }

            return new OperationResult.StepResult(OperationResult.Status.OK,"Data enriched successfully");
        }
        return new OperationResult.StepResult(OperationResult.Status.ERROR,"Enrichment error: User not found null");
    }

}
