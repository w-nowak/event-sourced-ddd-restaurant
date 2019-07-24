package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.AbstractEvent;

import java.time.Instant;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;

public final class OrderRejectedEvent extends AbstractEvent<Order.Id> implements OrderEvent {

    public static OrderRejectedEvent restoreFrom(Order.Id orderId, SequenceNumber sequenceNumber, Instant generatedOn) {
        return new OrderRejectedEvent(orderId, sequenceNumber, generatedOn);
    }

    public OrderRejectedEvent(Order.Id orderId) {
        super(orderId);
    }

    private OrderRejectedEvent(Order.Id orderId, SequenceNumber sequenceNumber, Instant generatedOn) {
        super(orderId, sequenceNumber, generatedOn);
    }

    @Override
    public void applyOn(Order order) {
        requireNonNull(order, "order");

        order.apply(this);
    }
}
