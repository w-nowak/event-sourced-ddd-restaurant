package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.AbstractSnapshot;
import com.wnowakcraft.samples.restaurant.core.domain.Aggregate.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Collection;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;
import static com.wnowakcraft.samples.restaurant.core.utils.ApplicationTime.instantNow;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class OrderSnapshot extends AbstractSnapshot<OrderSnapshot.Id, Order.Id> {
    private final RestaurantId restaurantId;
    private final Order.Status orderStatus;
    private final Collection<OrderItem> orderItems;

    static OrderSnapshot newSnapshot(Order.Id orderId, RestaurantId restaurantId, Order.Status orderStatus, Collection<OrderItem> orderItems) {
        requireNonNull(restaurantId, "restaurantId");
        requireNonNull(orderStatus, "orderStatus");
        requireNonNull(orderItems, "orderItems");

        return new OrderSnapshot(OrderSnapshot.Id.newId(), orderId, instantNow(), Version.NONE, restaurantId, orderStatus, orderItems);
    }

    public static OrderSnapshot recreateFrom(OrderSnapshot.Id snapshotId, Order.Id orderId, Instant creationDate, Version version,
                                             RestaurantId restaurantId, Order.Status orderStatus, Collection<OrderItem> orderItems) {
        requireNonNull(restaurantId, "restaurantId");
        requireNonNull(orderStatus, "orderStatus");
        requireNonNull(orderItems, "orderItems");

        return new OrderSnapshot(snapshotId, orderId, creationDate, version, restaurantId, orderStatus, orderItems);
    }

    private OrderSnapshot(OrderSnapshot.Id snapshotId, Order.Id orderId, Instant creationDate, Version version,
                          RestaurantId restaurantId, Order.Status orderStatus, Collection<OrderItem> orderItems) {
        super(snapshotId, orderId, creationDate, version);
        this.restaurantId = restaurantId;
        this.orderStatus = orderStatus;
        this.orderItems = orderItems;
    }

    public static final class Id extends SnapshotId {

        private static Id newId() {
            return new Id(OrderSubdomain.NAME, Order.AGGREGATE_NAME);
        }

        public static Id of(String orderSnapshotId) {
            return new Id(orderSnapshotId, OrderSubdomain.NAME, Order.AGGREGATE_NAME);
        }

        private Id(String domainName, String domainObjectName) {
            super(domainName, domainObjectName);
        }

        private Id(String snapshotId, String domainName, String domainObjectName) {
            super(snapshotId, domainName, domainObjectName);
        }
    }
}
