package com.wnowakcraft.samples.restaurant.order.domain.model;

import com.wnowakcraft.samples.restaurant.core.domain.model.AbstractSnapshot;
import com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate.Version;
import com.wnowakcraft.samples.restaurant.core.domain.model.Snapshot;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Collection;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;
import static com.wnowakcraft.samples.restaurant.core.utils.ApplicationTime.instantNow;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class OrderSnapshot extends AbstractSnapshot<OrderSnapshot.Id, Order.Id> {
    private final CustomerId customerId;
    private final RestaurantId restaurantId;
    private final Order.Status orderStatus;
    private final Collection<OrderItem> orderItems;

    static OrderSnapshot newSnapshot(Order.Id orderId, Version version, CustomerId customerId, RestaurantId restaurantId,
                                     Order.Status orderStatus, Collection<OrderItem> orderItems) {
        return new OrderSnapshot(
                OrderSnapshot.Id.newId(), orderId, instantNow(), version,
                customerId, restaurantId, orderStatus, orderItems
        );
    }

    public static OrderSnapshot recreateFrom(Id snapshotId, Order.Id orderId, Instant creationDate, Version version, CustomerId customerId,
                                             RestaurantId restaurantId, Order.Status orderStatus, Collection<OrderItem> orderItems) {
        return new OrderSnapshot(snapshotId, orderId, creationDate, version, customerId, restaurantId, orderStatus, orderItems);
    }

    private OrderSnapshot(Id snapshotId, Order.Id orderId, Instant creationDate, Version version, CustomerId customerId,
                          RestaurantId restaurantId, Order.Status orderStatus, Collection<OrderItem> orderItems) {
        super(snapshotId, orderId, creationDate, version);
        this.customerId = requireNonNull(customerId, "customerId");
        this.restaurantId = requireNonNull(restaurantId, "restaurantId");;
        this.orderStatus = requireNonNull(orderStatus, "orderStatus");;
        this.orderItems = requireNonNull(orderItems, "orderItems");
    }

    public static final class Id extends Snapshot.Id {

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
