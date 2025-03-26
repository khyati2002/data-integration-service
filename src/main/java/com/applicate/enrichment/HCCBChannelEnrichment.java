package com.applicate.enrichment;
import com.applicate.services.channelkart.services.GenericObjectService;
import com.applicate.services.channelkart.utils.NullUtils;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.GenericObject;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeOutletBifurcations;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.salescode.dim.jooq.impl.SchemeDefination;


import java.util.List;

/**
This class enriches the data into channel column from the mapping present in generic object.
 We get the ChannelId in the sheet and enrich the  channel according to mapped channelName in the key3 of generic object.
*/

public class HCCBChannelEnrichment extends AbstractEnrichment<SchemeDefination> {

    private static GenericObjectService genericObjectService;

    public HCCBChannelEnrichment() {
        genericObjectService = new GenericObjectService();
    }
    Logger logger = LoggerFactory.getLogger(HCCBChannelEnrichment.class);
    @Override
    public EnrichmentResult apply(SchemeDefination cdm) {
        if(ObjectUtils.isNotEmpty(cdm.getProgramLevel()) && "pricingCondition".equalsIgnoreCase(cdm.getProgramLevel())){
            return new OperationResult.StepResult(OperationResult.Status.OK,"Data enriched successfully");
        }
        try {
            List<SchemeOutletBifurcations> allOutletBifurcationDetails = cdm.getSchemeOutletBifurcationsList();
            long currentTime = System.currentTimeMillis();

            if (checkIfEmpty(allOutletBifurcationDetails)) {
                for (SchemeOutletBifurcations outletBifurcation : allOutletBifurcationDetails) {
                    if (outletBifurcation.getChannel() != null && !outletBifurcation.getChannel().isEmpty() && !outletBifurcation.getChannel().equalsIgnoreCase("all")){
                        String channelId=outletBifurcation.getChannel();
                        GenericObject ge = genericObjectService.fetchByValue("channel", channelId);
                        if (ObjectUtils.isNotEmpty(ge)) {
                            GenericObject geobject=ge;
                            outletBifurcation.setChannel(geobject.getKey3());
                        }else {
//                            If Channel not found in generic object for then keep the channel as value
                            outletBifurcation.setChannel(channelId);
                        }
                    }
                }
            }
            logger.info("Time taken for channel enrichment : {}", System.currentTimeMillis() - currentTime);
        }catch (Exception ex){
            throw new RuntimeException("Exception in channel enrichment {}", ex);
        }
        return new OperationResult.StepResult(OperationResult.Status.OK,"Data enriched successfully");
    }

    private boolean checkIfEmpty(List<SchemeOutletBifurcations> allOutletBifurcationDetails) {
        return NullUtils.isNotNull(allOutletBifurcationDetails) && !allOutletBifurcationDetails.isEmpty();
    }
}
