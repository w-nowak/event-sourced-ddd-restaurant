package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.AbstractEvent;
import com.wnowakcraft.samples.restaurant.order.model.Order.OrderId;

import java.time.Instant;

public final class OrderApprovedEvent extends AbstractEvent<OrderId> implements OrderEvent {

    public OrderApprovedEvent restoreFrom(OrderId orderId, SequenceNumber sequenceNumber, Instant generatedOn) {
        return new OrderApprovedEvent(orderId, sequenceNumber, generatedOn);
    }

    public OrderApprovedEvent(OrderId orderId) {
        super(orderId);
    }

    private OrderApprovedEvent(OrderId orderId, SequenceNumber sequenceNumber, Instant generatedOn) {
        super(orderId, sequenceNumber, generatedOn);
    }

    @Override
    public void applyOn(Order order) {
        order.apply(this);
    }
}
