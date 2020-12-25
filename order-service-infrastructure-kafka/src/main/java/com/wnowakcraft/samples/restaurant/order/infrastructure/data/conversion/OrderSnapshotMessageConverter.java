package com.wnowakcraft.samples.restaurant.order.infrastructure.data.conversion;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.wnowakcraft.samples.restaurant.order.domain.model.CustomerId;
import com.wnowakcraft.samples.restaurant.order.domain.model.Order;
import com.wnowakcraft.samples.restaurant.order.domain.model.OrderSnapshot;
import com.wnowakcraft.samples.restaurant.order.domain.model.RestaurantId;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.message.OrderSnapshotMessage;

import java.time.Instant;

import static com.wnowakcraft.samples.restaurant.order.infrastructure.data.conversion.OrderItemConverter.messageOrderItemsOf;
import static com.wnowakcraft.samples.restaurant.order.infrastructure.data.conversion.OrderItemConverter.orderItemsOf;

@ConcreteDataConverter
public class OrderSnapshotMessageConverter implements MessageConverter<OrderSnapshot, OrderSnapshotMessage> {
    @Override
    public OrderSnapshot convert(OrderSnapshotMessage orderSnapshotMessage, long offset) {
        return OrderSnapshot.recreateFrom(
                OrderSnapshot.Id.of(orderSnapshotMessage.getSnapshotId()),
                Order.Id.of(orderSnapshotMessage.getOrderId()),
                Instant.ofEpochSecond(orderSnapshotMessage.getDateGenerated().getSeconds()),
                Order.Version.of(orderSnapshotMessage.getOrderVersion()),
                CustomerId.of(orderSnapshotMessage.getCustomerId()),
                RestaurantId.of(orderSnapshotMessage.getRestaurantId()),
                Order.Status.valueOf(orderSnapshotMessage.getOrderStatus()),
                orderItemsOf(orderSnapshotMessage.getOrderItemsList())
        );
    }

    @Override
    public boolean canConvert(Message message) {
        return OrderSnapshotMessage.class == message.getClass();
    }

    @Override
    public OrderSnapshotMessage convert(OrderSnapshot orderSnapshot) {
        return OrderSnapshotMessage.newBuilder()
                .setSnapshotId(orderSnapshot.getSnapshotId().getValue())
                .setOrderId(orderSnapshot.getAggregateId().getValue())
                .setRestaurantId(orderSnapshot.getRestaurantId().getValue())
                .setCustomerId(orderSnapshot.getCustomerId().getValue())
                .setOrderStatus(orderSnapshot.getOrderStatus().getName())
                .addAllOrderItems(messageOrderItemsOf(orderSnapshot.getOrderItems()))
                .setOrderVersion(orderSnapshot.getAggregateVersion().number)
                .setDateGenerated(Timestamp.newBuilder().setSeconds(orderSnapshot.getCreationDate().getEpochSecond()).build())
                .build();
    }

    @Override
    public boolean canConvert(Object snapshotCandidate) {
        return OrderSnapshot.class == snapshotCandidate.getClass();
    }
}
