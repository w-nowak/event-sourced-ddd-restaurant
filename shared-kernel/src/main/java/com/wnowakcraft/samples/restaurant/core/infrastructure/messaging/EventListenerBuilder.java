package com.wnowakcraft.samples.restaurant.core.infrastructure.messaging;

import com.wnowakcraft.samples.restaurant.core.domain.model.Event;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class EventListenerBuilder {
    private final EventListenerFactory eventListenerFactory;

    public <E extends Event> EventListenerBuilderBound<E> listenToEventsOfKind(Class<E> eventKind) {
        return new EventListenerBuilderBound<>(eventKind);
    }

    @RequiredArgsConstructor
    public class EventListenerBuilderBound<E extends Event> {
        private final Class<E> boundOnEventKind;
        private Consumer<E> eventConsumer;
        private Collection<Class<? extends E>> acceptedEventTypes;

        @SafeVarargs
        public final EventListenerBuilderBound<E> acceptOnly(Class<? extends E>... acceptedEventTypes) {
            this.acceptedEventTypes = List.of(acceptedEventTypes);
            return this;
        }

        public EventListenerBuilderBound<E> onEvent(Consumer<E> eventConsumer) {
            this.eventConsumer = eventConsumer;
            return this;
        }

        public void listenToEvents() {
            eventListenerFactory
                    .listenToEventsOfKind(boundOnEventKind)
                    .thenAccept(eventListener -> eventListener.onEvent(this::handOverAcceptedEvent));

        }

        private void handOverAcceptedEvent(E event) {
            if(acceptedEventTypes.contains(event.getClass())) {
                eventConsumer.accept(event);
            }
        }

    }
}
