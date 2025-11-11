package com.applicate.services.channelkart.services;
import com.applicate.services.channelkart.client.properties.PropertyDefinition;
import com.applicate.services.channelkart.client.properties.PropertyRegistry;
import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.utils.CdmDiffUtil;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.IdGenerator;
import com.salescode.dim.jooq.generated.tables.pojos.Stock;
import com.salescode.dim.jooq.generated.tables.records.CkStockRecord;


import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_STOCK;

public class StockService extends AbstractCDMService<Stock> {

    protected EntityUtils entityUtils;
    
    @Override
    public Collection<Stock> batchSave(Collection<Stock> stockList){
        List<Stock> stocks = new ArrayList<>(stockList);
        IdGenerator generator = new IdGenerator(stocks.get(0).getClass().getSimpleName());
        for(Stock item : stockList){
            item.setId(generator.getId(item));
        }
        List<List<Stock>> saveItemsList = getItemsToSaveList(stocks);
        if (!saveItemsList.get(0).isEmpty()) {
            getDslContext().batchInsert(
                    saveItemsList.get(0).stream()
                            .map(outlet -> getDslContext().newRecord(CK_STOCK, outlet))
                            .collect(Collectors.toList())
            ).execute();
        }
        if (!saveItemsList.get(1).isEmpty()) {
            getDslContext().batchUpdate(
                    saveItemsList.get(1).stream()
                            .map(outlet -> {
                                CkStockRecord record = getDslContext().newRecord(CK_STOCK, outlet);
                                //                          record.changed(CK_USER.ID, false); // Avoid updating primary key
                                return record;
                            })
                            .collect(Collectors.toList())
            ).execute();
        }
        return stockList;
    }

    public List<List<Stock>> getItemsToSaveList(List<Stock> stockList){
        List<List<Stock>> result = new ArrayList<>();
        List<String> outletCodes = stockList.stream()
                .map(Stock::getId)
                .collect(Collectors.toList());

        Map<String, com.salescode.dim.jooq.generated.tables.pojos.Stock> savedList = getDslContext()
                .select(CK_STOCK.asterisk())
                .from(CK_STOCK)
                .where(CK_STOCK.ID.in(outletCodes))
                .fetch()
                .intoMap(CK_STOCK.ID, record -> record.into(com.salescode.dim.jooq.generated.tables.pojos.Stock.class));
        List<Stock> itemsToInsert = new ArrayList<>();
        List<Stock> itemsToUpdate = new ArrayList<>();
        for (Stock outlet : stockList) {
            fillAttributes(outlet,savedList.get(outlet.getId()));
            fillCommonAttributes(outlet);
            new AttributeUpdateOverrideManager().overrideAttributes(outlet,savedList.get(outlet.getId()));
            super.addHash(outlet);
            if (savedList.get(outlet.getOutletcode()) == null) {
                outlet.setVersion(0);
                outlet.setId(UUID.randomUUID().toString());
                outlet.setChanged(true);
                itemsToInsert.add(outlet);
                outlet.setOperationPerformed(ActionType.INSERT);
            } else {
                Stock existingOutlet = savedList.get(outlet.getId());
                outlet.setId(existingOutlet.getId());
                outlet.setVersion(existingOutlet.getVersion() + 1);

                if (!Objects.equals(outlet.getHash(), existingOutlet.getHash())) {
                    outlet.setChanges(CdmDiffUtil.getChanges(outlet,existingOutlet));
                    outlet.setOperationPerformed(ActionType.UPDATE);
                    outlet.setChanged(true);
                    itemsToUpdate.add(outlet);
                }
            }
        }
        result.add(itemsToInsert);
        result.add(itemsToUpdate);
        return result;
    }

    public void deductAndAddStock(Map<String, Map<String, Double>> suppBatCodQty, boolean flag) {
        List<Stock> stocksToSaved = new ArrayList<>();
        Set<String> supSet = suppBatCodQty.keySet();
        for (String supplier : supSet) {
            Map<String, Double> batQty = suppBatCodQty.get(supplier);
            List<String> batchCode = new ArrayList<>(suppBatCodQty.get(supplier).keySet());
            List<Stock> stocks = getDslContext().selectFrom(CK_STOCK)
                                         .where(CK_STOCK.BATCH_CODE.in(batchCode))
                                         .and(CK_STOCK.SUPPLIER.eq(supplier))
                                         .forUpdate()
                                         .fetchInto(Stock.class);
            processDeductAndAddStock(stocks,batchCode,batQty,flag,stocksToSaved);

        }
        if(stocksToSaved.isEmpty()) {
            return;
        }
        batchSave(stocksToSaved);
    }


    private void processDeductAndAddStock(List<Stock> stocks, List<String> batchCode, Map<String, Double> batQty, boolean flag, List<Stock> stocksToSaved) {
        Map<String, Stock> stockMap = stocks.stream().collect(Collectors.toMap(Stock::getBatchCode, s -> s));
        boolean allowNegativeStock = PropertyRegistry.getAsBoolean(PropertyDefinition.ALLOW_NEGETIVE_STOCK);
        batchCode.forEach(b -> {
            Stock s = stockMap.get(b);
            if (s == null) {
                return;
            }
            if (flag) {
                Double curBatQty = batQty.get(s.getBatchCode());
                if(curBatQty < 0) {
                    curBatQty = (curBatQty * (-1.0));
                }
                s.setQty((s.getQty() + curBatQty));
                stocksToSaved.add(s);
            } else {
                if ((s.getQty() - batQty.get(s.getBatchCode())) < 0 && !allowNegativeStock){
                    s.setQty(0.0);
                    return;
                }
                s.setQty((s.getQty() - batQty.get(s.getBatchCode())));
                stocksToSaved.add(s);
            }
        });
    }
    
}

