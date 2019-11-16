package com.wnowakcraft.samples.restaurant.core.infrastructure.messaging;

import com.wnowakcraft.samples.restaurant.core.domain.model.Event;

import java.util.concurrent.CompletableFuture;

public interface EventListenerFactory {
    <E extends Event> CompletableFuture<EventListener<E>> listenToEventsOfKind(Class<E> eventFamily);
}
