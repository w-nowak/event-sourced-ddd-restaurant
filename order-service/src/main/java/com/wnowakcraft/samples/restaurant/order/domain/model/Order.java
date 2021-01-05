package com.wnowakcraft.samples.restaurant.order.domain.model;

import com.wnowakcraft.samples.restaurant.core.domain.model.AbstractAggregate;
import com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;
import static com.wnowakcraft.preconditions.Preconditions.requireThat;
import static java.lang.String.format;
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
        return new Order(orderCreatedEvent);
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

    public void reject() {
        if(this.status == Status.REJECTED) {
            return;
        }

        requireThat(this.status == Status.APPROVAL_PENDING,
                () -> new IllegalStateChangeException(this, this.status, Status.REJECTED,
                        format("Order cannot be rejected when in %s status", this.status)));

        final var orderRejectedEvent = new OrderRejectedEvent(getId());
        changes.add(orderRejectedEvent);
        apply(orderRejectedEvent);
    }

    public void cancel() {
        if(this.status == Status.CANCELLED || this.status == Status.CANCEL_PENDING) {
            return;
        }

        requireThat(this.status == Status.APPROVED,
                () -> new IllegalStateChangeException(this, this.status, Status.CANCEL_PENDING,
                        "Order cannot be cancelled as it hasn't been approved yet"));

        final var orderCancelStartededEvent = new OrderCancelStartedEvent(getId());
        changes.add(orderCancelStartededEvent);
        apply(orderCancelStartededEvent);
    }

    public void cancelConfirmed() {
        if(this.status == Status.CANCELLED) {
            return;
        }

        requireThat(this.status == Status.CANCEL_PENDING,
                () -> new IllegalStateChangeException(this, this.status, Status.CANCELLED,
                        format("Order cancellation status cannot be confirmed when in %s status", this.status))
        );

        final var orderCanceledEvent = new OrderCancelledEvent(getId());
        changes.add(orderCanceledEvent);
        apply(orderCanceledEvent);
    }

    public void approve() {
        if(this.status == Status.APPROVED) {
            return;
        }

        requireThat(this.status == Status.APPROVAL_PENDING,
                () -> new IllegalStateChangeException(this, this.status, Status.APPROVED,
                        format("Order cannot be approved when in %s status", this.status))
        );

        final var  orderApprovedEvent = new OrderApprovedEvent(getId());
        changes.add(orderApprovedEvent);
        apply(orderApprovedEvent);
    }

    @Override
    public OrderSnapshot takeSnapshot() {
        return OrderSnapshot.newSnapshot(getId(), getVersion(), customerId, restaurantId, status, orderItems);
    }

    void apply(OrderCreatedEvent orderCreatedEvent) {
        this.customerId = orderCreatedEvent.getCustomerId();
        this.restaurantId = orderCreatedEvent.getRestaurantId();
        this.orderItems = orderCreatedEvent.getOrderItems();
        this.status = Status.APPROVAL_PENDING;
    }

    void apply(OrderApprovedEvent orderApprovedEvent) {
        this.status = Status.APPROVED;
    }

    void apply(OrderCancelStartedEvent orderCancelStartedEvent) {
        this.status = Status.CANCEL_PENDING;
    }

    void apply(OrderCancelledEvent orderCancelledEvent) {
        this.status = Status.CANCELLED;
    }

    void apply(OrderRejectedEvent orderRejectedEvent) {
        this.status = Status.REJECTED;
    }

    @Getter
    @RequiredArgsConstructor(access = PRIVATE)
    public enum Status implements State {
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


