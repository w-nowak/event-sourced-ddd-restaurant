package com.wnowakcraft.samples.restaurant.core.domain.model;

import java.time.Instant;

public interface Snapshot <ID extends Snapshot.Id, AID extends Aggregate.Id> {
    ID getSnapshotId();
    AID getAggregateId();
    Instant getCreationDate();
    Aggregate.Version getAggregateVersion();

    abstract class Id extends DomainBoundBusinessId {
        private static final char TYPE_SYMBOL = 'S';


        protected Id(String domainName, String domainObjectName) {
            super(domainName, domainObjectName, TYPE_SYMBOL);
        }

        protected Id(String snapshotId, String domainName, String domainObjectName) {
            super(snapshotId, domainName, domainObjectName, TYPE_SYMBOL);
        }
    }
}
