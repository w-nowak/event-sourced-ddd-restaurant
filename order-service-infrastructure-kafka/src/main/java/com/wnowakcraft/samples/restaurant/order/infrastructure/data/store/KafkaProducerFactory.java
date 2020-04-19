package com.wnowakcraft.samples.restaurant.order.infrastructure.data.store;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;

import javax.inject.Inject;
import java.util.Properties;

@RequiredArgsConstructor(onConstructor_ = { @Inject})
public class KafkaProducerFactory {
    @NonNull private final KafkaBrokerConfig kafkaBrokerConfig;

    public <V> Producer<String, V> createProducer() {
        Properties producerProperties = new Properties();
        producerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokerConfig.getBootstrapServers());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "com.wnowakcraft.samples.restaurant.order.infrastructure.kafka.ProtobuffSerializer");
        //producerProperties.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "orderServiceInstance1");
        return new KafkaProducer<>(producerProperties);
    }
}
