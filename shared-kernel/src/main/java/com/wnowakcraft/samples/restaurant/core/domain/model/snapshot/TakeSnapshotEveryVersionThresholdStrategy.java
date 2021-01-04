package com.wnowakcraft.samples.restaurant.core.domain.model.snapshot;

import com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate;
import com.wnowakcraft.samples.restaurant.core.domain.model.Event;
import com.wnowakcraft.samples.restaurant.core.domain.model.Snapshot;
import com.wnowakcraft.samples.restaurant.core.domain.model.WithUpdatableVersion;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.function.Function;

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
    public boolean shouldTakeNewSnapshot(A aggregate, Function<A, Optional<S>> previousSnapshotProvider) {
        requireNonNull(aggregate, "aggregate");
        requireNonNull(previousSnapshotProvider, "previousSnapshotProvider");

        return aggregate.getVersion().number >=
                snapshottedAggregateVersionUsing(previousSnapshotProvider, aggregate) + versionThreshold;
    }

    private long snapshottedAggregateVersionUsing(Function<A, Optional<S>> previousSnapshotProvider, A aggregate) {
        return previousSnapshotProvider
                .apply(aggregate)
                .map(snapshot -> snapshot.getAggregateVersion().number)
                .orElse(0L);
    }
}
