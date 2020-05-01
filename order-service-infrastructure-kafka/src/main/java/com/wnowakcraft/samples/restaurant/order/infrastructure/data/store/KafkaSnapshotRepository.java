package com.wnowakcraft.samples.restaurant.order.infrastructure.data.store;

import com.google.common.collect.Iterables;
import com.google.protobuf.Message;
import com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate;
import com.wnowakcraft.samples.restaurant.core.domain.model.Event;
import com.wnowakcraft.samples.restaurant.core.domain.model.Snapshot;
import com.wnowakcraft.samples.restaurant.core.domain.model.SnapshotRepository;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard.ShardManager;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard.ShardManager.ShardRef;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard.ShardMetadataProvider;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.store.RecordSearchStrategyFactory.SearchStrategy;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.google.common.collect.Iterables.getLast;
import static com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard.ShardManager.ShardingStrategy.ShardingType.SNAPSHOT_STORE;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = { @Inject })
public class KafkaSnapshotRepository<S extends Snapshot<? extends Snapshot.Id, AID>, AID extends Aggregate.Id> implements SnapshotRepository<S, AID> {
    private static final short READ_ONE_RECORD = 1;

    private S nullWhenEmpty = null;
    @NonNull private final KafkaConsumerFactory consumerFactory;
    @ShardManager.ShardingStrategy(SNAPSHOT_STORE) @NonNull private ShardManager shardManager;
    @NonNull private final ShardMetadataProvider shardMetadataProvider;
    @NonNull private final KafkaRecordReader<S> kafkaRecordReader;
    @NonNull private final RecordSearchStrategyFactory recordSearchStrategyFactory;

    @Override
    public Optional<S> findLatestSnapshotFor(AID aggregateId) {
        return findAppropriateSnapshotFor(aggregateId, shardMetadataProvider::getLastRecordOffsetForShard, Iterables::getFirst, nullWhenEmpty);
    }

    private Optional<S> findAppropriateSnapshotFor(AID aggregateId, Function<ShardRef, CompletableFuture<Long>> snapshotOffsetFutureProviderForShardRef,
                                                   BiFunction<Collection<S>, S, S> snapshotExtractor, S defaultSnapshotWhenNotFound) {
        var shardRef = shardManager.getShardForBusinessIdOf(aggregateId);
        var shardOffsetFuture = snapshotOffsetFutureProviderForShardRef.apply(shardRef);
        var shardOffset = offsetOf(shardOffsetFuture);

        if(shardOffset == ShardMetadataProvider.SHARD_OFFSET_UNKNOWN) {
            return Optional.empty();
        }

        S lastSnapshot = null;
        try(Consumer<String, Message> consumer = consumerFactory.createConsumerFor(shardRef)) {
            consumer.assignment().forEach(assignment -> consumer.seek(assignment, shardOffset));

            lastSnapshot = snapshotExtractor.apply(kafkaRecordReader.readRecordsFrom(consumer, aggregateId), defaultSnapshotWhenNotFound);
        }

        return Optional.ofNullable(lastSnapshot);
    }

    private long offsetOf(CompletableFuture<Long> currentOffsetFuture) {
        var offset = 0L;

        try {
            offset = currentOffsetFuture.get();
        } catch (InterruptedException | ExecutionException ex) {
            log.error(ex.getMessage(), ex);
        }

        return offset;
    }

    @Override
    public Optional<S> findLatestSnapshotFor(AID aggregateId, Event.SequenceNumber beforeGivenEventSequenceNumber) {
        var shardRef = shardManager.getShardForBusinessIdOf(aggregateId);
        var shardEndOffsetFuture = shardMetadataProvider.getLastRecordOffsetForShard(shardRef);
        var shardEndOffset = offsetOf(shardEndOffsetFuture);

        if(shardEndOffset == ShardMetadataProvider.SHARD_OFFSET_UNKNOWN) {
            return Optional.empty();
        }

        S lastFoundSnapshotBeforeGivenSequenceNumber = null;
        try(Consumer<String, Message> consumer = consumerFactory.createConsumerFor(shardRef)) {

            consumer.assignment().forEach(assignment -> consumer.seek(assignment, shardEndOffset));
            var lastSnapshot = getLast(kafkaRecordReader.readLimitedNumberOfRecordsFrom(consumer, aggregateId, READ_ONE_RECORD), nullWhenEmpty);

            if(lastSnapshot != null && lastSnapshot.getAggregateVersion().number < beforeGivenEventSequenceNumber.number) {
                return Optional.of(lastSnapshot);
            }

            SearchStrategy searchStrategy = recordSearchStrategyFactory.getSearchStrategyFor(0, shardEndOffset);
            var nextOffsetToTry = searchStrategy.getNextOffsetToTry();

            while (nextOffsetToTry != SearchStrategy.NO_FURTHER_OFFSET_AVAILABLE) {
                final var currentOffset = nextOffsetToTry;
                consumer.assignment().forEach(assignment -> consumer.seek(assignment, currentOffset));
                var foundSnapshots = kafkaRecordReader.readLimitedNumberOfRecordsFrom(consumer, aggregateId, READ_ONE_RECORD);

                nextOffsetToTry = SearchStrategy.NO_FURTHER_OFFSET_AVAILABLE;

                if(!foundSnapshots.isEmpty()) {
                    var foundSnapshot = foundSnapshots.iterator().next();

                    if(foundSnapshot.getAggregateVersion().number < beforeGivenEventSequenceNumber.number) {
                        lastFoundSnapshotBeforeGivenSequenceNumber = foundSnapshot;
                        searchStrategy = searchStrategy.searchUpper();
                    } else {
                        searchStrategy = searchStrategy.searchLower();
                    }

                    nextOffsetToTry = searchStrategy.getNextOffsetToTry();
                }
            }
        }

        return Optional.ofNullable(lastFoundSnapshotBeforeGivenSequenceNumber);
    }

    @Override
    public Optional<S> findLatestSnapshotFor(AID aggregateId, Instant beforeGivenPointInTime) {
        Function<ShardRef, CompletableFuture<Long>> latestOffsetBeforeGivenPointInTimeProvider =
                (shardRef) -> shardMetadataProvider.getLatestOffsetFor(shardRef, beforeGivenPointInTime);

        return findAppropriateSnapshotFor(aggregateId, latestOffsetBeforeGivenPointInTimeProvider, Iterables::getFirst, nullWhenEmpty);
    }

    @Override
    public Optional<S> findFirstSnapshotFor(AID aggregateId, Instant afterGivenPointInTime) {
        return Optional.empty();
    }

    @Override
    public void addNewSnapshot(S snapshot) {

    }
}
