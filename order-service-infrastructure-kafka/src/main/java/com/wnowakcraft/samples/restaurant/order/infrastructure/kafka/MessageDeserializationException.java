package com.wnowakcraft.samples.restaurant.order.infrastructure.kafka;

import lombok.ToString;

@ToString(callSuper = true)
public class MessageDeserializationException extends RuntimeException {
    public MessageDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
