package com.wnowakcraft.samples.restaurant.order.domain.model;

import com.wnowakcraft.samples.restaurant.core.domain.model.AbstractEvent;

import java.time.Instant;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;

public final class OrderCancelStartedEvent extends AbstractEvent<Order.Id> implements OrderEvent {

    public static OrderCancelStartedEvent restoreFrom(Order.Id orderId, SequenceNumber sequenceNumber, Instant generatedOn) {
        return new OrderCancelStartedEvent(orderId, sequenceNumber, generatedOn);
    }

    public OrderCancelStartedEvent(Order.Id orderId) {
        super(orderId);
    }

    private OrderCancelStartedEvent(Order.Id orderId, SequenceNumber sequenceNumber, Instant generatedOn) {
        super(orderId, sequenceNumber, generatedOn);
    }

    @Override
    public void applyOn(Order order) {
        requireNonNull(order, "order");

        order.apply(this);
    }
}
