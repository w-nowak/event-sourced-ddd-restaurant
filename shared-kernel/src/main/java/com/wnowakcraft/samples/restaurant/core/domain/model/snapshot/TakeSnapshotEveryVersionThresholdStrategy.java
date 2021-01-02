package com.wnowakcraft.samples.restaurant.core.domain.model.snapshot;

import com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate;
import com.wnowakcraft.samples.restaurant.core.domain.model.Event;
import com.wnowakcraft.samples.restaurant.core.domain.model.Snapshot;
import com.wnowakcraft.samples.restaurant.core.domain.model.WithUpdatableVersion;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nullable;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;

@RequiredArgsConstructor
public class TakeSnapshotEveryVersionThresholdStrategy<
        E extends Event<?>,
        A extends Aggregate<ID, E> & WithUpdatableVersion,
        S extends Snapshot<? extends Snapshot.Id, ID>,
        ID extends Aggregate.Id>
        implements TakeSnapshotStrategy<E, A, S, ID> {

    private final int versionThreshold;

    @Override
    public boolean shouldTakeNewSnapshot(A aggregate, @Nullable S previousSnapshot) {
        requireNonNull(aggregate, "aggregate");

        return aggregate.getVersion().number >= aggregateVersionFrom(previousSnapshot) + versionThreshold;
    }

    private long aggregateVersionFrom(S snapshot) {
        return snapshot != null ? snapshot.getAggregateVersion().number : 0;
    }
}
