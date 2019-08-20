package com.wnowakcraft.samples.restaurant.order.domain.model;

import com.wnowakcraft.samples.restaurant.core.domain.model.AbstractEvent;
import lombok.*;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static com.wnowakcraft.preconditions.Preconditions.requireNonEmpty;
import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class OrderCreatedEvent extends AbstractEvent<Order.Id> implements OrderEvent {
    private final CustomerId customerId;
    private final RestaurantId restaurantId;
    private final Collection<OrderItem> orderItems;

    public static OrderCreatedEvent restoreFrom(Order.Id orderId, CustomerId customerId, RestaurantId restaurantId,
                                                Collection<OrderItem> orderItems, SequenceNumber sequenceNumber, Instant generatedOn) {
        return new OrderCreatedEvent(orderId, customerId, restaurantId, orderItems, sequenceNumber, generatedOn);
    }

    public OrderCreatedEvent(Order.Id orderId, CustomerId customerId, RestaurantId restaurantId, Collection<OrderItem> orderItems) {
        super(orderId);
        this.customerId = requireNonNull(customerId, "customerId");
        this.restaurantId = requireNonNull(restaurantId, "restaurantId");
        this.orderItems = List.copyOf(requireNonEmpty(orderItems, "orderItems"));
    }

    private OrderCreatedEvent(Order.Id orderId, CustomerId customerId, RestaurantId restaurantId, Collection<OrderItem> orderItems,
                              SequenceNumber sequenceNumber, Instant generatedOn) {
        super(orderId, sequenceNumber, generatedOn);
        this.customerId = requireNonNull(customerId, "customerId");
        this.restaurantId = requireNonNull(restaurantId, "restaurantId");
        this.orderItems = List.copyOf(requireNonEmpty(orderItems, "orderItems"));
    }

    @Override
    public void applyOn(Order order) {
        requireNonNull(order, "order");

        order.apply(this);
    }
}
