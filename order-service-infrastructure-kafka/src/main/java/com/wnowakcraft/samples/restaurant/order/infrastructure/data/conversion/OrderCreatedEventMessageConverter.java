package com.wnowakcraft.samples.restaurant.order.infrastructure.data.conversion;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.wnowakcraft.samples.restaurant.core.domain.model.Event.SequenceNumber;
import com.wnowakcraft.samples.restaurant.order.domain.model.CustomerId;
import com.wnowakcraft.samples.restaurant.order.domain.model.Order;
import com.wnowakcraft.samples.restaurant.order.domain.model.OrderCreatedEvent;
import com.wnowakcraft.samples.restaurant.order.domain.model.RestaurantId;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.message.OrderCreatedEventMessage;

import java.time.Instant;

import static com.wnowakcraft.samples.restaurant.order.infrastructure.data.conversion.OrderItemConverter.messageOrderItemsOf;
import static com.wnowakcraft.samples.restaurant.order.infrastructure.data.conversion.OrderItemConverter.orderItemsOf;

@ConcreteDataConverter
public class OrderCreatedEventMessageConverter implements MessageConverter<OrderCreatedEvent, OrderCreatedEventMessage> {
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

    @Override
    public OrderCreatedEventMessage convert(OrderCreatedEvent event) {
        return orderCreatedEventMessageOf(event);
    }

    private OrderCreatedEventMessage orderCreatedEventMessageOf(OrderCreatedEvent event) {
        return OrderCreatedEventMessage.newBuilder()
                .setOrderId(event.getConcernedAggregateId().getValue())
                .setCustomerId(event.getCustomerId().getValue())
                .setRestaurantId(event.getRestaurantId().getValue())
                .addAllOrderItems(messageOrderItemsOf(event.getOrderItems()))
                .setDateGenerated(Timestamp.newBuilder().setSeconds(event.getGeneratedOn().getEpochSecond()))
                .build();
    }

    @Override
    public boolean canConvert(Message message) {
        return OrderCreatedEventMessage.class == message.getClass();
    }

    @Override
    public boolean canConvert(Object eventCandidate) {
        return OrderCreatedEvent.class == eventCandidate.getClass();
    }
}
