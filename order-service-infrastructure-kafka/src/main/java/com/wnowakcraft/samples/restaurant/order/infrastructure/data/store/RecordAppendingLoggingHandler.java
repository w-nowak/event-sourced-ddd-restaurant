package com.wnowakcraft.samples.restaurant.order.infrastructure.data.store;

import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;

@Slf4j
public class RecordAppendingLoggingHandler {
    public static Callback getHandlerFor(ProducerRecord<String, Message> record) {
        return (recordMetadata, exception) -> {
            if(exception == null) {
                log.info("A new record with a key of {} appended successfully to a topic {} and partition {}",
                        record.key(), record.topic(), record.partition());
            } else {
                log.error("Appending a new record with a key of {} to a topic {} and partition {} has failed due to: {}",
                        record.key(), record.topic(), record.partition(), exception.getMessage(), exception);
            }

            log.atDebug().addArgument(record::toString).log("Processed record: {}");
        };
    }
}
