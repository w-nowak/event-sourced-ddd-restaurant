package com.wnowakcraft.samples.restaurant.order.application;

import com.wnowakcraft.samples.restaurant.core.domain.EventStore;
import com.wnowakcraft.samples.restaurant.core.domain.EventStore.EventStream;
import com.wnowakcraft.samples.restaurant.core.domain.SnapshotRepository;
import com.wnowakcraft.samples.restaurant.order.model.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.wnowakcraft.preconditions.Preconditions.requireNonEmpty;

@RequiredArgsConstructor
public class OrderUseCase {
    @NonNull private final EventStore<OrderEvent, Order, Order.Id> eventStore;
    @NonNull private final SnapshotRepository<OrderSnapshot, Order.Id> snapshotRepository;

    public String createOrder(String customerId, String restaurantId, List<OrderLine> orderLines) {
        requireNonEmpty(orderLines, "orderLines");

        var newOrder = Order.newOrder(
                CustomerId.of(customerId),
                RestaurantId.of(restaurantId),
                orderItemsFrom(orderLines)
        );

        eventStore.append(newOrder.getId(), newOrder.getVersion(), newOrder.getChanges());

        return newOrder.getId().getId();
    }

    private Collection<OrderItem> orderItemsFrom(List<OrderLine> orderLines) {
        return orderLines.stream()
                .map(ol -> new OrderItem(ol.getQuantity(), ol.getName(), MenuItemId.of(ol.getMenuItemId())))
                .collect(Collectors.toUnmodifiableList());
    }

    public void approveOrder(String orderId) {
        final var anOrderId = Order.Id.of(orderId);

        var order = snapshotRepository.findLatestSnapshotFor(anOrderId)
                .map(s -> restoreOrderFrom(s, eventStore.loadEventsFor(anOrderId, s.getAggregateVersion())))
                .orElseGet(() -> restoreOrderFrom(eventStore.loadAllEventsFor(anOrderId)));

        order.approve();

        eventStore.append(anOrderId, order.getVersion(), order.getChanges());
    }

    private static Order restoreOrderFrom(OrderSnapshot snapshot, EventStream<OrderEvent> eventStream) {
        return Order.restoreFrom(
                snapshot,
                eventStream.getEvents(),
                eventStream.isEmpty() ? snapshot.getAggregateVersion() : eventStream.getVersion()
        );
    }

    private static Order restoreOrderFrom(EventStream<OrderEvent> eventStream) {
        return Order.restoreFrom(eventStream.getEvents(), eventStream.getVersion());
    }
}
