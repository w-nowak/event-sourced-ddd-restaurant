package com.wnowakcraft.samples.restaurant.order.application;

import com.wnowakcraft.samples.restaurant.core.domain.model.AggregateRepository;
import com.wnowakcraft.samples.restaurant.core.domain.model.EventStore;
import com.wnowakcraft.samples.restaurant.order.domain.model.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.wnowakcraft.preconditions.Preconditions.requireNonEmpty;

@RequiredArgsConstructor(onConstructor_ = { @Inject })
public class OrderUseCase {
    @NonNull private final EventStore<OrderEvent, Order, Order.Id> eventStore;
    @NonNull private final AggregateRepository<OrderEvent, Order, OrderSnapshot, Order.Id> aggregateRepository;

    public String createOrder(String customerId, String restaurantId, List<OrderLine> orderLines) {
        requireNonEmpty(orderLines, "orderLines");

        var newOrder = Order.newOrder(
                CustomerId.of(customerId),
                RestaurantId.of(restaurantId),
                orderItemsFrom(orderLines)
        );

        aggregateRepository.save(newOrder);

        return newOrder.getId().getValue();
    }

    private Collection<OrderItem> orderItemsFrom(List<OrderLine> orderLines) {
        return orderLines.stream()
                .map(ol -> new OrderItem(ol.getQuantity(), ol.getName(), MenuItemId.of(ol.getMenuItemId())))
                .collect(Collectors.toUnmodifiableList());
    }

    public void approveOrder(String orderId) {
        final var anOrderId = Order.Id.of(orderId);

        var order = aggregateRepository.getById(anOrderId);

        order.approve();

        aggregateRepository.save(order);
    }
}
