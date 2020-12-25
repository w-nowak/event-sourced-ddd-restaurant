package com.wnowakcraft.samples.restaurant.order.infrastructure.data.conversion;

import com.google.protobuf.Message;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;
import static java.lang.String.format;

public class MessageConverterComposite<S> implements MessageConverter<S, Message> {
    private Collection<MessageConverter<S, Message>> messageConverters;

    @Inject
    public MessageConverterComposite(Collection<MessageConverter<S, Message>> messageConverters) {
        requireNonNull(messageConverters, "messageConverters");

        this.messageConverters = List.copyOf(messageConverters);
    }

    @Override
    public S convert(Message message, long offset) {
        return messageConverters.stream()
                .filter(dataConverter -> dataConverter.canConvert(message))
                .findFirst().orElseThrow(unsupportedInputType(message))
                .convert(message, offset);
    }

    @Override
    public Message convert(S source) {
        return messageConverters.stream()
                .filter(dataConverter -> dataConverter.canConvert(source))
                .findFirst().orElseThrow(unsupportedInputType(source))
                .convert(source);
    }

    private Supplier<IllegalArgumentException> unsupportedInputType(Object object) {
        return () ->
                new IllegalArgumentException(
                        format("Couldn't find converter for a type: %s", object.getClass().getName())
                );
    }

    @Override
    public boolean canConvert(Message messageCandidate) {
        return messageConverters.stream().anyMatch(messageConverter -> messageConverter.canConvert(messageCandidate));
    }

    @Override
    public boolean canConvert(Object sourceCandidate) {
        return messageConverters.stream().anyMatch(dataConverter -> dataConverter.canConvert(sourceCandidate));
    }
}
