package com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard;

import com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard.ShardManager.ShardRef;
import org.apache.kafka.common.TopicPartition;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;

public class KafkaPartition {
    public static TopicPartition of(ShardRef shardRef) {
        requireNonNull(shardRef, "shardRef");

        return new TopicPartition(shardRef.topicName, shardRef.shardId);
    }
}
