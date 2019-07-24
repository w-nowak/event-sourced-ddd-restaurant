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

    public static final Order ORDER = Order.newOrder(CUSTOMER_ID, RESTAURANT_ID, THREE_ORDER_ITEMS);
    public static final Order.Version ORDER_VERSION = Aggregate.Version.of(10);
    public static final OrderCreatedEvent ORDER_CREATED_EVENT =
            new OrderCreatedEvent(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, THREE_ORDER_ITEMS);
    public static final OrderCancelStartedEvent ORDER_CANCEL_STARTED_EVENT = new OrderCancelStartedEvent(ORDER_ID);
    public static final OrderCancelledEvent ORDER_CANCELLED_EVENT = new OrderCancelledEvent(ORDER_ID);
    public static final OrderApprovedEvent ORDER_APPROVED_EVENT = new OrderApprovedEvent(ORDER_ID);
    public static final OrderRejectedEvent ORDER_REJECTED_EVENT = new OrderRejectedEvent(ORDER_ID);
    public static final OrderSnapshot ORDER_SNAPSHOT =
            OrderSnapshot.recreateFrom(ORDER_SNAPSHOT_ID, ORDER_ID, CREATION_DATE, AGGREGATE_VERSION,
                    CUSTOMER_ID, RESTAURANT_ID,Order.Status.APPROVED, THREE_ORDER_ITEMS);
}
