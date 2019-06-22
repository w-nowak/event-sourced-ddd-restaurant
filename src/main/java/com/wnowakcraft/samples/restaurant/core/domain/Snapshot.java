package com.wnowakcraft.samples.restaurant.core.domain;

import com.wnowakcraft.samples.restaurant.core.domain.Aggregate.AggregateId;

import java.time.Instant;

public interface Snapshot <ID extends Id, AID extends AggregateId> {
    ID getSnapshotId();
    AID getAggregateId();
    Instant getCreationDate();
}
