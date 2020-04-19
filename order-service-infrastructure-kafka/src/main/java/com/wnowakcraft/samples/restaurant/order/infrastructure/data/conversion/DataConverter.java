package com.wnowakcraft.samples.restaurant.order.infrastructure.data.conversion;

import com.google.protobuf.Message;
import com.wnowakcraft.samples.restaurant.core.domain.model.Event;

public interface DataConverter<E extends Event<?>, M extends Message> {
    E convert(M message, long offset);
    M convert(E event) ;
    boolean canConvert(Message message);
    boolean canConvert(Event<?> event);
}
