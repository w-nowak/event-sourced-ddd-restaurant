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
    public ID getId() {
        return aggregateId;
    }

    @Override
    public Collection<E> getChanges() {
        return List.copyOf(changes);
    }

}
