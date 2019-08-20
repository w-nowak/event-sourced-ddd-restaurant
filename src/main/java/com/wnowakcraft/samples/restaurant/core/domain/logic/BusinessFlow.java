package com.wnowakcraft.samples.restaurant.core.domain.logic;

import com.wnowakcraft.samples.restaurant.core.domain.model.Command;
import com.wnowakcraft.samples.restaurant.core.domain.model.Event;
import com.wnowakcraft.samples.restaurant.core.domain.model.Message;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import static com.wnowakcraft.preconditions.Preconditions.requireNonNull;
import static com.wnowakcraft.preconditions.Preconditions.requireStateThat;
import static lombok.AccessLevel.PRIVATE;

@Getter
@RequiredArgsConstructor
public class BusinessFlow<E extends Event> {
    private final Class<E> flowTriggerEventClass;
    private final Function<? super E, ? extends Command> flowTriggerCompensation;
    private final List<BusinessFlowStep> businessFlowSteps;

    public static <E extends Event> ThenSendWithCompensation<E> startWith(Class<E> event) {
        requireNonNull(event, "event");

        return new BusinessFlowBuilder<>(event);
    }

    public static class ResponseMapping extends HashMap<Class<? extends Message>, Function<Message, ? extends Command>>{ }

    @Getter
    @RequiredArgsConstructor(access = PRIVATE)
    public static class BusinessFlowStep<E extends Event, C extends Command>  {
        private final Function<? super E, ? extends C> stepCommandFnProvider;
        private final ResponseMapping responseMapping;
        private final Function<? super E, ? extends C> compensatingCommandFnProvider;
    }

    @RequiredArgsConstructor(access = PRIVATE)
    private static class BusinessFlowBuilder<E extends Event> implements ThenSend<E>, ThenSendWithCompensation<E>, ThenSendResultCommand<E>, On<E> {
        private final Class<E> flowTriggerEventClass;
        private final List<BusinessFlowStep> businessFlowSteps = new LinkedList<>();
        private Function<? super E, ? extends Command> flowTriggerCompensation;
        private Function<? super E, ? extends Command> currentCommandFnProvider;

        @Override
        public <C extends Command> On<E> thenSend(Function<? super E, C> nextCommandFnProvider) {
            this.currentCommandFnProvider = nextCommandFnProvider;
            return this;
        }

        @Override
        public <C extends Command> ThenSend<E> compensateBy(Function<? super E, C> flowTriggerCompensation) {
            this.flowTriggerCompensation = flowTriggerCompensation;
            return this;
        }

        @Override
        public On<E> thenSendResultCommand() {
            this.currentCommandFnProvider = null;
            return this;
        }

        @Override
        public <M extends Message, C extends Command> OnResponse<E, C> on(Class<M> message, Function<? super M, C> generateCommand) {
            return new BusinessFlowStepBuilder<>();
        }

        private BusinessFlow<E> build() {
            return new BusinessFlow<>(flowTriggerEventClass, flowTriggerCompensation, businessFlowSteps);
        }

        @RequiredArgsConstructor
        private class BusinessFlowStepBuilder<C extends Command> implements OnResponse<E, C> {
            private ResponseMapping responseMapping = new ResponseMapping();
            private Function<? super E, ? extends C> compensatingCommandFnProvider;

            @Override
            public <M extends Message> OnResponse<E, C> on(Class<M> message, Function<? super M, C> generateCommand) {
                this.responseMapping.put(message, (Function<Message, C>)generateCommand);
                return this;
            }

            @Override
            public OnResponse<E, C> compensateBy(Function<? super E, ? extends C> compensationFnProvider) {
                this.compensatingCommandFnProvider = compensationFnProvider;
                return this;
            }

            @Override
            public BusinessFlow<E> done() {
                return build();
            }

            @Override
            public <C extends Command> On<E> thenSend(Function<? super E, C> nextCommandFnProvider) {
                businessFlowSteps.add(
                        new BusinessFlowStep<>(currentCommandFnProvider, this.responseMapping, compensatingCommandFnProvider)
                );
                return BusinessFlowBuilder.this.thenSend(nextCommandFnProvider);
            }

            @Override
            public On<E> thenSendResultCommand() {
                businessFlowSteps.add(
                        new BusinessFlowStep<>(currentCommandFnProvider, this.responseMapping, compensatingCommandFnProvider)
                );
                return BusinessFlowBuilder.this.thenSendResultCommand();
            }
        }


    }

    public interface ThenSend<E extends Event> {
        <C extends Command> On<E> thenSend(Function<? super E, C> nextCommand);
    }

    public interface ThenSendWithCompensation<E extends Event> extends ThenSend<E> {
        <C extends Command> ThenSend<E> compensateBy(Function<? super E, C> nextCommandSupplier);
    }

    public interface ThenSendResultCommand<E extends Event> extends ThenSend<E> {
        On<E> thenSendResultCommand();
    }

    private static class MarkerFunction<C extends Command> implements Function<Message, C> {
        @Override
        public C apply(Message message) {
            requireStateThat(false, "This function is a marker function so must not be invoked");
            return null;
        }
    }
    private static class MarkerSuccessFunction<C extends Command> extends MarkerFunction<C> { }
    private static class MarkerFailureWithCompensateFunction<C extends Command> extends MarkerFunction<C> { }
    private static class MarkerFailureWithRetryFunction<C extends Command> extends MarkerFunction<C> { }
    public interface OnResponse<E extends  Event, C extends Command> extends ThenSendResultCommand<E> {
        static <C extends Command> Function<Message, C> success() { return new MarkerSuccessFunction<C>(); };
        static <C extends Command> Function<Message, C> failureWithCompensation() { return  new MarkerFailureWithCompensateFunction<>(); };
        static <C extends Command> Function<Message, C> failureWithRetry() { return  new MarkerFailureWithRetryFunction<>(); };
        <M extends Message> OnResponse<E, C> on(Class<M> message, Function<? super M, C> generateCommand);
        OnResponse<E, C> compensateBy(Function<? super E, ? extends C> compensationFnProvider);
        BusinessFlow<E> done();
    }

    public interface On<E extends Event> {
        <M extends Message, C extends Command> OnResponse<E, C> on(Class<M> message, Function<? super M, C> generateCommand);
    }
}