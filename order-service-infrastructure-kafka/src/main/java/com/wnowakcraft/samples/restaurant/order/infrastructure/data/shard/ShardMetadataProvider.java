package com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ShardMetadataProvider {
    long SHARD_OFFSET_UNKNOWN = -1;
    CompletableFuture<Long> getLastRecordOffsetFor(ShardManager.ShardRef shardRef);
}
