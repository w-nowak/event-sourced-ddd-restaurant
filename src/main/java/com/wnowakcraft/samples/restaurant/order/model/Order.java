package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.AbstractAggregate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;

import static com.wnowakcraft.preconditions.Preconditions.*;
import static lombok.AccessLevel.PRIVATE;

@Getter
public class Order extends AbstractAggregate<Order.OrderId, OrderEvent, OrderSnapshot> {
    private static final String AGGREGATE_NAME = "ORDER";

    private RestaurantId restaurantId;
    private Status status;

    public static Order newOrder(CustomerId customerId, RestaurantId restaurantId) {
        requireNonNull(customerId, "customerId");
        requireNonNull(restaurantId, "restaurantId");

        final OrderId orderId = OrderId.newOrderId();
        final OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(orderId, customerId, restaurantId);

        final Order order = new Order(orderId, List.of(orderCreatedEvent));
        order.changes.add(orderCreatedEvent);

        return order;
    }

    public static Order restoreFrom(Collection<OrderEvent> events) {
        requireNonEmpty(events, "events");

        final OrderEvent firstOrderEvent = events.iterator().next();
        requireThat(firstOrderEvent instanceof OrderCreatedEvent,
                "Invalid event stream. Fist Order event needs to be OrderCreatedEvent");

        return new Order(firstOrderEvent.getConcernedAggregateId(), List.copyOf(events));
    }

    private Order(OrderId orderId, Collection<OrderEvent> orderEvents) {
        super(orderId, orderEvents);
    }

    @Override
    protected void applyAll(Collection<OrderEvent> orderEvents) {
        orderEvents.forEach(event -> event.applyOn(this));
    }

    @Override
    protected void restoreFrom(OrderSnapshot snapshot) {

    }

    //public void process(Collection<OrderEvent> orderEvents)

    public void cancel() {
        final var orderCanceledEvent = new OrderCanceledEvent(getAggregateId());
        changes.add(orderCanceledEvent);
        apply(orderCanceledEvent);
    }

    public void approve() {
        final var  orderApprovedEvent = new OrderApprovedEvent(getAggregateId());
        changes.add(orderApprovedEvent);
        apply(orderApprovedEvent);
    }

    void apply(OrderCreatedEvent orderCreatedEvent) {
        this.restaurantId = orderCreatedEvent.getRestaurantId();
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

    public static class OrderId extends AbstractAggregate.PrefixedUuidAggregateId {
        private OrderId() {
            super(OrderSubdomain.NAME, AGGREGATE_NAME);
        }

        private OrderId(String id) {
            super(OrderSubdomain.NAME, AGGREGATE_NAME, id);
        }

        public static OrderId newOrderId() {
            return new OrderId();
        }

        public static OrderId of(String id) {
            return new OrderId(id);
        }
    }

}


