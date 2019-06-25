package com.wnowakcraft.samples.restaurant.core.domain;

import java.util.Collection;

public interface Aggregate<ID extends Aggregate.AggregateId, E extends Event> {
    ID getId();
    Collection<E> getChanges();

    abstract class AggregateId extends DomainBoundBusinessId {
        private static final char AGGREGATE_TYPE_SYMBOL = 'A';


        protected AggregateId(String domainName, String domainObjectName) {
            super(domainName, domainObjectName, AGGREGATE_TYPE_SYMBOL);
        }

        protected AggregateId(String aggregateId, String domainName, String domainObjectName) {
            super(aggregateId, domainName, domainObjectName, AGGREGATE_TYPE_SYMBOL);
        }
    }
}