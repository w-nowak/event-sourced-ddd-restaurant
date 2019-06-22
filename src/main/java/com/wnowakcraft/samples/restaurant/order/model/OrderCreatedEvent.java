package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.AbstractEvent;
import com.wnowakcraft.samples.restaurant.order.model.Order.OrderId;
import lombok.*;

import java.time.Instant;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class OrderCreatedEvent extends AbstractEvent<OrderId> implements OrderEvent {
    private final CustomerId customerId;
    private final RestaurantId restaurantId;

    public static OrderCreatedEvent restoreFrom(OrderId orderId, CustomerId customerId, RestaurantId restaurantId,
                                                SequenceNumber sequenceNumber, Instant generatedOn) {
        return new OrderCreatedEvent(orderId, customerId, restaurantId, sequenceNumber, generatedOn);
    }

    public OrderCreatedEvent(OrderId orderId, CustomerId customerId, RestaurantId restaurantId) {
        super(orderId);
        this.customerId = requireNonNull(customerId, "customerId");
        this.restaurantId = requireNonNull(restaurantId, "restaurantId");
    }

    public OrderCreatedEvent(OrderId orderId, CustomerId customerId, RestaurantId restaurantId,
                             SequenceNumber sequenceNumber, Instant generatedOn) {
        super(orderId, sequenceNumber, generatedOn);
        this.customerId = requireNonNull(customerId, "customerId");
        this.restaurantId = requireNonNull(restaurantId, "restaurantId");
    }

    @Override
    public void applyOn(Order order) {
        order.apply(this);
    }
}
