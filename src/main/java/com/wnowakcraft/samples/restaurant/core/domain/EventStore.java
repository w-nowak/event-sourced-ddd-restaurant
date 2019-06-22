package com.wnowakcraft.samples.restaurant.core.domain;

import com.wnowakcraft.samples.restaurant.core.domain.Aggregate.AggregateId;
import com.wnowakcraft.samples.restaurant.core.domain.Event.SequenceNumber;

import java.util.Collection;

public interface EventStore<E extends Event, A extends Aggregate<ID, E>, ID extends AggregateId> {
    Collection<E> loadAllEventsFor(ID aggregateId);
    Collection<E> loadEventsFor(ID aggregateId, SequenceNumber startingFromSequenceNumber);
    void append(ID aggregateId, Collection<E> events);
}
