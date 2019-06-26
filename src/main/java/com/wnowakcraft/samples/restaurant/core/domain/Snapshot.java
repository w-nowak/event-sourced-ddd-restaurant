package com.wnowakcraft.samples.restaurant.core.domain;

import com.wnowakcraft.samples.restaurant.core.domain.Aggregate.AggregateId;

import java.time.Instant;

public interface Snapshot <ID extends Snapshot.SnapshotId, AID extends AggregateId> {
    ID getSnapshotId();
    AID getAggregateId();
    Instant getCreationDate();

    abstract class SnapshotId extends DomainBoundBusinessId {
        private static final char SNAPSHOT_TYPE_SYMBOL = 'S';


        protected SnapshotId(String domainName, String domainObjectName) {
            super(domainName, domainObjectName, SNAPSHOT_TYPE_SYMBOL);
        }

        protected SnapshotId(String snapshotId, String domainName, String domainObjectName) {
            super(snapshotId, domainName, domainObjectName, SNAPSHOT_TYPE_SYMBOL);
        }
    }
}
