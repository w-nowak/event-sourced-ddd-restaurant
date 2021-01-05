package com.wnowakcraft.samples.restaurant.core.domain.model;

import com.wnowakcraft.samples.restaurant.core.domain.model.snapshot.Snapshottable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collection;
import java.util.LinkedList;

import static com.wnowakcraft.preconditions.Preconditions.*;
import static java.lang.String.format;
import static java.util.List.copyOf;
import static java.util.List.of;

@ToString
@EqualsAndHashCode
public abstract class AbstractAggregate<ID extends Aggregate.Id, E extends Event<ID>, S extends Snapshot<? extends Snapshot.Id, ID>>
        implements Aggregate<ID, E>, WithUpdatableVersion, Snapshottable<S> {
    private final ID aggregateId;
    protected final Collection<E> changes;
    private Version version;

    protected AbstractAggregate(E creatingEvent) {
        requireNonNull(creatingEvent, "creatingEvent");
        this.aggregateId = creatingEvent.getConcernedAggregateId();
        this.version = Version.NONE;

        var events = of(creatingEvent);
        this.changes = new LinkedList<>(events);
        applyAll(events);
    }

    protected AbstractAggregate(Collection<? extends E> events, Class<? extends E> creatingEventClass, Version version) {
        requireNonEmpty(events, "events");
        this.version = requireNonNull(version, "version");
        requireThat(version != Version.NONE, "An aggregate version needs to be specified.");

        final var creatingEvent = events.iterator().next();
        requireThat(creatingEventClass == creatingEvent.getClass(),
                format("Invalid event stream. Fist event needs to be of type %s", creatingEventClass.getSimpleName()));

        this.aggregateId = creatingEvent.getConcernedAggregateId();

        applyAll(copyOf(events));
        this.changes = new LinkedList<>();
    }

    protected AbstractAggregate(S snapshot, Collection<? extends E> events, Version version) {
        requireNonNull(snapshot, "snapshot");
        requireNonNull(events, "events");
        this.version = requireNonNull(version, "version");
        requireThat(version != Version.NONE, "An aggregate version needs to be specified.");

        this.aggregateId = snapshot.getAggregateId();

        restoreFrom(snapshot);
        applyAll(copyOf(events));
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
        return copyOf(changes);
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public void updateVersionTo(Version version) {
        this.version = version;
    }
}
