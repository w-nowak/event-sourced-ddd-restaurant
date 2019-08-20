package com.wnowakcraft.samples.restaurant.core.domain.model;

import com.wnowakcraft.samples.restaurant.core.domain.model.Event.SequenceNumber;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Collections;

import static lombok.AccessLevel.PRIVATE;

public interface EventStore<E extends Event, A extends Aggregate<ID, E>, ID extends Aggregate.Id> {
    EventStream<E> loadAllEventsFor(ID aggregateId);
    EventStream<E> loadEventsFor(ID aggregateId, SequenceNumber startingFromSequenceNumber);
    EventStream<E> loadEventsFor(ID aggregateId, Aggregate.Version whichFollowsAggregateVersion);
    void append(ID aggregateId, Aggregate.Version aggregateVersion, Collection<E> events);

    interface EventStream<E extends Event> {
        EventStream<? extends Event> EMPTY = new EmptyEventStream<>();
        Aggregate.Version getVersion();
        Collection<E> getEvents();
        default boolean isEmpty() {
            return this == EMPTY;
        }

        @RequiredArgsConstructor(access = PRIVATE)
        final class EmptyEventStream<E extends Event> implements EventStream<Event> {
            @Override
            public Aggregate.Version getVersion() {
                return Aggregate.Version.NONE;
            }
            @Override @SuppressWarnings("unchecked")
            public Collection<Event> getEvents() {
                return Collections.EMPTY_LIST;
            }
        }
    }
}
