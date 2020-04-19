package com.wnowakcraft.samples.restaurant.order.infrastructure.data.conversion;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.wnowakcraft.samples.restaurant.core.domain.model.Event;
import com.wnowakcraft.samples.restaurant.core.domain.model.Event.SequenceNumber;
import com.wnowakcraft.samples.restaurant.order.domain.model.*;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.message.OrderApprovedEventMessage;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.message.OrderCreatedEventMessage;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Collection;
import java.util.stream.Collectors;

@ConcreteDataConverter
//@RequiredArgsConstructor(onConstructor_ = { @Inject})
public class OrderApprovedEventDataConverter implements DataConverter<OrderApprovedEvent, OrderApprovedEventMessage> {
    @Override
    public OrderApprovedEvent convert(OrderApprovedEventMessage message, long offset) {
        return orderApprovedEventOf(message, offset);
    }

    private OrderApprovedEvent orderApprovedEventOf(OrderApprovedEventMessage message, long offset) {
       return OrderApprovedEvent.restoreFrom(
               Order.Id.of(message.getOrderId()),
               SequenceNumber.of(offset),
               Instant.ofEpochSecond(message.getDateGenerated().getSeconds())
       );
    }

    @Override
    public OrderApprovedEventMessage convert(OrderApprovedEvent event) {
        return orderApprovedEventMessageOf(event);
    }

    private OrderApprovedEventMessage orderApprovedEventMessageOf(OrderApprovedEvent event) {
        return OrderApprovedEventMessage.newBuilder()
                .setOrderId(event.getConcernedAggregateId().getValue())
                .setDateGenerated(Timestamp.newBuilder().setSeconds(event.getGeneratedOn().getEpochSecond()))
                .build();
    }

    @Override
    public boolean canConvert(Message message) {
        return OrderApprovedEventMessage.class == message.getClass();
    }

    @Override
    public boolean canConvert(Event<?> event) {
        return OrderApprovedEvent.class == event.getClass();
    }
}
