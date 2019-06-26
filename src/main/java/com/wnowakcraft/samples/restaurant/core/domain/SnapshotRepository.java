package com.wnowakcraft.samples.restaurant.core.domain;

import com.wnowakcraft.samples.restaurant.core.domain.Aggregate.AggregateId;
import com.wnowakcraft.samples.restaurant.core.domain.Snapshot.SnapshotId;

import java.time.Instant;

public interface SnapshotRepository<ID extends SnapshotId, AID extends AggregateId> {
    Snapshot<SnapshotId, AggregateId> findLatestSnapshotFor(AID aggregateId);
    Snapshot<SnapshotId, AggregateId> findLatestSnapshotFor(AID aggregateId, Event.SequenceNumber beforeGivenEventSequenceNumber);
    Snapshot<SnapshotId, AggregateId> findLatestSnapshotFor(AID aggregateId, Instant beforeGivenPointInTime);
    Snapshot<SnapshotId, AggregateId> findFirstSnapshotFor(AID aggregateId, Instant afterGivenPointInTime);
    void addNewSnapshot(Snapshot<SnapshotId, AggregateId> snapshot);
}
