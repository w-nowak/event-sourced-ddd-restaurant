package com.wnowakcraft.samples.restaurant.order.application;

import com.wnowakcraft.samples.restaurant.core.domain.EventStore;
import com.wnowakcraft.samples.restaurant.order.model.CustomerId;
import com.wnowakcraft.samples.restaurant.order.model.Order;
import com.wnowakcraft.samples.restaurant.order.model.Order.OrderId;
import com.wnowakcraft.samples.restaurant.order.model.OrderEvent;
import com.wnowakcraft.samples.restaurant.order.model.RestaurantId;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.wnowakcraft.preconditions.Preconditions.requireNonEmpty;

@RequiredArgsConstructor
public class OrderUseCase {
    @NonNull private final EventStore<OrderEvent, Order, OrderId> eventStore;

    public String createOrder(String customerId, String restaurantId, List<OrderLine> orderLines) {
        requireNonEmpty(orderLines, "orderLines");

        var newOrder = Order.newOrder(CustomerId.of(customerId), RestaurantId.of(restaurantId));

        eventStore.append(OrderId.newOrderId(), newOrder.getChanges());

        return newOrder.getAggregateId().getId();
    }

    public void approveOrder(String orderId) {
        final OrderId anOrderId = OrderId.of(orderId);
        var order = Order.restoreFrom(eventStore.loadAllEventsFor(anOrderId));

        order.approve();

        eventStore.append(anOrderId, order.getChanges());
    }
}
