package com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard;

import com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard.ShardManager.ShardingStrategy;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;

import static com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard.ShardManager.ShardingStrategy.ShardingType.EVENT_STORE;

@ShardingStrategy(EVENT_STORE)
@RequiredArgsConstructor(onConstructor_ = { @Inject})
public class EventStoreShardManager implements ShardManager {
    private static final int SINGLE_SHARD_ID = 0;

    @Override
    public ShardRef getShardForBusinessIdOf(Aggregate.Id aggregateId) {
        return new ShardRef(getTopicNameFor(aggregateId), SINGLE_SHARD_ID);
    }

    private String getTopicNameFor(Aggregate.Id aggregateId)
    {
        return aggregateId.getValue();
    }
}
