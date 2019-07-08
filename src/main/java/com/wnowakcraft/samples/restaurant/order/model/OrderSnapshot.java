package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.AbstractSnapshot;
import com.wnowakcraft.samples.restaurant.core.domain.Aggregate.Version;
import com.wnowakcraft.samples.restaurant.order.model.Order.OrderId;
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
public class OrderSnapshot extends AbstractSnapshot<OrderSnapshot.OrderSnapshotId, OrderId> {
    private final RestaurantId restaurantId;
    private final Order.Status orderStatus;
    private final Collection<OrderItem> orderItems;

    static OrderSnapshot newSnapshot(OrderId orderId, RestaurantId restaurantId, Order.Status orderStatus, Collection<OrderItem> orderItems) {
        requireNonNull(restaurantId, "restaurantId");
        requireNonNull(orderStatus, "orderStatus");
        requireNonNull(orderItems, "orderItems");

        return new OrderSnapshot(OrderSnapshotId.newId(), orderId, instantNow(), Version.NONE, restaurantId, orderStatus, orderItems);
    }

    public static OrderSnapshot recreateFrom(OrderSnapshotId snapshotId, OrderId orderId, Instant creationDate, Version version,
                                             RestaurantId restaurantId, Order.Status orderStatus, Collection<OrderItem> orderItems) {
        requireNonNull(restaurantId, "restaurantId");
        requireNonNull(orderStatus, "orderStatus");
        requireNonNull(orderItems, "orderItems");

        return new OrderSnapshot(snapshotId, orderId, creationDate, version, restaurantId, orderStatus, orderItems);
    }

    private OrderSnapshot(OrderSnapshotId snapshotId, OrderId orderId, Instant creationDate, Version version,
                          RestaurantId restaurantId, Order.Status orderStatus, Collection<OrderItem> orderItems) {
        super(snapshotId, orderId, creationDate, version);
        this.restaurantId = restaurantId;
        this.orderStatus = orderStatus;
        this.orderItems = orderItems;
    }

    public static final class OrderSnapshotId extends SnapshotId {

        private static OrderSnapshotId newId() {
            return new OrderSnapshotId(OrderSubdomain.NAME, Order.AGGREGATE_NAME);
        }

        public static OrderSnapshotId of(String orderSnapshotId) {
            return new OrderSnapshotId(orderSnapshotId, OrderSubdomain.NAME, Order.AGGREGATE_NAME);
        }

        private OrderSnapshotId(String domainName, String domainObjectName) {
            super(domainName, domainObjectName);
        }

        private OrderSnapshotId(String snapshotId, String domainName, String domainObjectName) {
            super(snapshotId, domainName, domainObjectName);
        }
    }
}
