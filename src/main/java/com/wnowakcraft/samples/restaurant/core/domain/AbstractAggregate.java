package com.wnowakcraft.samples.restaurant.core.domain;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static com.wnowakcraft.preconditions.Preconditions.requireNonEmpty;
import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;

@ToString
@EqualsAndHashCode
public abstract class AbstractAggregate<ID extends Aggregate.Id, E extends Event, S extends Snapshot>
        implements Aggregate<ID, E> {
    private final ID aggregateId;
    private final Version version;
    protected final Collection<E> changes;

    protected AbstractAggregate(ID aggregateId, Collection<E> events, Version version) {
        this.aggregateId = requireNonNull(aggregateId, "aggregateId");
        requireNonEmpty(events, "events");
        this.version = requireNonNull(version, "version");

        applyAll(events);
        this.changes = new LinkedList<>();
    }

    protected AbstractAggregate(ID aggregateId, S snapshot, Collection<E> events, Version version) {
        this.aggregateId = requireNonNull(aggregateId, "aggregateId");
        requireNonNull(snapshot, "snapshot");
        requireNonNull(events, "events");
        this.version = requireNonNull(version, "version");

        restoreFrom(snapshot);
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

    @Override
    public Version getVersion() {
        return version;
    }
}
