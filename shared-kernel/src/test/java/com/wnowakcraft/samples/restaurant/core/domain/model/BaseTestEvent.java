package com.wnowakcraft.samples.restaurant.core.domain.model;

import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
public abstract class BaseTestEvent implements TestEvent {
    public static final TestAggregateId AGGREGATE_ID = TestAggregateId.DEFAULT_ONE;
    public static final SequenceNumber SEQUENCE_NUMBER = SequenceNumber.of(5);
    public static final Instant GENERATED_ON = Instant.now();

    private final TestAggregateId aggregateId;
    private final SequenceNumber sequenceNumber;
    private final Instant generatedOn;

    @Override
    public TestAggregateId getConcernedAggregateId() {
        return aggregateId;
    }

    @Override
    public SequenceNumber getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public Instant getGeneratedOn() {
        return generatedOn;
    }
}
