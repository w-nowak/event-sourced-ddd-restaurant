package com.wnowakcraft.samples.restaurant.core.domain;

import com.wnowakcraft.preconditions.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collection;

import static lombok.AccessLevel.PRIVATE;

public interface Aggregate<ID extends Aggregate.AggregateId, E extends Event> {
    ID getId();
    Collection<E> getChanges();
    Version getVersion();

    abstract class AggregateId extends DomainBoundBusinessId {
        private static final char AGGREGATE_TYPE_SYMBOL = 'A';


        protected AggregateId(String domainName, String domainObjectName) {
            super(domainName, domainObjectName, AGGREGATE_TYPE_SYMBOL);
        }

        protected AggregateId(String aggregateId, String domainName, String domainObjectName) {
            super(aggregateId, domainName, domainObjectName, AGGREGATE_TYPE_SYMBOL);
        }
    }

    @Getter
    @AllArgsConstructor(access = PRIVATE)
    final class Version {
        public final static Version NONE = new Version(0);

        private final long number;

        public static Version of(long number) {
            Preconditions.requireThat(number > 0, "The version number needs to be positive integer");

            return new Version(number);
        }
    }
}