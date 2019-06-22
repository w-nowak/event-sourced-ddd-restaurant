package com.wnowakcraft.samples.restaurant.core.domain;

import com.wnowakcraft.samples.restaurant.core.domain.Aggregate.AggregateId;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static com.wnowakcraft.preconditions.Preconditions.*;
import static java.lang.String.format;

public abstract class AbstractAggregate<ID extends AggregateId, E extends Event, S extends Snapshot>
        implements Aggregate<ID, E> {
    protected final ID aggregateId;
    protected final Collection<E> changes;

    protected AbstractAggregate(ID aggregateId, Collection<E> events) {
        this.aggregateId = requireNonNull(aggregateId, "aggregateId");
        requireNonEmpty(events, "events");

        applyAll(events);
        this.changes = new LinkedList<>();
    }

    protected AbstractAggregate(ID aggregateId, S snapshot, Collection<E> events) {
        this.aggregateId = requireNonNull(aggregateId, "aggregateId");

        applyAll(events);
        this.changes = new LinkedList<>();
    }

    protected abstract void applyAll(Collection<E> events);

    protected abstract void restoreFrom(S snapshot);

    @Override
    public ID getAggregateId() {
        return aggregateId;
    }

    @Override
    public Collection<E> getChanges() {
        return List.copyOf(changes);
    }

    public abstract static class PrefixedUuidAggregateId extends AggregateId<String> {
        private static final String SEPARATOR = "-";

        protected PrefixedUuidAggregateId(String domainName, String aggregateType) {
            super(
                    idPrefixOf(
                            requireNonEmpty(domainName, "domainName"),
                            requireNonEmpty(aggregateType, "aggregateType")
                    )
                    + SEPARATOR
                    + threeMostSignificantComponentsOf(UUID.randomUUID())
            );
        }

        protected PrefixedUuidAggregateId(String domainName, String aggregateType, String aggregateId) {
            super(
                    verifyAggregateIdCorrectness(
                            requireNonEmpty(domainName, "domainName"),
                            requireNonEmpty(aggregateType, "aggregateType"),
                            requireNonEmpty(aggregateId, "aggregateId")
                    ));
        }

        private static String verifyAggregateIdCorrectness(String domainName, String aggregateType, String aggregateId) {
            requireThat(
                    aggregateId.startsWith(idPrefixOf(domainName, aggregateType)),
                    format("aggregateId is not valid identifier for %s domain and %s aggregate", domainName, aggregateType)
            );

            return aggregateId;
        }

        private static String threeMostSignificantComponentsOf(UUID uuid) {
            final String uuidString = uuid.toString();
            return uuidString.substring(uuidString.indexOf(SEPARATOR, uuidString.indexOf(SEPARATOR)));
        }

        private static String idPrefixOf(String domainName, String aggregateType) {
            return domainName + SEPARATOR + aggregateType;
        }
    }
}
