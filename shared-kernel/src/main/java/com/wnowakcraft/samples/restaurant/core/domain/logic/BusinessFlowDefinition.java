package com.wnowakcraft.samples.restaurant.core.domain.logic;

import com.wnowakcraft.samples.restaurant.core.domain.model.Command;
import com.wnowakcraft.samples.restaurant.core.domain.model.Event;
import com.wnowakcraft.samples.restaurant.core.domain.model.Message;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;
import static com.wnowakcraft.preconditions.Preconditions.requireStateThat;
import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

@Getter
@RequiredArgsConstructor
public class BusinessFlowDefinition<E extends Event<?>, S> {
    private final Class<E> flowTriggerEventClass;
    private final Function<E, S> flowInitStateProvider;
    private final List<BusinessFlowStep<S>> businessFlowSteps;

    public static <E extends Event<?>, S> OnResponse<E, S> startWith(Class<E> event, Function<E, S> flowInitStateProvider) {
        requireNonNull(event, "event");

        return new BusinessFlowBuilder<>(event, flowInitStateProvider);
    }

    @NoArgsConstructor(access = PRIVATE)
    public static class ResponseMapping<S> extends HashMap<Class<? extends Message>, BiConsumer<Message, S>>{
        public static <S> ResponseMapping<S> empty() {
            return new ResponseMapping<>();
        }
    }

    public static boolean isCompensateMarkerConsumer(BiConsumer<?, ?> consumer) {
        return consumer.getClass() == MarkerFailureWithCompensateConsumer.class;
    }

    public static boolean isRetryMarkerConsumer(BiConsumer<?, ?> consumer) {
        return consumer.getClass() == MarkerFailureWithRetryConsumer.class;
    }

    public static boolean isNotMarkerConsumer(BiConsumer<?, ?> consumer) {
        return !MarkerConsumer.class.isAssignableFrom(consumer.getClass());
    }

    @Getter
    @RequiredArgsConstructor(access = PRIVATE)
    public static class BusinessFlowStep<S>  {
        private final Function<S, ? extends Command> stepCommandProvider;
        private final ResponseMapping<S> responseMapping;
        private final Function<S, ? extends Command> compensatingCommandFnProvider;

        public static <S> BusinessFlowStep<S> emptyBusinessFlowStep() {
            return new BusinessFlowStep<>(null, ResponseMapping.empty(), null );
        }

        public Optional<Function<S, ? extends Command>> getCompensatingCommandFnProvider() {
            return ofNullable(compensatingCommandFnProvider);
        }
    }

    @RequiredArgsConstructor(access = PRIVATE)
    private static class BusinessFlowBuilder<E extends Event<?>, S> implements ThenSend<E, S>, OnResponse<E, S>, On<E, S> {
        private final Class<E> flowTriggerEventClass;
        private final List<BusinessFlowStep<S>> businessFlowSteps = new LinkedList<>();
        private final Function<E, S> flowInitStateProvider;
        private Function<S, ? extends Command> flowTriggerCompensationCommandProvider;
        private Function<S, ? extends Command> currentCommandProvider;

        @Override
        public <C extends Command> On<E, S> thenSend(Function<S, C> nextCommandProvider) {
            addNextBusinessFlowStepWith(ResponseMapping.empty(), this.flowTriggerCompensationCommandProvider);
            return andThenSend(nextCommandProvider);
        }

        private <C extends Command>  BusinessFlowBuilder<E, S> andThenSend(Function<S, C> nextCommandProvider) {
            this.currentCommandProvider = nextCommandProvider;
            return this;
        }

        @Override
        public <C extends Command> ThenSend<E, S> compensateBy(Function<S, ? extends C> flowTriggerCompensationCommandProvider) {
            this.flowTriggerCompensationCommandProvider = flowTriggerCompensationCommandProvider;
            return this;
        }

        private void addNextBusinessFlowStepWith(ResponseMapping<S> responseMapping, Function<S, ? extends Command> compensatingCommandProvider) {
            businessFlowSteps.add(
                    new BusinessFlowStep<>(currentCommandProvider, responseMapping, compensatingCommandProvider)
            );
        }

        @Override
        public <M extends Message> OnResponseFinalizable<E, S> on(Class<M> message, BiConsumer<? super M, S> responseMessageConsumer) {
            return new BusinessFlowStepBuilder()
                    .on(message, responseMessageConsumer);
        }

