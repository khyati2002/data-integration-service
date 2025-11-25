package com.applicate.services.channelkart.services;

import com.salescode.dim.jooq.generated.tables.pojos.Orders;

import static com.salescode.dim.jooq.generated.Tables.CK_ORDERS;

public class OrderService extends AbstractCDMService<Orders> {


    public Orders findFullLoadedOrder(String orderNumber){
        return getDslContext().select(CK_ORDERS.asterisk()).where(CK_ORDERS.ORDER_NUMBER.eq(orderNumber)).fetchInto(Orders.class).get(0);
    }
}
