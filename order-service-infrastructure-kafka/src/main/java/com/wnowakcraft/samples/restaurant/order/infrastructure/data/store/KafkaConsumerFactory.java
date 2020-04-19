package com.wnowakcraft.samples.restaurant.order.infrastructure.data.store;

import com.wnowakcraft.samples.restaurant.order.infrastructure.data.shard.ShardManager.ShardRef;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import javax.inject.Inject;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.Collections.singleton;

@RequiredArgsConstructor(onConstructor_ = { @Inject})
public class KafkaConsumerFactory {
    @NonNull private final KafkaBrokerConfig kafkaBrokerConfig;

    public <V> Consumer<String, V> createConsumer() {
        Properties consumerProperties = new Properties();
        return new KafkaConsumer<>(consumerProperties);
    }

    public <V> Consumer<String, V> createConsumerFor(ShardRef shardRef) {
        Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokerConfig.getBootstrapServers());
        //consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "myGroup");
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "com.wnowakcraft.samples.restaurant.order.infrastructure.kafka.ProtobuffDeserializer");

        var kafkaConsumer = new KafkaConsumer<String, V>(consumerProperties);
        kafkaConsumer.assign(singleton(new TopicPartition(shardRef.topicName, shardRef.shardId)));

        return kafkaConsumer;
    }

    public <V, R> CompletableFuture<R> doConsumerRead(ShardRef shardRef, Function<Consumer<String, V>, R> readFunction) {
        Consumer<String, V> kafkaConsumer = createConsumerFor(shardRef);

        return CompletableFuture
                .supplyAsync(() -> readFunction.apply(kafkaConsumer))
                .whenCompleteAsync((r, e) -> kafkaConsumer.close());
    }
}
