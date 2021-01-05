package com.wnowakcraft.samples.restaurant.core.domain.model.snapshot;

import com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate;
import com.wnowakcraft.samples.restaurant.core.domain.model.Snapshot;

public interface Snapshottable <S extends Snapshot<? extends Snapshot.Id, ? extends Aggregate.Id>> {
    S takeSnapshot();
}
