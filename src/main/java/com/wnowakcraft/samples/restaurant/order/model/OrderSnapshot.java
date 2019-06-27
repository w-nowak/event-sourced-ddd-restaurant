package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.AbstractSnapshot;
import com.wnowakcraft.samples.restaurant.core.domain.Aggregate.Version;
import com.wnowakcraft.samples.restaurant.order.model.Order.OrderId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class OrderSnapshot extends AbstractSnapshot<OrderSnapshot.OrderSnapshotId, OrderId> {
    private final RestaurantId restaurantId;
    private final Order.Status orderStatus;

    static OrderSnapshot newSnapshot(OrderId orderId, RestaurantId restaurantId, Order.Status orderStatus) {
        requireNonNull(restaurantId, "restaurantId");
        requireNonNull(orderStatus, "orderStatus");

        return new OrderSnapshot(OrderSnapshotId.newId(), orderId, Instant.now(), Version.NONE, restaurantId, orderStatus);
    }

    public static OrderSnapshot recreateFrom(OrderSnapshotId snapshotId, OrderId orderId, Instant creationDate, Version version,
                                             RestaurantId restaurantId, Order.Status orderStatus) {
        requireNonNull(restaurantId, "restaurantId");
        requireNonNull(orderStatus, "orderStatus");

        return new OrderSnapshot(snapshotId, orderId, creationDate, version, restaurantId, orderStatus);
    }

    private OrderSnapshot(OrderSnapshotId snapshotId, OrderId orderId, Instant creationDate, Version version,
                          RestaurantId restaurantId, Order.Status orderStatus) {
        super(snapshotId, orderId, creationDate, version);
        this.restaurantId = restaurantId;
        this.orderStatus = orderStatus;
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
