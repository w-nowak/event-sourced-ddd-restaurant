package com.wnowakcraft.samples.restaurant.order.application;

import com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate;
import com.wnowakcraft.samples.restaurant.core.domain.model.EventStore;
import com.wnowakcraft.samples.restaurant.core.domain.model.EventStore.EventStream;
import com.wnowakcraft.samples.restaurant.core.domain.model.SnapshotRepository;
import com.wnowakcraft.samples.restaurant.order.domain.model.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.wnowakcraft.preconditions.Preconditions.requireNonEmpty;

@RequiredArgsConstructor(onConstructor_ = { @Inject })
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

        eventStore.append(newOrder.getId(), newOrder.getVersion(), newOrder.getChanges())
                .whenCompleteAsync(updateOrderVersion(newOrder));


        return newOrder.getId().getValue();
    }

    private Collection<OrderItem> orderItemsFrom(List<OrderLine> orderLines) {
        return orderLines.stream()
                .map(ol -> new OrderItem(ol.getQuantity(), ol.getName(), MenuItemId.of(ol.getMenuItemId())))
                .collect(Collectors.toUnmodifiableList());
    }

    public void approveOrder(String orderId) {
        final var anOrderId = Order.Id.of(orderId);

        var order = snapshotRepository.findLatestSnapshotFor(anOrderId)
                .map(s -> restoreOrderFrom(s, eventStore.loadEventsFor(anOrderId, s.getAggregateVersion().nextVersion())))
                .orElseGet(() -> restoreOrderFrom(eventStore.loadAllEventsFor(anOrderId)));

        order.approve();

        eventStore.append(anOrderId, order.getVersion(), order.getChanges())
                .whenCompleteAsync(updateOrderVersion(order));
    }

    private BiConsumer<Aggregate.Version, Throwable> updateOrderVersion(Order order) {
        return (newOrderVersion, exception) -> {
            if (newOrderVersion != null) {
                order.updateVersionTo(newOrderVersion);
            }
        };
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
