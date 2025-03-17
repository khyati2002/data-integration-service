package com.salescode.dim.bundle.enrichment;
import com.applicate.services.channelkart.utils.NullUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeDefination;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeOutletBifurcations;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
This class enriches the data into channel column from the mapping present in generic object.
 We get the ChannelId in the sheet and enrich the  channel according to mapped channelName in the key3 of generic object.
*/

public class HCCBChannelEnrichment extends AbstractEnrichment<SchemeDefination> {

    Logger logger = LoggerFactory.getLogger(HCCBChannelEnrichment.class);
// findByBatchCode
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
                        List<GenericEntity> ge = genericEntityService.readModelsByNameAndKey1AndKey2("channel", "Channel", channelId);
                        if (ObjectUtils.isNotEmpty(ge)) {
                            GenericEntity geobject=ge.get(0);
                            outletBifurcation.setChannel(geobject.getKey3());
                        }else {
//                            If Channel not found in generic object for then keep the channel as value
                            outletBifurcation.setChannel(channelId);
                        }
                    }
                }
            }
            enrichItemSchemeDescription(cdm);
            logger.info("Time taken to apply schemes : {}", System.currentTimeMillis() - currentTime);
        }catch (Exception ex){
            throw new CustomRuntimeException(ex.getMessage());
        }
        return EnrichmentResult.OK;

    }

    private boolean checkIfEmpty(List<SchemeOutletBifurcations> allOutletBifurcationDetails) {
        return NullUtils.isNotNull(allOutletBifurcationDetails) && !allOutletBifurcationDetails.isEmpty();
    }

    private void enrichItemSchemeDescription(SchemeDefination cdm) {
            if(cdm.getSchemeType().contains("item") && ObjectUtils.isNotEmpty(cdm.getSchemeCalculation().getSchemeDiscountedProductcode())){
                ProductDetails pd = productDetailsService.findByBatchCode(cdm.getSchemeCalculation().getSchemeDiscountedProductcode(),false);
                String name = pd.getSkuDescription();
                String newDes= cdm.getSchemeDescription() + " (" + name +")";
                cdm.setSchemeDescription(newDes);
                updateSlabDescription(cdm.getSchemeCalculation().getSlabInfo(),name);
            }
        }

    private void updateSlabDescription(ArrayNode slabInfo, String name) {
        for (JsonNode node : slabInfo) {
                String newDes = node.get("schemeDescription").asText() + "(" + name +")";
                ObjectNode objectNode = (ObjectNode) node;
                objectNode.put("schemeDescription", newDes);
        }
    }


}
