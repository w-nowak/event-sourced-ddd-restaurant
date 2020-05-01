package com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public interface ShardMetadataProvider {
    long SHARD_OFFSET_UNKNOWN = -1;
    CompletableFuture<Long> getLastRecordOffsetForShard(ShardManager.ShardRef shardRef);
    CompletableFuture<Long> getLatestOffsetFor(ShardManager.ShardRef shardRef, Instant beforeGivenPointInTime);
}
