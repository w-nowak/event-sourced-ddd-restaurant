package com.wnowakcraft.samples.restaurant.core.domain.model;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.time.Instant;

import static com.wnowakcraft.preconditions.Preconditions.requireThat;
import static lombok.AccessLevel.PRIVATE;

public interface Event<ID extends Aggregate.Id> {
    ID getConcernedAggregateId();
    SequenceNumber getSequenceNumber();
    Instant getGeneratedOn();

    @ToString
    @EqualsAndHashCode
    @RequiredArgsConstructor(access = PRIVATE)
    class SequenceNumber {
        public static SequenceNumber NOT_ASSIGNED = new SequenceNumber(-1);
        public final long number;

        public static SequenceNumber of(long sequenceNumber) {
            requireThat(sequenceNumber > NOT_ASSIGNED.number, "sequenceNumber needs to be a positive integer");
            return new SequenceNumber(sequenceNumber);
        }

        public SequenceNumber next() {
            return new SequenceNumber(number + 1);
        }
    }
}
