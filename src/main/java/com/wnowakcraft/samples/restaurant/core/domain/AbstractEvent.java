package com.wnowakcraft.samples.restaurant.core.domain;

import com.wnowakcraft.samples.restaurant.core.domain.Aggregate.AggregateId;
import lombok.*;

import java.time.Instant;

import static com.wnowakcraft.samples.restaurant.core.utils.ApplicationTime.instantNow;
import static lombok.AccessLevel.PROTECTED;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor(access = PROTECTED)
public abstract class AbstractEvent<ID extends AggregateId> implements Event<ID> {
    @NonNull private final ID concernedAggregateId;
    @NonNull private final SequenceNumber sequenceNumber;
    @NonNull private final Instant generatedOn;

    protected AbstractEvent(ID concernedAggregateId) {
        this(concernedAggregateId, SequenceNumber.NOT_ASSIGNED, instantNow());
    }
}