        private BusinessFlowDefinition<E, S> build() {
            return new BusinessFlowDefinition<>(flowTriggerEventClass, flowInitStateProvider, businessFlowSteps);
        }

        @RequiredArgsConstructor
        private class BusinessFlowStepBuilder implements OnResponseFinalizable<E, S>, ThenSendFinalizable<E, S> {
            private ResponseMapping<S> responseMapping = ResponseMapping.empty();
            private Function<S, ? extends Command> compensatingCommandProvider;

            @Override
            @SuppressWarnings("unchecked")
            public <M extends Message> OnResponseFinalizable<E, S> on(Class<M> message, BiConsumer<? super M, S> responseMessageConsumer) {
                this.responseMapping.put(message, (BiConsumer<Message, S>)responseMessageConsumer);
                return this;
            }

            @Override
            public <C extends Command> ThenSendFinalizable<E, S> compensateBy(Function<S, ? extends C> compensatingCommandProvider) {
                this.compensatingCommandProvider = compensatingCommandProvider;
                return this;
            }

            @Override
            public BusinessFlowDefinition<E, S> done() {
                addNextBusinessFlowStepWith(responseMapping, compensatingCommandProvider);
                return build();
            }

            @Override
            public <C extends Command> On<E, S> thenSend(Function<S, C> nextCommandProvider) {
                addNextBusinessFlowStepWith(responseMapping, compensatingCommandProvider);
                return andThenSend(nextCommandProvider);
            }
        }


    }

    private static class MarkerConsumer<S> implements BiConsumer<Message, S> {
        @Override
        public void accept(Message message, S stateHolder) {
            requireStateThat(false, "This is just a marker consumer so must not be invoked");
        }
    }
    private static class MarkerSuccessConsumer<S> extends MarkerConsumer<S> { }
    private static class MarkerFailureWithRetryConsumer<S> extends MarkerConsumer<S> { }

    @RequiredArgsConstructor
    private static class MarkerFailureWithCompensateConsumer<S> extends MarkerConsumer<S> {
        private final BiConsumer<Message, S> targetConsumer;

        MarkerFailureWithCompensateConsumer() {
            this.targetConsumer = (message, s) -> {};
        }

        @Override
        public void accept(Message message, S stateHolder) {
            targetConsumer.accept(message, stateHolder);
        }
    }

    public interface ThenSend<E extends Event<?>, S> {
        <C extends Command> On<E, S> thenSend(Function<S, C> nextCommandProvider);
    }

    public interface Finalizable<E extends Event<?>, S> {
        BusinessFlowDefinition<E, S> done();
    }

    public interface ThenSendFinalizable<E extends Event<?>, S>  extends ThenSend<E, S>, Finalizable<E, S> {
    }

    public interface OnResponse <E extends  Event<?>, S> extends ThenSend<E, S> {
        static <S> MarkerConsumer<S> success() { return new MarkerSuccessConsumer<>(); }
        static <S> MarkerConsumer<S> failureWithRetry() { return  new MarkerFailureWithRetryConsumer<>(); }
        static <S> MarkerConsumer<S> failureWithCompensation() { return  new MarkerFailureWithCompensateConsumer<>(); }
        @SuppressWarnings("unchecked")
        static <S, M extends Message> MarkerConsumer<S> failureWithCompensation(BiConsumer<? super M, S> targetConsumer) { return  new MarkerFailureWithCompensateConsumer<>((BiConsumer<Message, S>)targetConsumer); }
        <M extends Message> OnResponse<E, S> on(Class<M> message, BiConsumer<? super M, S> responseMessageConsumer);
        <C extends Command> ThenSend<E, S> compensateBy(Function<S, ? extends C> compensatingCommandProvider);
    }

    public interface OnResponseFinalizable<E extends  Event<?>, S> extends OnResponse<E, S>, Finalizable<E, S> {
        <M extends Message> OnResponseFinalizable<E, S> on(Class<M> message, BiConsumer<? super M, S> responseMessageConsumer);
        <C extends Command> ThenSendFinalizable<E, S> compensateBy(Function<S, ? extends C> compensatingCommandProvider);
    }

    public interface On<E extends Event<?>, S> {
        <M extends Message> OnResponseFinalizable<E, S> on(Class<M> message, BiConsumer<? super M, S> responseMessageConsumer);
    }
}