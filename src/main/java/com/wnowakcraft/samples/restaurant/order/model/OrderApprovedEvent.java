package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.AbstractEvent;

import java.time.Instant;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;

public final class OrderApprovedEvent extends AbstractEvent<Order.Id> implements OrderEvent {

    public static OrderApprovedEvent restoreFrom(Order.Id orderId, SequenceNumber sequenceNumber, Instant generatedOn) {
        return new OrderApprovedEvent(orderId, sequenceNumber, generatedOn);
    }

    public OrderApprovedEvent(Order.Id orderId) {
        super(orderId);
    }

    private OrderApprovedEvent(Order.Id orderId, SequenceNumber sequenceNumber, Instant generatedOn) {
        super(orderId, sequenceNumber, generatedOn);
    }

    @Override
    public void applyOn(Order order) {
        requireNonNull(order, "order");
        order.apply(this);
    }
}
