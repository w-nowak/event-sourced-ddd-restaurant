package com.wnowakcraft.samples.restaurant.core.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.time.Instant;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;
import static com.wnowakcraft.preconditions.Preconditions.requireThat;
import static com.wnowakcraft.samples.restaurant.core.utils.ApplicationTime.instantNow;

@Getter
@ToString
@EqualsAndHashCode
public abstract class AbstractEvent<ID extends Aggregate.Id> implements Event<ID> {
    @NonNull private final ID concernedAggregateId;
    @NonNull private final SequenceNumber sequenceNumber;
    @NonNull private final Instant generatedOn;

    /**
     * Constructor used to create a new {@link AbstractEvent} instance
     * @param concernedAggregateId id of an aggregate which is source of this event
     */
    protected AbstractEvent(ID concernedAggregateId) {
        this.concernedAggregateId = requireNonNull(concernedAggregateId, "concernedAggregateId");
        this.sequenceNumber =  SequenceNumber.NOT_ASSIGNED;
        this.generatedOn = instantNow();
    }

    /**
     * Constructor used to recreate a {@link AbstractEvent} instance - in contrary to creating a brand new event instance
     * @param concernedAggregateId id of an aggregate which is source of this event
     * @param sequenceNumber a sequence number of the event.
     *                       A sequence number needs to be assigned - otherwise  {@link IllegalArgumentException} is thrown
     * @param generatedOn instant when the event was generated
     */
    protected AbstractEvent(ID concernedAggregateId, SequenceNumber sequenceNumber, Instant generatedOn) {
        this.concernedAggregateId = requireNonNull(concernedAggregateId, "concernedAggregateId");
        this.sequenceNumber = requireNonNull(sequenceNumber, "sequenceNumber");
        this.generatedOn = requireNonNull(generatedOn, "generatedOn");

        requireThat(sequenceNumber != SequenceNumber.NOT_ASSIGNED, "sequenceNumber needs to be assigned");
    }
}
