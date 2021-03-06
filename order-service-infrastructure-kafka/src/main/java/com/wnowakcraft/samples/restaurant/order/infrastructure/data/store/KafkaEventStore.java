package com.wnowakcraft.samples.restaurant.order.infrastructure.data.store;

import com.google.protobuf.Message;
import com.wnowakcraft.samples.restaurant.core.domain.model.Aggregate;
import com.wnowakcraft.samples.restaurant.core.domain.model.Event;
import com.wnowakcraft.samples.restaurant.core.domain.model.EventStore;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.conversion.MessageConverter;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard.ShardManager;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard.ShardMetadataProvider;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.collect.Iterables.getLast;
import static java.util.List.copyOf;
import static lombok.AccessLevel.PRIVATE;

@Slf4j
@RequiredArgsConstructor
public class KafkaEventStore<E extends Event<?>, A extends Aggregate<ID, E>, ID extends Aggregate.Id> implements EventStore<E, A, ID> {
    private static final long DEFAULT_SHARD_OFFSET = 0;
    @NonNull private final MessageConverter<E, Message> eventMessageConverter;
    @NonNull private final KafkaConsumerFactory consumerFactory;
    @NonNull private final KafkaProducerFactory producerFactory;
    @NonNull private final ShardMetadataProvider shardMetadataProvider;
    @NonNull private final ShardManager shardManager;
    private Producer<String, Message> producer;

    @PostConstruct
    public void initializeKafkaProducer() {
        producer = producerFactory.createProducer();
        //producer.initTransactions();
    }

    @PreDestroy
    private void destroyKafkaProducer() {
        if(producer != null) {
            producer.close();
        }
    }

    @Override
    public EventStream<E> loadAllEventsFor(ID businessId) {
        var shardRef = shardManager.getShardForBusinessIdOf(businessId);
        try(Consumer<String, Message> kafkaConsumer = consumerFactory.createConsumerFor(shardRef)) {
            kafkaConsumer.seekToBeginning(kafkaConsumer.assignment());

            Collection<E> readEvents = readEventsFrom(kafkaConsumer, businessId);
            return KafkaEventStream.of(readEvents);
        }
    }

    private Collection<E> readEventsFrom(Consumer<String, Message> consumer, ID businessId) {
        Collection<E> readEvents = new LinkedHashSet<>();
        ConsumerRecords<String, Message> readRecords;

        while(!(readRecords = consumer.poll(Duration.ofSeconds(1))).isEmpty()) {
            StreamSupport.stream(readRecords.spliterator(), false)
                    .filter(record -> businessId.getValue().equals(record.key()))
                    .map(record -> eventMessageConverter.convert(record.value(), record.offset()))
                    .forEachOrdered(readEvents::add);
        }
        return readEvents;
    }

    @Override
    public EventStream<E> loadEventsFor(ID businessId, Event.SequenceNumber sequenceNumber) {
        return readEventsStartingFrom(sequenceNumber.number, businessId);
    }

    private EventStream<E> readEventsStartingFrom(long offset, ID businessId) {
        var shardRef = shardManager.getShardForBusinessIdOf(businessId);
        try(Consumer<String, Message> consumer = consumerFactory.createConsumerFor(shardRef)) {
            consumer.assignment().forEach(assignment -> consumer.seek(assignment, offset));

            var readEvents = readEventsFrom(consumer, businessId);
            return KafkaEventStream.of(readEvents);
        }
    }

    @Override
    public EventStream<E> loadEventsFor(ID businessId, Aggregate.Version aggregateVersion) {
        return readEventsStartingFrom(aggregateVersion.number, businessId);
    }

    @Override
    public CompletableFuture<Aggregate.Version> append(ID businessId, Aggregate.Version aggregateVersion, Collection<E> events) {
        var shardRef = shardManager.getShardForBusinessIdOf(businessId);
        var shardCurrentOffsetFuture = shardMetadataProvider.getLastRecordOffsetForShard(shardRef);
        var outgoingRecords = createKafkaRecordsFor(events, shardRef, businessId);

        var expectedOffset = aggregateVersion.number;
        long currentOffset = ShardMetadataProvider.offsetOf(shardCurrentOffsetFuture, DEFAULT_SHARD_OFFSET);

        if(currentOffset > expectedOffset) {
            throw new ConcurrentLogAppendingException(shardRef, currentOffset, expectedOffset);
        }

        return sendAsync(outgoingRecords);
    }

    private CompletableFuture<Aggregate.Version> sendAsync(List<ProducerRecord<String, Message>> outgoingRecords) {
        return CompletableFuture.supplyAsync(() -> {
            RecordAppendingOffsetTrackerHandler recordOffsetTrackerHandler = new RecordAppendingOffsetTrackerHandler();
            outgoingRecords.forEach(record -> producer.send(
                    record,
                    recordOffsetTrackerHandler.getHandlerComposedWith(RecordAppendingLoggingHandler.getHandlerFor(record)))
            );

            return Aggregate.Version.of(recordOffsetTrackerHandler.getOffsetOfLatestAppendedRecord());
        });
    }

    private List<ProducerRecord<String, Message>> createKafkaRecordsFor(Collection<E> events, ShardManager.ShardRef shardRef, ID businessId) {
        return events.stream()
                .map(eventMessageConverter::convert)
                .map(message -> new ProducerRecord<>(shardRef.topicName, shardRef.shardId, businessId.getValue(), message))
                .collect(Collectors.toUnmodifiableList());
    }

    @Getter
    @RequiredArgsConstructor(access = PRIVATE)
    private static class KafkaEventStream<E extends Event<?>> implements EventStream<E> {
        private final Collection<E> events;
        private final Aggregate.Version version;


        static <E extends Event<?>> EventStream<E> of(Collection<E> events) {
            if(events.isEmpty()) {
                return emptyEventStream();
            }

            long offset = getLast(events).getSequenceNumber().number;
            return new KafkaEventStream<>(copyOf(events), Aggregate.Version.of(offset));
        }

        @SuppressWarnings("unchecked")
        private static <E extends Event<?>> EventStream<E> emptyEventStream() {
            return (EventStream<E>)EMPTY;
        }
    }
}
