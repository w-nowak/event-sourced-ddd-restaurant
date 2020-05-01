package com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard;

import com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard.ShardManager.ShardingStrategy;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;

import static com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard.ShardManager.ShardingStrategy.ShardingType.SNAPSHOT_STORE;

@ShardingStrategy(SNAPSHOT_STORE)
@RequiredArgsConstructor(onConstructor_ = { @Inject})
public class SnapshotStoreShardManager implements ShardManager {
    private static final int SINGLE_SHARD_ID = 0;
    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    @Override
    public ShardRef getShardForBusinessIdOf(Aggregate.Id aggregateId) {
        return new ShardRef(getSnapshotTopicNameFor(aggregateId), SINGLE_SHARD_ID);
    }

    private String getSnapshotTopicNameFor(Aggregate.Id aggregateId)
    {
        return aggregateId.getValue() + SNAPSHOT_SUFFIX;
    }
}
