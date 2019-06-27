package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.AbstractAggregate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static com.wnowakcraft.preconditions.Preconditions.*;
import static lombok.AccessLevel.PRIVATE;

@Getter
public class Order extends AbstractAggregate<Order.OrderId, OrderEvent, OrderSnapshot> {
    static final String AGGREGATE_NAME = "ORDER";

    private RestaurantId restaurantId;
    private Status status;
    private Collection<OrderItem> orderItems = new LinkedList<>();

    public static Order newOrder(CustomerId customerId, RestaurantId restaurantId, Collection<OrderItem> orderItems) {
        final OrderId orderId = OrderId.newOrderId();
        final OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(orderId, customerId, restaurantId, orderItems);

        final Order order = new Order(orderId, List.of(orderCreatedEvent), Version.NONE);
        order.changes.add(orderCreatedEvent);

        return order;
    }

    public static Order restoreFrom(Collection<OrderEvent> events, Version version) {
        requireNonEmpty(events, "events");

        final OrderEvent firstOrderEvent = events.iterator().next();
        requireThat(firstOrderEvent instanceof OrderCreatedEvent,
                "Invalid event stream. Fist Order event needs to be OrderCreatedEvent");

        return new Order(firstOrderEvent.getConcernedAggregateId(), List.copyOf(events), version);
    }

    public static Order restoreFrom(OrderSnapshot orderSnapshot, Collection<OrderEvent> events, Version version) {
        return new Order(orderSnapshot, List.copyOf(events), version);
    }

    private Order(OrderId orderId, Collection<OrderEvent> orderEvents, Version version) {
        super(orderId, orderEvents, version);
    }

    private Order(OrderSnapshot orderSnapshot, Collection<OrderEvent> orderEvents, Version version) {
        super(orderSnapshot.getAggregateId(), orderSnapshot, orderEvents, version);
    }

    @Override
    protected void applyAll(Collection<OrderEvent> orderEvents) {
        orderEvents.forEach(event -> event.applyOn(this));
    }

    @Override
    protected void restoreFrom(OrderSnapshot orderSnapshot) {
        this.restaurantId = orderSnapshot.getRestaurantId();
        this.status = orderSnapshot.getOrderStatus();
    }

    public void cancel() {
        final var orderCanceledEvent = new OrderCanceledEvent(getId());
        changes.add(orderCanceledEvent);
        apply(orderCanceledEvent);
    }

    public void approve() {
        final var  orderApprovedEvent = new OrderApprovedEvent(getId());
        changes.add(orderApprovedEvent);
        apply(orderApprovedEvent);
    }

    public OrderSnapshot takeSnapshot() {
        return OrderSnapshot.newSnapshot(getId(), restaurantId, status);
    }

    void apply(OrderCreatedEvent orderCreatedEvent) {
        this.restaurantId = orderCreatedEvent.getRestaurantId();
        this.orderItems = orderCreatedEvent.getOrderItems();
        this.status = Status.APPROVAL_PENDING;
    }

    void apply(OrderApprovedEvent orderApprovedEvent) {
        this.status = Status.APPROVED;
    }

    void apply(OrderCanceledEvent orderCanceledEvent) {
        this.status = Status.CANCELLED;
    }


    @Getter
    @RequiredArgsConstructor(access = PRIVATE)
    public enum Status {
        APPROVAL_PENDING("APPROVAL_PENDING"),
        APPROVED("APPROVED"),
        REJECTED("REJECTED"),
        CANCEL_PENDING("CANCEL_PENDING"),
        CANCELLED("CANCELED");

        private final String name;
    }

    public static final class OrderId extends AggregateId {
        private OrderId() {
            super(OrderSubdomain.NAME, AGGREGATE_NAME);
        }

        private OrderId(String id) {
            super(id, OrderSubdomain.NAME, AGGREGATE_NAME);
        }

        private static OrderId newOrderId() {
            return new OrderId();
        }

        public static OrderId of(String id) {
            return new OrderId(id);
        }
    }

}


