package com.wnowakcraft.samples.restaurant.order.domain.model;

import com.wnowakcraft.samples.restaurant.core.domain.model.AbstractEvent;

import java.time.Instant;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;

public final class OrderCancelledEvent extends AbstractEvent<Order.Id> implements OrderEvent {

    public static OrderCancelledEvent restoreFrom(Order.Id orderId, SequenceNumber sequenceNumber, Instant generatedOn) {
        return new OrderCancelledEvent(orderId, sequenceNumber, generatedOn);
    }

    public OrderCancelledEvent(Order.Id orderId) {
        super(orderId);
    }

    private OrderCancelledEvent(Order.Id orderId, SequenceNumber sequenceNumber, Instant generatedOn) {
        super(orderId, sequenceNumber, generatedOn);
    }

    @Override
    public void applyOn(Order order) {
        requireNonNull(order, "order");

        order.apply(this);
    }
}
