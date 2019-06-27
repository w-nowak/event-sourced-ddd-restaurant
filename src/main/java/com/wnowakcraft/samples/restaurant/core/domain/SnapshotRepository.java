package com.wnowakcraft.samples.restaurant.core.domain;

import com.wnowakcraft.samples.restaurant.core.domain.Aggregate.AggregateId;
import com.wnowakcraft.samples.restaurant.core.domain.Snapshot.SnapshotId;

import java.time.Instant;
import java.util.Optional;

public interface SnapshotRepository<S extends Snapshot<? extends SnapshotId, AID>, AID extends AggregateId> {
    Optional<S> findLatestSnapshotFor(AID aggregateId);
    Optional<S> findLatestSnapshotFor(AID aggregateId, Event.SequenceNumber beforeGivenEventSequenceNumber);
    Optional<S> findLatestSnapshotFor(AID aggregateId, Instant beforeGivenPointInTime);
    Optional<S> findFirstSnapshotFor(AID aggregateId, Instant afterGivenPointInTime);
    void addNewSnapshot(S snapshot);
}
