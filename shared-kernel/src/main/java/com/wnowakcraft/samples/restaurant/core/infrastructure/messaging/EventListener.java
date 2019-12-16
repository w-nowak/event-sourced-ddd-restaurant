package com.wnowakcraft.samples.restaurant.core.infrastructure.messaging;

import com.wnowakcraft.samples.restaurant.core.domain.model.Event;

import java.util.function.Consumer;

public interface EventListener<E extends Event> {
    void onEvent(Consumer<E> eventConsumer);
}
