package com.wnowakcraft.samples.restaurant.order.infrastructure.data.store;

import com.google.protobuf.Message;
import com.wnowakcraft.samples.restaurant.core.domain.model.DomainBoundBusinessId;
import com.wnowakcraft.samples.restaurant.order.infrastructure.data.conversion.MessageConverter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.StreamSupport;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;

@RequiredArgsConstructor(onConstructor_ = { @Inject})
public class KafkaRecordReader<R> {
    private static final Duration FOR_ONE_SECOND = Duration.ofSeconds(1);
    @NonNull private final MessageConverter<R, Message> snapshotMessageConverter;

    public Collection<R> readRecordsFrom(Consumer<String, Message> recordConsumer, DomainBoundBusinessId byBusinessId) {
        requireNonNull(recordConsumer, "recordConsumer");
        requireNonNull(byBusinessId, "byBusinessId");

        return readRecordsFrom(recordConsumer, byBusinessId, Integer.MAX_VALUE);
    }

    public Collection<R> readLimitedNumberOfRecordsFrom(Consumer<String, Message> recordConsumer,
                                                        DomainBoundBusinessId byBusinessId,
                                                        int recordsLimit) {
        requireNonNull(recordConsumer, "recordConsumer");
        requireNonNull(byBusinessId, "byBusinessId");

        return readRecordsFrom(recordConsumer, byBusinessId, recordsLimit);
    }

    private Collection<R> readRecordsFrom(Consumer<String, Message> recordConsumer,
                                          DomainBoundBusinessId businessId,
                                          int recordsLimit) {

        Collection<R> records = new LinkedHashSet<>();
        ConsumerRecords<String, Message> readRecords;
        var remainingRecordsLimit = recordsLimit;

        while(!(readRecords = recordConsumer.poll(FOR_ONE_SECOND)).isEmpty() && remainingRecordsLimit > 0) {
            StreamSupport.stream(readRecords.spliterator(), false)
                    //TODO Consider passing buisness object id explicitly
                    //.filter(record -> businessId.getValue().equals(record.key()))
                    .map(record -> snapshotMessageConverter.convert(record.value(), record.offset()))
                    .limit(remainingRecordsLimit)
                    .forEachOrdered(records::add);

            remainingRecordsLimit = recordsLimit - records.size();
        }
        return List.copyOf(records);
    }
}
