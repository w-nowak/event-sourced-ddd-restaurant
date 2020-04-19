package com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard;

import com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard.ShardManager.ShardingStrategy;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;

import static com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard.ShardManager.ShardingStrategy.ShardingType.EVENT_PUBLISHING;

@ShardingStrategy(EVENT_PUBLISHING)
@RequiredArgsConstructor(onConstructor_ = { @Inject})
public class EventPublishingShardManager implements ShardManager {
    private static final short MAX_PARTITION_NUMBER = 10;
    @Override
    public ShardRef getShardForBusinessIdOf(Aggregate.Id aggregateId) {
        return new ShardRef(getTopicNameFor(aggregateId), calculateShardNumberFor(aggregateId));
    }

    private String getTopicNameFor(Aggregate.Id aggregateId)
    {
        return aggregateId.domainName + "-" + aggregateId.domainObjectName;
    }

    private short calculateShardNumberFor(Aggregate.Id aggregateId) {
        return (short)(Math.abs(aggregateId.hashCode()) % MAX_PARTITION_NUMBER);
    }
}
