package com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard;

import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface ShardMetadataProvider {
    long SHARD_OFFSET_UNKNOWN = -1;
    CompletableFuture<Long> getLastRecordOffsetForShard(ShardManager.ShardRef shardRef);
    CompletableFuture<Long> getLatestOffsetFor(ShardManager.ShardRef shardRef, Instant beforeGivenPointInTime);
    CompletableFuture<Long> getFirstOffsetFor(ShardManager.ShardRef shardRef, Instant afterOrEqualGivenPointInTime);

    static long offsetOf(CompletableFuture<Long> currentOffsetFuture, long defaultOffset) {
        var offset = defaultOffset;

        try {
            offset = currentOffsetFuture.get();
        } catch (InterruptedException | ExecutionException ex) {
            LoggerFactory.getLogger(ShardMetadataProvider.class).error(ex.getMessage(), ex);
        }

        return offset;
    }
}
