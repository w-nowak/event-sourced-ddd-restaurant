package com.wnowakcraft.samples.restaurant.core.infrastructure.messaging;

import com.wnowakcraft.samples.restaurant.core.domain.model.Response;

import java.util.function.Consumer;

public interface CommandResponseChannel {
    void invokeOnCommand(Consumer<Response> responseConsumer);
}
