package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.AbstractEvent;
import com.wnowakcraft.samples.restaurant.order.model.Order.OrderId;

import java.time.Instant;

public final class OrderCanceledEvent extends AbstractEvent<OrderId> implements OrderEvent {

    public OrderCanceledEvent restoreFrom(OrderId orderId, SequenceNumber sequenceNumber, Instant generatedOn) {
        return new OrderCanceledEvent(orderId, sequenceNumber, generatedOn);
    }

    public OrderCanceledEvent(OrderId orderId) {
        super(orderId);
    }

    private OrderCanceledEvent(OrderId orderId, SequenceNumber sequenceNumber, Instant generatedOn) {
        super(orderId, sequenceNumber, generatedOn);
    }

    @Override
    public void applyOn(Order order) {
        order.apply(this);
    }
}
