package com.applicate.services.channelkart.services;
import  com.salescode.dim.jooq.impl.*;
import com.applicate.services.channelkart.models.enums.ActionType;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.utils.IdGenerator;
import com.salescode.dim.jooq.generated.tables.records.CkOrdersRecord;
import com.salescode.dim.jooq.impl.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.salescode.dim.jooq.generated.Tables.CK_ORDERS;

public class OrdersService extends AbstractCDMService<Order> {
    private static final Logger LOG = LoggerFactory.getLogger(OrdersService.class);

    public List<List<Order>> getItemsToSaveList(List<Order> OrdersList) {
        List<List<Order>> result = new ArrayList<>();
        List<String> ordersIds = OrdersList.stream()
                .map(Order::getId)
                .collect(Collectors.toList());

        Map<String, Order> savedList = getDslContext()
                .selectFrom(CK_ORDERS)
                .where(CK_ORDERS.ID.in(ordersIds))
                .fetch()
                .intoMap(CK_ORDERS.ID, this::convertToOrders);

        List<Order> itemsToInsert = new ArrayList<>();
        List<Order> itemsToUpdate = new ArrayList<>();

        for (Order order : OrdersList) {
            fillAttributes(order, savedList.get(order.getId()));

            if (savedList.get(order.getId()) == null) {

                if (order.getId() == null) {
                    order.setId(new IdGenerator(order.getClass().getSimpleName()).getId(order));
                }

                fillCommonAttributes(order);
                itemsToInsert.add(order);
                order.setOperationPerformed(ActionType.INSERT);
                order.setActiveStatus(ActiveStatus.ACTIVE);
                order.setChanged(true);
            } else {
                fillCommonAttributesForUpdate(savedList.get(order.getId()),order);
                order.setOperationPerformed(ActionType.UPDATE);
                order.setChanged(true);
                itemsToUpdate.add(order);
            }
        }

        result.add(itemsToInsert);
        result.add(itemsToUpdate);
        return result;
    }

    public void fillCommonAttributesForUpdate(Order orderDb, Order order){
        if(orderDb.getCreationTime() == null){
            order.setCreationTime(LocalDateTime.now(ZoneOffset.UTC));
        }

        order.setLastModifiedTime(LocalDateTime.now(ZoneOffset.UTC));

    }

    private Order convertToOrders(CkOrdersRecord ckOrderRecord) {
        Order order = new Order();
        order.setId(ckOrderRecord.getId());
        order.setChanged(true);
        order.setActiveStatus(ckOrderRecord.getActiveStatus());
        order.setCreationTime(ckOrderRecord.getCreationTime());
        return order;
    }

    @Override
    public Collection<Order> batchSave(Collection<Order> OrdersList) {
        LOG.info("Size of list is {}", OrdersList.size());

        List<List<Order>> saveItemsList = getItemsToSaveList(new ArrayList<>(OrdersList));

        if (!saveItemsList.get(0).isEmpty()) {
            getDslContext().batchInsert(saveItemsList.get(0).stream()
                    .map(order -> getDslContext().newRecord(CK_ORDERS, order))
                    .collect(Collectors.toList())).execute();
        }

        if (!saveItemsList.get(1).isEmpty()) {
            getDslContext().batchUpdate(saveItemsList.get(1).stream()
                    .map(order -> getDslContext().newRecord(CK_ORDERS, order))
                    .collect(Collectors.toList())).execute();
        }

        LOG.info("Batch save for orders is successful");
        return OrdersList;
    }
}