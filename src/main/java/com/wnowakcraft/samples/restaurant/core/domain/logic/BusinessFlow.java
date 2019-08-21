package com.wnowakcraft.samples.restaurant.core.domain.logic;

import com.wnowakcraft.samples.restaurant.core.domain.model.Command;
import com.wnowakcraft.samples.restaurant.core.domain.model.Event;
import com.wnowakcraft.samples.restaurant.core.domain.model.Message;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;
import static com.wnowakcraft.preconditions.Preconditions.requireStateThat;
import static lombok.AccessLevel.PRIVATE;

@Getter
@RequiredArgsConstructor
public class BusinessFlow<E extends Event, S> {
    private final Class<E> flowTriggerEventClass;
    private final Function<S, ? extends Command> flowTriggerCompensationCommandProvider;
    private final List<BusinessFlowStep> businessFlowSteps;
    private final Supplier<S> flowStateHolderSupplier;

    public static <E extends Event, S> ThenSendWithCompensation<E, S> startWith(Class<E> event, Supplier<S> flowStateHolderSupplier) {
        requireNonNull(event, "event");

        return new BusinessFlowBuilder<>(event, flowStateHolderSupplier);
    }

    public static class ResponseMapping<S> extends HashMap<Class<? extends Message>, BiConsumer<Message, S>>{ }

    @Getter
    @RequiredArgsConstructor(access = PRIVATE)
    public static class BusinessFlowStep<S>  {
        private final Function<S, ? extends Command> stepCommandProvider;
        private final ResponseMapping<S> responseMapping;
        private final Function<S, ? extends Command> compensatingCommandFnProvider;
    }

    @RequiredArgsConstructor(access = PRIVATE)
    private static class BusinessFlowBuilder<E extends Event, S> implements ThenSend<E, S>, ThenSendWithCompensation<E, S>, On<E, S> {
        private final Class<E> flowTriggerEventClass;
        private final List<BusinessFlowStep> businessFlowSteps = new LinkedList<>();
        private final Supplier<S> flowStateHolderSupplier;
        private Function<S, ? extends Command> flowTriggerCompensationCommandProvider;
        private Function<S, ? extends Command> currentCommandProvider;

        @Override
        public <C extends Command> On<E, S> thenSend(Function<S, C> nextCommandProvider) {
            this.currentCommandProvider = nextCommandProvider;
            return this;
        }

        @Override
        public <C extends Command> ThenSend<E, S> compensateBy(Function<S, ? extends C> flowTriggerCompensationCommandProvider) {
            this.flowTriggerCompensationCommandProvider = flowTriggerCompensationCommandProvider;
            return this;
        }

        @Override
        public <M extends Message> OnResponse<E, S> on(Class<M> message, BiConsumer<? super M, S> responseMessageConsumer) {
            return new BusinessFlowStepBuilder();
        }

        private BusinessFlow<E, S> build() {
            return new BusinessFlow<>(flowTriggerEventClass, flowTriggerCompensationCommandProvider, businessFlowSteps, flowStateHolderSupplier);
        }

        @RequiredArgsConstructor
        private class BusinessFlowStepBuilder implements OnResponse<E, S> {
            private ResponseMapping<S> responseMapping = new ResponseMapping<>();
            private Function<S, ? extends Command> compensatingCommandProvider;

            @Override
            public <M extends Message> OnResponse<E, S> on(Class<M> message, BiConsumer<? super M, S> responseMessageConsumer) {
                this.responseMapping.put(message, (BiConsumer<Message, S>)responseMessageConsumer);
                return this;
            }

            @Override
            public <C extends Command> OnResponse<E, S> compensateBy(Function<S, ? extends C> compensatingCommandProvider) {
                this.compensatingCommandProvider = compensatingCommandProvider;
                return this;
            }

            @Override
            public BusinessFlow<E, S> done() {
                return build();
            }

            @Override
            public <C extends Command> On<E, S> thenSend(Function<S, C> nextCommandProvider) {
                businessFlowSteps.add(
                        new BusinessFlowStep<>(currentCommandProvider, this.responseMapping, this.compensatingCommandProvider)
                );
                return BusinessFlowBuilder.this.thenSend(nextCommandProvider);
            }
        }


    }

    public interface ThenSend<E extends Event, S> {
        <C extends Command> On<E, S> thenSend(Function<S, C> nextCommandProvider);
    }

    public interface ThenSendWithCompensation<E extends Event, S> extends ThenSend<E, S> {
        <C extends Command> ThenSend<E, S> compensateBy(Function<S, ? extends C> compensatingCommandProvider);
    }

    private static class MarkerConsumer<S> implements BiConsumer<Message, S> {
        @Override
        public void accept(Message message, S stateHolder) {
            requireStateThat(false, "This is just a marker consumer so must not be invoked");
        }
    }
    private static class MarkerSuccessConsumer<S> extends MarkerConsumer<S> { }
    private static class MarkerFailureWithCompensateConsumer<S> extends MarkerConsumer<S> { }
    private static class MarkerFailureWithRetryConsumer<S> extends MarkerConsumer<S> { }
    public interface OnResponse<E extends  Event, S> extends ThenSend<E, S> {
        static <S> MarkerConsumer<S> success() { return new MarkerSuccessConsumer<>(); };
        static <S> MarkerConsumer<S> failureWithCompensation() { return  new MarkerFailureWithCompensateConsumer<>(); };
        static <S> MarkerConsumer<S> failureWithRetry() { return  new MarkerFailureWithRetryConsumer<>(); };
        <M extends Message> OnResponse<E, S> on(Class<M> message, BiConsumer<? super M, S> responseMessageConsumer);
        <C extends Command> OnResponse<E, S> compensateBy(Function<S, ? extends C> compensatingCommandProvider);
        BusinessFlow<E, S> done();
    }

    public interface On<E extends Event, S> {
        <M extends Message> OnResponse<E, S> on(Class<M> message, BiConsumer<? super M, S> responseMessageConsumer);
    }
}