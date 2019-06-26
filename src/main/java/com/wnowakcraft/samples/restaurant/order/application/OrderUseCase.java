package com.wnowakcraft.samples.restaurant.order.application;

import com.wnowakcraft.samples.restaurant.core.domain.EventStore;
import com.wnowakcraft.samples.restaurant.order.model.*;
import com.wnowakcraft.samples.restaurant.order.model.Order.OrderId;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.wnowakcraft.preconditions.Preconditions.requireNonEmpty;

@RequiredArgsConstructor
public class OrderUseCase {
    @NonNull private final EventStore<OrderEvent, Order, OrderId> eventStore;

    public String createOrder(String customerId, String restaurantId, List<OrderLine> orderLines) {
        requireNonEmpty(orderLines, "orderLines");

        var newOrder = Order.newOrder(
                CustomerId.of(customerId),
                RestaurantId.of(restaurantId),
                orderItemsFrom(orderLines)
        );

        eventStore.append(newOrder.getId(), newOrder.getChanges());

        return newOrder.getId().getId();
    }

    private Collection<OrderItem> orderItemsFrom(List<OrderLine> orderLines) {
        return orderLines.stream()
                .map(ol -> new OrderItem(ol.getQuantity(), ol.getName(), MenuItemId.of(ol.getMenuItemId())))
                .collect(Collectors.toUnmodifiableList());
    }

    public void approveOrder(String orderId) {
        final OrderId anOrderId = OrderId.of(orderId);
        var order = Order.restoreFrom(eventStore.loadAllEventsFor(anOrderId));

        order.approve();

        eventStore.append(anOrderId, order.getChanges());
    }
}
