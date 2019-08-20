package com.wnowakcraft.samples.restaurant.core.domain.model;

import java.time.Instant;
import java.util.Optional;

public interface SnapshotRepository<S extends Snapshot<? extends Snapshot.Id, AID>, AID extends Aggregate.Id> {
    Optional<S> findLatestSnapshotFor(AID aggregateId);
    Optional<S> findLatestSnapshotFor(AID aggregateId, Event.SequenceNumber beforeGivenEventSequenceNumber);
    Optional<S> findLatestSnapshotFor(AID aggregateId, Instant beforeGivenPointInTime);
    Optional<S> findFirstSnapshotFor(AID aggregateId, Instant afterGivenPointInTime);
    void addNewSnapshot(S snapshot);
}
