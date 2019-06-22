package com.wnowakcraft.samples.restaurant.core.domain;

import com.wnowakcraft.samples.restaurant.core.domain.Aggregate.AggregateId;

import java.time.Instant;

public interface SnapshotRepository<ID extends Id, AID extends AggregateId> {
    Snapshot<ID, AggregateId> findLatestSnapshotFor(AID aggregateId);
    Snapshot<ID, AggregateId> findLatestSnapshotFor(AID aggregateId, Event.SequenceNumber beforeGivenEventSequenceNumber);
    Snapshot<ID, AggregateId> findLatestSnapshotFor(AID aggregateId, Instant beforeGivenPointInTime);
    Snapshot<ID, AggregateId> findFirstSnapshotFor(AID aggregateId, Instant afterGivenPointInTime);
    void addNewSnapshot(Snapshot<ID, AggregateId> snapshot);
}
