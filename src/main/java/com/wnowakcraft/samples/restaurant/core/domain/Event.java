package com.wnowakcraft.samples.restaurant.core.domain;

import com.wnowakcraft.samples.restaurant.core.domain.Aggregate.AggregateId;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

import static com.wnowakcraft.preconditions.Preconditions.requireThat;
import static lombok.AccessLevel.PRIVATE;

public interface Event<ID extends AggregateId> {
    ID getConcernedAggregateId();
    SequenceNumber getSequenceNumber();
    Instant getGeneratedOn();

    @RequiredArgsConstructor(access = PRIVATE)
    class SequenceNumber {
        public static SequenceNumber NOT_ASSIGNED = new SequenceNumber(-1);
        public final long sequenceNumber;

        public static SequenceNumber of(long sequenceNumber) {
            requireThat(sequenceNumber >= 0, "sequenceNumber needs to be a positive integer");
            return new SequenceNumber(sequenceNumber);
        }

        public SequenceNumber next() {
            return new SequenceNumber(sequenceNumber + 1);
        }
    }
}
