package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.AbstractEvent;
import com.wnowakcraft.samples.restaurant.order.model.Order.OrderId;
import lombok.*;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static com.wnowakcraft.preconditions.Preconditions.requireNonEmpty;
import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class OrderCreatedEvent extends AbstractEvent<OrderId> implements OrderEvent {
    private final CustomerId customerId;
    private final RestaurantId restaurantId;
    private final Collection<OrderItem> orderItems;

    public static OrderCreatedEvent restoreFrom(OrderId orderId, CustomerId customerId, RestaurantId restaurantId,
                                                Collection<OrderItem> orderItems, SequenceNumber sequenceNumber, Instant generatedOn) {
        return new OrderCreatedEvent(orderId, customerId, restaurantId, orderItems, sequenceNumber, generatedOn);
    }

    public OrderCreatedEvent(OrderId orderId, CustomerId customerId, RestaurantId restaurantId, Collection<OrderItem> orderItems) {
        super(orderId);
        this.customerId = requireNonNull(customerId, "customerId");
        this.restaurantId = requireNonNull(restaurantId, "restaurantId");
        this.orderItems = List.copyOf(requireNonEmpty(orderItems, "orderItems"));
    }

    private OrderCreatedEvent(OrderId orderId, CustomerId customerId, RestaurantId restaurantId, Collection<OrderItem> orderItems,
                             SequenceNumber sequenceNumber, Instant generatedOn) {
        super(orderId, sequenceNumber, generatedOn);
        this.customerId = requireNonNull(customerId, "customerId");
        this.restaurantId = requireNonNull(restaurantId, "restaurantId");
        this.orderItems = List.copyOf(requireNonEmpty(orderItems, "orderItems"));
    }

    @Override
    public void applyOn(Order order) {
        order.apply(this);
    }
}
