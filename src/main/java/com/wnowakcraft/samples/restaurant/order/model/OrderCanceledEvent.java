package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.AbstractEvent;

import java.time.Instant;

public final class OrderCanceledEvent extends AbstractEvent<Order.Id> implements OrderEvent {

    public OrderCanceledEvent restoreFrom(Order.Id orderId, SequenceNumber sequenceNumber, Instant generatedOn) {
        return new OrderCanceledEvent(orderId, sequenceNumber, generatedOn);
    }

    public OrderCanceledEvent(Order.Id orderId) {
        super(orderId);
    }

    private OrderCanceledEvent(Order.Id orderId, SequenceNumber sequenceNumber, Instant generatedOn) {
        super(orderId, sequenceNumber, generatedOn);
    }

    @Override
    public void applyOn(Order order) {
        order.apply(this);
    }
}
