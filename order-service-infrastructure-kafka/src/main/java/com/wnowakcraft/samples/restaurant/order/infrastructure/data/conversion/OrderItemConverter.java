package com.wnowakcraft.samples.restaurant.order.infrastructure.data.conversion;

import com.wnowakcraft.samples.restaurant.order.domain.model.MenuItemId;
import com.wnowakcraft.samples.restaurant.order.domain.model.OrderItem;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.message.OrderItemMessageComponent;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

class OrderItemConverter {
    static Collection<OrderItem> orderItemsOf(List<OrderItemMessageComponent> messageOrderItems) {
        return messageOrderItems.stream()
                .map(OrderItemConverter::toOrderItem)
                .collect(Collectors.toUnmodifiableList());
    }

    static OrderItem toOrderItem(OrderItemMessageComponent messageOrderItem) {
        return new OrderItem(
                messageOrderItem.getQuantity(),
                messageOrderItem.getName(),
                MenuItemId.of(messageOrderItem.getMenuItemId())
        );
    }

    static Collection<OrderItemMessageComponent> messageOrderItemsOf(Collection<OrderItem> orderItems) {
        return orderItems.stream()
                .map(OrderItemConverter::toMessageOrderItem)
                .collect(Collectors.toUnmodifiableList());
    }

    static OrderItemMessageComponent toMessageOrderItem(OrderItem orderItem) {
        return OrderItemMessageComponent.newBuilder()
                .setQuantity(orderItem.getQuantity())
                .setName(orderItem.getName())
                .setMenuItemId(orderItem.getMenuItemId().getValue())
                .build();
    }
}
