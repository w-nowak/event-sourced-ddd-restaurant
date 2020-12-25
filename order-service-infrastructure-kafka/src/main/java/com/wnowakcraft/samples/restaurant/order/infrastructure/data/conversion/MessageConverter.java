package com.wnowakcraft.samples.restaurant.order.infrastructure.data.conversion;

import com.google.protobuf.Message;

public interface MessageConverter<S, M extends Message> extends Converter<S, M> {
    S convert(M message, long offset);
    boolean canConvert(Message message);
}
