package com.applicate.services.channelkart.services;


import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.jooq.impl.GRNInfo;
import com.salescode.dim.jooq.impl.Sales;
import com.salescode.dim.jooq.impl.SalesDetails;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;





public class SalesService extends AbstractCDMService<Sales> {


	public static final String TAX_AMOUNT = "taxAmount";
	public static final String TAX_TYPE = "taxType";






	private static final String TAX_INFO = "taxInfo";





	private SalesGrnService salesGrnService = (SalesGrnService) ServiceLocator.lookup(GRNInfo.class);





	public Sales calculateCombinedSales(Sales salesPrev, Sales salesReturn) {
		updateSalesDetails(salesPrev, salesReturn);
		updateSalesAmounts(salesPrev, salesReturn);
		updateSalesQuantities(salesPrev, salesReturn);
		return salesPrev;
	}

	private void updateSalesAmounts(Sales salesPrev, Sales salesReturn) {
		salesGrnService.updateDoubleFields(salesPrev, salesReturn);
	}

	private void updateSalesQuantities(Sales salesPrev, Sales salesReturn) {
		salesPrev.setTotalInitialQuantity(salesGrnService.getFloatValueSafely(salesPrev.getTotalInitialQuantity()));
		Stream.of("TotalQuantity", "InitialNormalizedQuantity", "NormalizedQuantity")
				.forEach(field -> salesGrnService.updateField(salesPrev, salesReturn, field, salesGrnService::getFloatValueSafely, float.class));
	}

	private void updateSalesDetails(Sales salesPrev, Sales salesReturn) {
		// Create mapping between old and new sales details
		Map<SalesDetails, SalesDetails> salesDetailsMap = getSalesDetailsMap(salesReturn.getSalesDetails(), salesPrev.getSalesDetails());

		// Create reverse mapping for easier access
		Map<SalesDetails, SalesDetails> salesDetailsMapReversed = new HashMap<>();

		for(Map.Entry<SalesDetails, SalesDetails> sdm : salesDetailsMap.entrySet()) {
			salesDetailsMapReversed.putIfAbsent(sdm.getValue(), sdm.getKey());
		}

		// Use Set for more efficient lookups by batch code
		Set<String> modifiedBatchCodes = salesDetailsMap.keySet().stream()
				.map(sd -> (sd.getBatchId() + sd.getBatchCode()).toLowerCase())
				.collect(Collectors.toSet());

		// Filter unchanged details more efficiently
		List<SalesDetails> unchangedSalesDetails = salesPrev.getSalesDetails().stream()
				.filter(sd -> !modifiedBatchCodes.contains((sd.getBatchId() + sd.getBatchCode()).toLowerCase()))
				.collect(Collectors.toList());

		// Process modified details
		salesDetailsMapReversed.forEach(this::combineSalesDetails);

		// Combine unchanged and modified details
		List<SalesDetails> allSalesDetails = new ArrayList<>(unchangedSalesDetails);
		allSalesDetails.addAll(salesDetailsMapReversed.keySet());

		// Update sales with the final list
		salesPrev.setSalesDetails(allSalesDetails);
	}

    public Map<SalesDetails, SalesDetails> getSalesDetailsMap(List<SalesDetails> salesDetails, List<SalesDetails> salesDetailsDb) {
        Map<String, SalesDetails> salesDetailsDbMap = salesDetailsDb.stream()
                .collect(Collectors.toMap(s -> s.getBatchId()+s.getBatchCode(), Function.identity()));

        return salesDetails.stream()
                .map(details -> {
                    SalesDetails matchingDetails = salesDetailsDbMap.get(details.getBatchId()+details.getBatchCode());
                    return matchingDetails != null ? Map.entry(details, matchingDetails) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void combineSalesDetails(SalesDetails prevDetails, SalesDetails returnDetails) {
        updateTaxInfo(prevDetails, returnDetails);
        salesGrnService.updateFloatFields(prevDetails, returnDetails);
        salesGrnService.updateDoubleFields(prevDetails, returnDetails);
        prevDetails.setRemarks(returnDetails.getRemarks());
    }

    private void updateTaxInfo(SalesDetails prevDetails, SalesDetails returnDetails) {
        ObjectNode prevExtendedAttributes = (ObjectNode) prevDetails.getExtendedAttributes();
        ObjectNode returnExtendedAttributes = (ObjectNode) returnDetails.getExtendedAttributes();

        ArrayNode prevTaxInfo = (ArrayNode) Optional.ofNullable(prevExtendedAttributes.get(TAX_INFO))
                .orElse(JSONUtils.getObjectMapper().createArrayNode());
        ArrayNode returnTaxInfo = (ArrayNode) Optional.ofNullable(returnExtendedAttributes.get(TAX_INFO))
                .orElse(JSONUtils.getObjectMapper().createArrayNode());


        // Create a map with composite key of skuCode+taxType for prevTaxInfo
        Map<String, ObjectNode> prevTaxMap = new HashMap<>();
        for (int i = 0; i < prevTaxInfo.size(); i++) {
            ObjectNode taxNode = (ObjectNode) prevTaxInfo.get(i);
            String taxType = taxNode.get(TAX_TYPE).asText();
            prevTaxMap.put(taxType, taxNode);
        }

        // Create a map with composite key of skuCode+taxType for returnTaxInfo
        Map<String, ObjectNode> returnTaxMap = new HashMap<>();
        for (int i = 0; i < returnTaxInfo.size(); i++) {
            ObjectNode taxNode = (ObjectNode) returnTaxInfo.get(i);
            String taxType = taxNode.get(TAX_TYPE).asText();
            returnTaxMap.put(taxType, taxNode);
        }

        // Update prevTaxInfo nodes by adding return tax amounts
        // Iterate over entrySet instead of keySet
        for (Map.Entry<String, ObjectNode> entry : returnTaxMap.entrySet()) {
            String key = entry.getKey();
            ObjectNode returnNode = entry.getValue();

            if (prevTaxMap.containsKey(key)) {
                // If same skuCode and taxType exists in prevTaxInfo, add the tax amounts
                ObjectNode prevNode = prevTaxMap.get(key);

                double prevAmount = prevNode.get(TAX_AMOUNT).asDouble();
                double returnAmount = returnNode.get(TAX_AMOUNT).asDouble();

                // Update the tax amount in prevNode
                prevNode.put(TAX_AMOUNT, prevAmount + returnAmount);
            } else {
                // If the skuCode+taxType combination doesn't exist in prevTaxInfo, add it
                prevTaxInfo.add(returnNode);
            }
        }

        // Update the taxInfo in prevExtendedAttributes
        prevExtendedAttributes.set(TAX_INFO, prevTaxInfo);
        prevDetails.setExtendedAttributes(prevExtendedAttributes);
    }



}
