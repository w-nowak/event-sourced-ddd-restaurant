package com.wnowakcraft.samples.restaurant.order.infrastructure.data.store;

import com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard.ShardManager.ShardRef;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;
import static java.lang.String.format;

public class ConcurrentLogAppendingException extends RuntimeException {
    public final ShardRef shardRef;
    public final long currentOffset;
    public final long expectedOffset;

    public ConcurrentLogAppendingException(ShardRef shardRef, long currentOffset, long expectedOffset) {
        super(
                format(
                        "Concurrent modification of topic: %s, shardId: %d. Current offset: %d, expected offset: %d",
                        requireNonNull(shardRef, "shardRef").topicName,
                        shardRef.shardId,
                        currentOffset,
                        expectedOffset
                )
        );

        this.shardRef = shardRef;
        this.currentOffset = currentOffset;
        this.expectedOffset = expectedOffset;
    }

}
