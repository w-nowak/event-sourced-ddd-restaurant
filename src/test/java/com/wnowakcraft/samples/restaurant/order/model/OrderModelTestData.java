package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.Aggregate;
import com.wnowakcraft.samples.restaurant.core.domain.Event;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class OrderModelTestData {
    public static final Order.Id ORDER_ID = Order.Id.of("ORDER-ORDER-A-3df56c04-0bf9-4caa");
    public static final MenuItemId MENU_ITEM_ID = MenuItemId.of("RESTAURANT-MENU_ITEM-A-3df56c04-0bf9-4caa");
    public static final CustomerId CUSTOMER_ID = CustomerId.of("CUSTOMER-CUSTOMER-A-3df56c04-0bf9-4caa");
    public static final RestaurantId RESTAURANT_ID = RestaurantId.of("RESTAURANT-RESTAURANT-A-3df56c04-0bf9-4caa");
    public static final OrderSnapshot.Id ORDER_SNAPSHOT_ID = OrderSnapshot.Id.of("ORDER-ORDER-S-29ef61dc-651c-4b87");
    public static final Instant CREATION_DATE = Instant.now();
    public static final Aggregate.Version AGGREGATE_VERSION = Aggregate.Version.of(741258);
    public static final Event.SequenceNumber SEQUENCE_NUMBER = Event.SequenceNumber.of(5);

    @NoArgsConstructor(access = PRIVATE)
    public static class ORDER_ITEM_1 {
        public static final int QUANTITY = 3;
        public static final String NAME = "ORDER ITEM 1 NAME";
        public static final MenuItemId MENU_ITEM_ID = MenuItemId.of("RESTAURANT-MENU_ITEM-A-3df56c04-0bf9-4caa");
        public static final OrderItem ORDER_ITEM = new OrderItem(QUANTITY, NAME, MENU_ITEM_ID);
    }

    @NoArgsConstructor(access = PRIVATE)
    public static class ORDER_ITEM_2 {
        public static final int QUANTITY = 5;
        public static final String NAME = "ORDER ITEM 2 NAME";
        public static final MenuItemId MENU_ITEM_ID = MenuItemId.of("RESTAURANT-MENU_ITEM-A-fa76cd4b-96fc-49a0");
        public static final OrderItem ORDER_ITEM = new OrderItem(QUANTITY, NAME, MENU_ITEM_ID);
    }

    @NoArgsConstructor(access = PRIVATE)
    public static class ORDER_ITEM_3 {
        public static final int QUANTITY = 12;
        public static final String NAME = "ORDER ITEM 3 NAME";
        public static final MenuItemId MENU_ITEM_ID = MenuItemId.of("RESTAURANT-MENU_ITEM-A-c2291e2e-765d-4aed");
        public static final OrderItem ORDER_ITEM = new OrderItem(QUANTITY, NAME, MENU_ITEM_ID);
    }

    public static final Collection<OrderItem> THREE_ORDER_ITEMS =
            List.of(ORDER_ITEM_1.ORDER_ITEM, ORDER_ITEM_2.ORDER_ITEM, ORDER_ITEM_3.ORDER_ITEM);
}
