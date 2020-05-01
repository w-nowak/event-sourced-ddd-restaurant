package com.wnowakcraft.samples.restaurant.order.infrastructure.data.conversion;

import com.google.protobuf.Message;

public interface SnapshotConverter<S, M extends Message> {
    S convert(M message, long offset);
    M convert(S event) ;
    boolean canConvert(Message message);
    boolean canConvert(Object record);
}
