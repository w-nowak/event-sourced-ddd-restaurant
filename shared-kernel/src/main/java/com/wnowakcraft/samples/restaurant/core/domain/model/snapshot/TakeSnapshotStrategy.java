package com.wnowakcraft.samples.restaurant.core.domain.model.snapshot;

import com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate;
import com.wnowakcraft.samples.restaurant.core.domain.model.Event;
import com.wnowakcraft.samples.restaurant.core.domain.model.Snapshot;
import com.wnowakcraft.samples.restaurant.core.domain.model.WithUpdatableVersion;

import javax.annotation.Nullable;

public interface TakeSnapshotStrategy <
        E extends Event<?>,
        A extends Aggregate<ID, E> & WithUpdatableVersion,
        S extends Snapshot<? extends Snapshot.Id, ID>,
        ID extends Aggregate.Id> {
    boolean shouldTakeNewSnapshot(A aggregate, @Nullable S previousSnapshot);
}
