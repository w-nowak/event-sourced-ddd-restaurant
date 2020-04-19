package com.wnowakcraft.samples.restaurant.order.infrastructure.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.wnowakcraft.samples.restaurant.common.infrastructure.data.message.MessageEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import static java.lang.String.format;

@Slf4j
public class ProtobuffDeserializer implements Deserializer<Message> {
    private static final String PARSE_FROM_METHOD_NAME = "parseFrom";

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public Message deserialize(String s, byte[] bytes) {
        return tryDeserializeMessage(bytes);
    }

    private Message tryDeserializeMessage(byte[] bytes) {
        try {
            return deserializeMessage(bytes);
        } catch (InvalidProtocolBufferException ex) {
            throw new MessageDeserializationException(
                    format("Couldn't deserialize data. Data buffer may be corrupted. Details: %s", ex.getMessage()),
                    ex
            );
        } catch (ClassNotFoundException | NoSuchMethodException ex) {
            throw new MessageDeserializationException(
                    format("Invalid configuration of Protobuff object messages. Missing Protobuff classes on the classpath? " +
                            "Not the Protobuff serialization? Details: %s", ex.getMessage()),
                    ex
            );
        } catch (IllegalAccessException |InvocationTargetException ex) {
            throw new MessageDeserializationException(
                    format("Not allowed access to %s method of Protobuff class. Details: %s", PARSE_FROM_METHOD_NAME, ex.getMessage()),
                    ex
            );
        }
    }

    private Message deserializeMessage(byte[] bytes) throws InvalidProtocolBufferException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        var messageEnvelope = MessageEnvelope.parseFrom(bytes);
        var parseMethod =
                Class.forName(messageEnvelope.getPayloadTypeQualifiedName())
                .getMethod(PARSE_FROM_METHOD_NAME, byte[].class);

        return (Message) parseMethod.invoke(null, new Object[]{ messageEnvelope.getPayload().getValue().toByteArray()  });
    }

    @Override
    public Message deserialize(String topic, Headers headers, byte[] data) {
        return tryDeserializeMessage(data);
    }

    @Override
    public void close() {
    }
}
