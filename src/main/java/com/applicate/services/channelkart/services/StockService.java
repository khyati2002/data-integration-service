package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.repository.StockRepository;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.jooq.generated.tables.pojos.Stock;

import java.util.Map;

public class StockService extends AbstractCDMService<Stock> {

    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public Stock findBySkuCodeAndSupplier(String skuCode, String supplier) {
        return stockRepository.findBySkuCodeAndSupplier(skuCode, supplier).orElse(null);
    }

    public Stock save(Stock stock){
        return super.save(stock);
    }

}