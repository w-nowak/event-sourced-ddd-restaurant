package com.wnowakcraft.samples.restaurant.core.domain;

import com.wnowakcraft.samples.restaurant.core.domain.Aggregate.AggregateId;
import com.wnowakcraft.samples.restaurant.core.domain.Aggregate.Version;
import com.wnowakcraft.samples.restaurant.core.domain.Event.SequenceNumber;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Collections;

import static lombok.AccessLevel.PRIVATE;

public interface EventStore<E extends Event, A extends Aggregate<ID, E>, ID extends AggregateId> {
    EventStream<E> loadAllEventsFor(ID aggregateId);
    EventStream<E> loadEventsFor(ID aggregateId, SequenceNumber startingFromSequenceNumber);
    EventStream<E> loadEventsFor(ID aggregateId, Version whichFollowsAggregateVersion);
    void append(ID aggregateId, Version aggregateVersion, Collection<E> events);

    interface EventStream<E extends Event> {
        EventStream<? extends Event> EMPTY = new EmptyEventStream<>();
        Version getVersion();
        Collection<E> getEvents();
        default boolean isEmpty() {
            return this == EMPTY;
        }

        @RequiredArgsConstructor(access = PRIVATE)
        final class EmptyEventStream<E extends Event> implements EventStream<Event> {
            @Override
            public Version getVersion() {
                return Version.NONE;
            }
            @Override @SuppressWarnings("unchecked")
            public Collection<Event> getEvents() {
                return Collections.EMPTY_LIST;
            }
        }
    }
}
