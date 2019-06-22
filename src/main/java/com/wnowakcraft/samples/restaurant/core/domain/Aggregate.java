package com.wnowakcraft.samples.restaurant.core.domain;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.Collection;

public interface Aggregate<ID extends Aggregate.AggregateId, E extends Event> {
    ID getAggregateId();
    Collection<E> getChanges();

    @Getter
    @RequiredArgsConstructor
    abstract class AggregateId<T extends Serializable & Comparable<T>> implements Id<T> {
        @NonNull private final T id;
    }
}