package com.applicate.services.channelkart.services;

import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.repository.StockRepository;
import com.applicate.services.channelkart.utils.IdGenerator;
import com.salescode.dim.jooq.generated.tables.records.CkStockRecord;
import com.salescode.dim.jooq.impl.Stock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_STOCK;

public class StockService extends AbstractCDMService<Stock> {

    private static final Logger LOG = LoggerFactory.getLogger(StockService.class);
    private static StockRepository stockRepository;

    public StockService() {
        if (stockRepository == null) {
            stockRepository = new StockRepository(getDslContext());
        }
    }

    public List<List<Stock>> getItemsToSaveList(List<Stock> stockList) {
        List<List<Stock>> result = new ArrayList<>();

        List<String> stockIds = stockList.stream()
                .map(Stock::getId)
                .collect(Collectors.toList());

        Map<String, Stock> savedList = getDslContext()
                .selectFrom(CK_STOCK)
                .where(CK_STOCK.ID.in(stockIds))
                .fetch()
                .intoMap(CK_STOCK.ID, this::convertToStock);

        List<Stock> itemsToInsert = new ArrayList<>();
        List<Stock> itemsToUpdate = new ArrayList<>();

        for (Stock stock : stockList) {
            fillAttributes(stock, savedList.get(stock.getId()));
            fillCommonAttributes(stock);

            if (stock.getId() == null) {
                stock.setId(new IdGenerator(stock.getClass().getSimpleName()).getId(stock));
            }

            if (savedList.get(stock.getId()) == null) {
                itemsToInsert.add(stock);
                stock.setOperationPerformed(ActionType.INSERT);
                stock.setActiveStatus(ActiveStatus.ACTIVE);
                stock.setChanged(true);
            } else {
                stock.setOperationPerformed(ActionType.UPDATE);
                stock.setActiveStatus(ActiveStatus.ACTIVE);
                stock.setChanged(true);
                itemsToUpdate.add(stock);
            }
        }

        result.add(itemsToInsert);
        result.add(itemsToUpdate);
        return result;
    }

    private Stock convertToStock(CkStockRecord stockRecord) {
        Stock stock = new Stock();
        stock.setId(stockRecord.getId());
        stock.setChanged(true);
        stock.setActiveStatus(stockRecord.getActiveStatus());
        return stock;
    }

    @Override
    public Collection<Stock> batchSave(Collection<Stock> stockList) {
        LOG.info("Size of list is {}", stockList.size());

        List<List<Stock>> itemsToSaveList = getItemsToSaveList(new ArrayList<>(stockList));

        if (!itemsToSaveList.get(0).isEmpty()) {
            getDslContext().batchInsert(itemsToSaveList.get(0).stream()
                    .map(stock -> getDslContext().newRecord(CK_STOCK, stock))
                    .collect(Collectors.toList())).execute();
        }

        if (!itemsToSaveList.get(1).isEmpty()) {
            getDslContext().batchUpdate(itemsToSaveList.get(1).stream()
                    .map(stock -> getDslContext().newRecord(CK_STOCK, stock))
                    .collect(Collectors.toList())).execute();
        }

        LOG.info("Batch save for stock is successful");
        return stockList;
    }

    public List<Stock> findbySkuCodeSkuCodeAndSupplierLoginId(List<String> skuCode, String supplier) {
        return stockRepository.findBySkuCodeInAndSupplier(skuCode, supplier);
    }
}
