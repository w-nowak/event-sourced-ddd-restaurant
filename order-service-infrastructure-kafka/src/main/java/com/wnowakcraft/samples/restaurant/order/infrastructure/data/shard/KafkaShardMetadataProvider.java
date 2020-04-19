package com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard;

import com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard.ShardManager.ShardRef;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.store.KafkaConsumerFactory;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = { @Inject})
public class KafkaShardMetadataProvider implements ShardMetadataProvider {
    @NonNull private final KafkaConsumerFactory kafkaConsumerFactory;

    @Override
    public CompletableFuture<Long> getLastRecordOffsetFor(ShardRef shardRef) {
        return kafkaConsumerFactory
                .doConsumerRead(
                        shardRef,
                        this::getCurrentOffsetForConsumer
                )
                .handle((offset, error) -> handleOffsetResult(offset, error, shardRef));
    }

    private long getCurrentOffsetForConsumer(Consumer<String, Object> kafkaConsumer) {
        return kafkaConsumer.endOffsets(kafkaConsumer.assignment()).values().iterator().next() - 1;
    }

    private long handleOffsetResult(long offset, Throwable error, ShardRef shardRef) {
        if(error == null) {
            return offset;
        }

        log.warn("Couldn't obtain current offset for topic: {}, shardId: {}. Reason: {}",
                shardRef.topicName, shardRef.shardId, error.getMessage(), error);

        return SHARD_OFFSET_UNKNOWN;
    }
}
