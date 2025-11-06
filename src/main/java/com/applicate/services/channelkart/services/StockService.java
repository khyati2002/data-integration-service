package com.applicate.services.channelkart.services;


import com.applicate.services.channelkart.exceptions.checked.ConfigurationException;
import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.utils.CdmDiffUtil;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.IdGenerator;
import com.salescode.dim.jooq.generated.tables.pojos.Stock;
import com.salescode.dim.jooq.generated.tables.records.CkStockRecord;


import java.util.*;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_OUTLET_DETAILS;
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

                String outlethash = outlet.getHash();
                String existingHash = existingOutlet.getHash();

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
    
}

