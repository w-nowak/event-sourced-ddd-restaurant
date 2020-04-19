package com.wnowakcraft.samples.restaurant.order.infrastructure.data.conversion;

import com.google.protobuf.Message;
import com.wnowakcraft.samples.restaurant.core.domain.model.Event;
import com.wnowakcraft.samples.restaurant.order.domain.model.OrderEvent;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;
import static java.lang.String.format;

public class DataConverterComposite implements DataConverter<OrderEvent, Message> {
    private Collection<DataConverter<OrderEvent, Message>> dataConverters;

    @Inject
    public DataConverterComposite(Collection<DataConverter<OrderEvent, Message>> dataConverters) {
        requireNonNull(dataConverters, "dataConverters");

        this.dataConverters = List.copyOf(dataConverters);

    }

    @Override
    public OrderEvent convert(Message message, long offset) {
        return dataConverters.stream()
                .filter(dataConverter -> dataConverter.canConvert(message))
                .findFirst().orElseThrow(unsupportedInputType(message))
                .convert(message, offset);
    }

    @Override
    public Message convert(OrderEvent event) {
        return dataConverters.stream()
                .filter(dataConverter -> dataConverter.canConvert(event))
                .findFirst().orElseThrow(unsupportedInputType(event))
                .convert(event);
    }

    private Supplier<IllegalArgumentException> unsupportedInputType(Object object) {
        return () ->
                new IllegalArgumentException(
                        format("Couldn't find converter for a type: %s", object.getClass().getName())
                );
    }

    @Override
    public boolean canConvert(Message message) {
        return dataConverters.stream().anyMatch(dataConverter -> dataConverter.canConvert(message));
    }

    @Override
    public boolean canConvert(Event<?> event) {
        return dataConverters.stream().anyMatch(dataConverter -> dataConverter.canConvert(event));
    }

    @Produces
    static Collection<DataConverter<OrderEvent, Message>> dataConverters(
            @ConcreteDataConverter Instance<DataConverter<? extends OrderEvent, ? extends Message>> concreteDataConverters
    ) {
        return concreteDataConverters.stream()
                .map(converter -> (DataConverter<OrderEvent, Message>)converter)
                .collect(Collectors.toList());
    }
}
