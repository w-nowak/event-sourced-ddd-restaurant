package com.wnowakcraft.samples.restaurant.order.infrastructure.kafka;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.wnowakcraft.samples.restaurant.common.infrastructure.data.message.MessageEnvelope;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class ProtobuffSerializer implements Serializer<Message> {
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String s, Message message) {
        return serialize(message);
    }

    private byte[] serialize(Message message) {
        return MessageEnvelope.newBuilder()
                .setPayloadTypeQualifiedName(message.getClass().getName())
                .setPayload(Any.newBuilder().setValue(message.toByteString()).build())
                .build()
                .toByteArray();
    }

    @Override
    public byte[] serialize(String topic, Headers headers, Message data) {
        return serialize(data);
    }

    @Override
    public void close() {
    }
}
