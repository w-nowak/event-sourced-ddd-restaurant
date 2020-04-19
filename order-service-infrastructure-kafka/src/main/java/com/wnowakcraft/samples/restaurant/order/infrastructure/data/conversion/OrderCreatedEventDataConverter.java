package com.wnowakcraft.samples.restaurant.order.infrastructure.data.conversion;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.wnowakcraft.samples.restaurant.core.domain.model.Event;
import com.wnowakcraft.samples.restaurant.core.domain.model.Event.SequenceNumber;
import com.wnowakcraft.samples.restaurant.order.domain.model.*;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.message.OrderCreatedEventMessage;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@ConcreteDataConverter
@RequiredArgsConstructor(onConstructor_ = { @Inject})
public class OrderCreatedEventDataConverter implements DataConverter<OrderCreatedEvent, OrderCreatedEventMessage> {
    @Override
    public OrderCreatedEvent convert(OrderCreatedEventMessage message, long offset) {
        return orderCreatedEventOf(message, offset);
    }

    private OrderCreatedEvent orderCreatedEventOf(OrderCreatedEventMessage message, long offset) {
       return OrderCreatedEvent.restoreFrom(
               Order.Id.of(message.getOrderId()),
               CustomerId.of(message.getCustomerId()),
               RestaurantId.of(message.getRestaurantId()),
               orderItemsOf(message.getOrderItemsList()),
               SequenceNumber.of(offset),
               Instant.ofEpochSecond(message.getDateGenerated().getSeconds())
       );
    }

    private Collection<OrderItem> orderItemsOf(List<OrderCreatedEventMessage.OrderItem> messageOrderItems) {
        return messageOrderItems.stream()
                .map(this::toOrderItem)
                .collect(Collectors.toUnmodifiableList());
    }

    private OrderItem toOrderItem(OrderCreatedEventMessage.OrderItem messageOrderItem) {
        return new OrderItem(
                messageOrderItem.getQuantity(),
                messageOrderItem.getName(),
                MenuItemId.of(messageOrderItem.getMenuItemId())
        );
    }

    @Override
    public OrderCreatedEventMessage convert(OrderCreatedEvent event) {
        return orderCreatedEventMessageOf(event);
    }

    private OrderCreatedEventMessage orderCreatedEventMessageOf(OrderCreatedEvent event) {
        return OrderCreatedEventMessage.newBuilder()
                .setOrderId(event.getConcernedAggregateId().getValue())
                .setCustomerId(event.getCustomerId().getValue())
                .setRestaurantId(event.getRestaurantId().getValue())
                .addAllOrderItems(messageOrderItemsOf(event))
                .setDateGenerated(Timestamp.newBuilder().setSeconds(event.getGeneratedOn().getEpochSecond()))
                .build();
    }

    private Collection<OrderCreatedEventMessage.OrderItem> messageOrderItemsOf(OrderCreatedEvent event) {
        return event.getOrderItems().stream()
                .map(this::toMessageOrderItem)
                .collect(Collectors.toUnmodifiableList());
    }

    private OrderCreatedEventMessage.OrderItem toMessageOrderItem(OrderItem orderItem) {
        return OrderCreatedEventMessage.OrderItem.newBuilder()
                .setQuantity(orderItem.getQuantity())
                .setName(orderItem.getName())
                .setMenuItemId(orderItem.getMenuItemId().getValue())
                .build();
    }

    @Override
    public boolean canConvert(Message message) {
        return OrderCreatedEventMessage.class == message.getClass();
    }

    @Override
    public boolean canConvert(Event<?> event) {
        return OrderCreatedEvent.class == event.getClass();
    }
}
