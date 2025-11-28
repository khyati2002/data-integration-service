package com.applicate.services.channelkart.enrichments.repository;

import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.applicate.services.channelkart.utils.StringUtils;
import com.applicate.services.channelkart.validations.ValidationResponseMessage;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.Stock;

import java.util.List;
import java.util.Map;

public class StockEncrichment extends AbstractEnrichment<Stock> {

	private static final String PRODUCT_FIND_BY_CODE="select batch_code,category,brand,sku_description from ck_productdetails where sku_code='{}';";

	@Override
	public EnrichmentResult apply(Stock stock) {

		StringBuilder enrichResult = new StringBuilder();
		if(NullUtils.isNull(stock.getBatchCode()))
		{
//			EntityUtils entityUtils=(EntityUtils) ServiceLocator.lookup(Entity.class);
			List<Map<String,Object>> result= (List<Map<String,Object>>) EntityUtils.getInstance().findDataByQuery(Map.class, StringUtils.format(PRODUCT_FIND_BY_CODE, stock.getSkuCode()), true);
			Map<String,Object> product=result.isEmpty()?null:result.stream().findFirst().get();
			if(NullUtils.isNotNull(product)){ 
				stock.setBatchCode(product.get("batch_code").toString());
				stock.setSkuDesc(product.get("sku_description").toString());
				stock.setCat(product.get("category").toString());
				stock.setBrand(product.get("brand").toString());
			}
			else
			{
				enrichResult.append(ValidationResponseMessage.SKU_NOT_FOUND);
			}
		}

		if(enrichResult.toString().isEmpty())
		{
            return new OperationResult.StepResult(OperationResult.Status.OK, "Data enriched successfully");

		}
        return new OperationResult.StepResult(OperationResult.Status.ERROR, enrichResult.toString().substring(0, enrichResult.toString().length()));

	}

}
