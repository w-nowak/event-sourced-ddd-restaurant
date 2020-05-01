package com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard;

import com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard.ShardManager.ShardRef;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.store.KafkaConsumerFactory;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = { @Inject})
public class KafkaShardMetadataProvider implements ShardMetadataProvider {
    @NonNull private final KafkaConsumerFactory kafkaConsumerFactory;

    @Override
    public CompletableFuture<Long> getLastRecordOffsetForShard(ShardRef shardRef) {
        return kafkaConsumerFactory
                .doConsumerRead(
                        shardRef,
                        this::getCurrentOffsetForConsumer
                )
                .handle((offset, error) -> handleOffsetResult(offset, error, shardRef));
    }


    private long getCurrentOffsetForConsumer(Consumer<String, Object> consumer) {
        return consumer.endOffsets(consumer.assignment()).values().iterator().next() - 1;
    }

    private long handleOffsetResult(long offset, Throwable error, ShardRef shardRef) {
        if(error == null) {
            return offset;
        }

        log.warn("Couldn't obtain an offset for a topic: {}, shardId: {}. Reason: {}",
                shardRef.topicName, shardRef.shardId, error.getMessage(), error);

        return SHARD_OFFSET_UNKNOWN;
    }

    @Override
    public CompletableFuture<Long> getLatestOffsetFor(ShardRef shardRef, Instant beforeGivenPointInTime) {
        return kafkaConsumerFactory
                .doConsumerRead(
                        shardRef,
                        consumer -> latestOffsetFor(consumer, shardRef, beforeGivenPointInTime)
                )
                .handle((offset, error) -> handleOffsetResult(offset, error, shardRef));
    }

    private long latestOffsetFor(Consumer<String, Object> consumer, ShardRef shardRef, Instant beforeGivenPointInTime) {
        return offsetFor(consumer, shardRef, beforeGivenPointInTime)
                .map(offset -> offset - 1) //minus one as Kafka returns first record at or after given point in time, we need last one before then
                .orElse(ShardMetadataProvider.SHARD_OFFSET_UNKNOWN);
    }

    private Optional<Long> offsetFor(Consumer<String, Object> consumer, ShardRef shardRef, Instant atGivenPointInTime) {
        return consumer
                .offsetsForTimes(Map.of(KafkaPartition.of(shardRef), atGivenPointInTime.getEpochSecond()))
                .values().stream()
                .filter(Objects::nonNull)
                .findFirst()
                .map(OffsetAndTimestamp::offset);
    }

    @Override
    public CompletableFuture<Long> getFirstOffsetFor(ShardRef shardRef, Instant afterOrEqualGivenPointInTime) {
        return kafkaConsumerFactory
                .doConsumerRead(
                        shardRef,
                        consumer -> firstOffsetFor(consumer, shardRef, afterOrEqualGivenPointInTime)
                )
                .handle((offset, error) -> handleOffsetResult(offset, error, shardRef));
    }

    private long firstOffsetFor(Consumer<String, Object> consumer, ShardRef shardRef, Instant afterOrEqualGivenPointInTime) {
        return offsetFor(consumer, shardRef, afterOrEqualGivenPointInTime)
                .orElse(ShardMetadataProvider.SHARD_OFFSET_UNKNOWN);
    }
}
