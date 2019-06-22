package com.wnowakcraft.samples.restaurant.order.model;

import com.wnowakcraft.samples.restaurant.core.domain.Id;
import com.wnowakcraft.samples.restaurant.core.domain.Snapshot;
import com.wnowakcraft.samples.restaurant.order.model.Order.OrderId;

import java.time.Instant;

public class OrderSnapshot implements Snapshot<OrderSnapshot.OrderSnapshotId,OrderId> {

    @Override
    public OrderSnapshotId getSnapshotId() {
        return null;
    }

    @Override
    public OrderId getAggregateId() {
        return null;
    }

    @Override
    public Instant getCreationDate() {
        return null;
    }

    public static class OrderSnapshotId implements Id<String> {

        @Override
        public String getId() {
            return "something";
        }
    }
}
