package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.AbstractAggregate;
import com.wnowakcraft.samples.restaurant.core.domain.Aggregate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;
import static lombok.AccessLevel.PRIVATE;

@Getter
public class Order extends AbstractAggregate<Order.Id, OrderEvent, OrderSnapshot> {
    static final String AGGREGATE_NAME = "ORDER";

    private CustomerId customerId;
    private RestaurantId restaurantId;
    private Status status;
    private Collection<OrderItem> orderItems;

    public static Order newOrder(CustomerId customerId, RestaurantId restaurantId, Collection<OrderItem> orderItems) {
        final var orderCreatedEvent = new OrderCreatedEvent(Order.Id.newId(), customerId, restaurantId, orderItems);

        final Order order = new Order(orderCreatedEvent);

        return order;
    }

    public static Order restoreFrom(Collection<? extends OrderEvent> events, Version version) {
        return new Order(events, version);
    }

    public static Order restoreFrom(OrderSnapshot orderSnapshot, Collection<? extends OrderEvent> events, Version version) {
        return new Order(orderSnapshot, events, version);
    }

    private Order(OrderCreatedEvent orderCreatedEvent) {
        super(orderCreatedEvent);
    }

    private Order(Collection<? extends OrderEvent> orderEvents, Version version) {
        super(orderEvents, OrderCreatedEvent.class, version);
    }

    private Order(OrderSnapshot orderSnapshot, Collection<? extends OrderEvent> orderEvents, Version version) {
        super(orderSnapshot, orderEvents, version);
    }

    @Override
    protected void applyAll(Collection<OrderEvent> orderEvents) {
        requireNonNull(orderEvents, "orderEvents");

        orderEvents.forEach(event -> event.applyOn(this));
    }

    @Override
    protected void restoreFrom(OrderSnapshot orderSnapshot) {
        requireNonNull(orderSnapshot, "orderSnapshot");

        this.customerId = orderSnapshot.getCustomerId();
        this.restaurantId = orderSnapshot.getRestaurantId();
        this.status = orderSnapshot.getOrderStatus();
        this.orderItems = orderSnapshot.getOrderItems();
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
        return OrderSnapshot.newSnapshot(getId(), customerId, restaurantId, status, orderItems);
    }

    void apply(OrderCreatedEvent orderCreatedEvent) {
        requireNonNull(orderCreatedEvent, "orderCreatedEvent");

        this.customerId = orderCreatedEvent.getCustomerId();
        this.restaurantId = orderCreatedEvent.getRestaurantId();
        this.orderItems = orderCreatedEvent.getOrderItems();
        this.status = Status.APPROVAL_PENDING;
    }

    void apply(OrderApprovedEvent orderApprovedEvent) {
        requireNonNull(orderApprovedEvent, "orderApprovedEvent");

        this.status = Status.APPROVED;
    }

    void apply(OrderCanceledEvent orderCanceledEvent) {
        requireNonNull(orderCanceledEvent, "orderCanceledEvent");

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

    public static final class Id extends Aggregate.Id {
        private Id() {
            super(OrderSubdomain.NAME, AGGREGATE_NAME);
        }

        private Id(String id) {
            super(id, OrderSubdomain.NAME, AGGREGATE_NAME);
        }

        private static Id newId() {
            return new Id();
        }

        public static Id of(String id) {
            return new Id(id);
        }
    }

}


