package com.wnowakcraft.samples.restaurant.core.infrastructure.saga;

import com.wnowakcraft.samples.restaurant.core.domain.logic.BusinessFlowDefinition;
import com.wnowakcraft.samples.restaurant.core.domain.logic.BusinessFlowDefinition.BusinessFlowStep;
import com.wnowakcraft.samples.restaurant.core.domain.model.Command;
import com.wnowakcraft.samples.restaurant.core.domain.model.Event;
import com.wnowakcraft.samples.restaurant.core.domain.model.Message;
import com.wnowakcraft.samples.restaurant.core.domain.model.Response;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.wnowakcraft.preconditions.Preconditions.requireStateThat;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor
public class BusinessFlowRunner <E extends Event, S> {
    @NonNull private final BusinessFlowHandler<E, S> businessFlowHandler;
    @NonNull private final Function<UUID, StateEnvelope<S>> flowStateProvider;
    @NonNull private final BiConsumer<StateEnvelope<S>, Command> businessFlowInitHandler;
    @NonNull private final BiConsumer<StateEnvelope<S>, Command> flowStateChangeHandler;
    @NonNull private final Consumer<StateEnvelope<S>> flowFinishedHandler;

    public static <E extends Event, S> BusinessFlowRunnerBuilder<E, S> from(BusinessFlowDefinition<E, S> businessFlowDefinition) {
        var businessFlowHandler = BusinessFlowHandler.createFor(businessFlowDefinition);
        return new BusinessFlowRunnerBuilder<>(businessFlowHandler);
    }

    public void onCommandResponse(Response commandResponse) {
        if(!businessFlowHandler.isInitialized()){
            businessFlowHandler.initWith(flowStateProvider.apply(commandResponse.getCorrelationId()));
        }

        if(!businessFlowHandler.accepts(commandResponse)) {
           return;
        }

        businessFlowHandler.consume(commandResponse);
        var flowCurrentState = businessFlowHandler.getFlowCurrentState();
        var nextCommand = businessFlowHandler.getNextCommand();

        if(businessFlowHandler.isFlowComplete()) {
            flowFinishedHandler.accept(flowCurrentState);
        } else {
            flowStateChangeHandler.accept(
                    flowCurrentState,
                    nextCommand.orElseThrow(() -> new IllegalStateException("Flow is not finished but no next command defined"))
            );
        }
    }

    public void onInitHandler(E initEvent) {
        var initFlowState = businessFlowHandler.initWith(initEvent);
        var firstCommand = businessFlowHandler.getNextCommand().get();

        businessFlowInitHandler.accept(initFlowState, firstCommand);
    }


    @RequiredArgsConstructor
    public static class BusinessFlowRunnerBuilder<E extends Event, S>  {
        private final BusinessFlowHandler<E, S> businessFlowHandler;
        private Function<UUID, StateEnvelope<S>> flowStateProvider;
        private BiConsumer<StateEnvelope<S>, Command> businessFlowInitHandler;
        private BiConsumer<StateEnvelope<S>, Command> flowStateChangeHandler;
        private Consumer<StateEnvelope<S>> flowFinishedHandler;

        public BusinessFlowRunnerBuilder<E, S> withFlowStateProvider(Function<UUID, StateEnvelope<S>> flowStateProvider) {
            this.flowStateProvider = flowStateProvider;
            return this;
        }

        public BusinessFlowRunnerBuilder<E, S> onInit(BiConsumer<StateEnvelope<S>, Command> businessFlowInitHandler) {
            this.businessFlowInitHandler = businessFlowInitHandler;
            return this;
        }

        public BusinessFlowRunnerBuilder<E, S> onFlowStateChange(BiConsumer<StateEnvelope<S>, Command> flowStateChangeHandler) {
            this.flowStateChangeHandler = flowStateChangeHandler;
            return this;
        }

        public BusinessFlowRunnerBuilder<E, S> onFlowFinished(Consumer<StateEnvelope<S>> flowFinishedHandler) {
            this.flowFinishedHandler = flowFinishedHandler;
            return this;
        }

        public BusinessFlowRunner<E, S> run() {
            return new BusinessFlowRunner<>(
                    businessFlowHandler, flowStateProvider, businessFlowInitHandler,
                    flowStateChangeHandler, flowFinishedHandler
            );
        }
    }

    @RequiredArgsConstructor(access = PRIVATE)
    private static class BusinessFlowHandler<E extends Event, S> {
        private final BusinessFlowDefinition<E, S> businessFlowDefinition;
        private StateEnvelope<S> flowCurrentState;
        private boolean flowInterruptedByCompensation;

        static <S, E extends Event> BusinessFlowHandler<E, S> createFor(BusinessFlowDefinition<E, S> businessFlowDefinition) {
            return new BusinessFlowHandler<>(businessFlowDefinition);
        }

        private boolean accepts(Response commandResponse) {
            requireFlowNotYetComplete();

            return currentStepDefinition().getResponseMapping().keySet().stream()
                    .anyMatch(mappedClass -> mappedClass.isAssignableFrom(commandResponse.getClass()));
        }

        private BusinessFlowStep<S> currentStepDefinition() {
            requireFlowNotYetComplete();

            return businessFlowDefinition.getBusinessFlowSteps().get(flowCurrentState.getStateIndex());
        }

        void consume(Response commandResponse) {
            requireFlowNotYetComplete();

            currentStepDefinition().getResponseMapping().entrySet().stream()
                    .filter(mapping -> mapping.getKey().isAssignableFrom(commandResponse.getClass()))
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .ifPresent(handleResponse(commandResponse));
        }

        private Consumer<BiConsumer<Message, S>> handleResponse(Response commandResponse) {
            return foundResponseMapping -> {
                if(BusinessFlowDefinition.isCompensateMarkerConsumer(foundResponseMapping))
                {
                    flowInterruptedByCompensation = true;
                    return;
                }

                if (BusinessFlowDefinition.isNotMarkerConsumer(foundResponseMapping)) {
                    foundResponseMapping.accept(commandResponse, flowCurrentState.getState());
                }
                flowCurrentState = flowCurrentState.advanceState();
            };
        }

        StateEnvelope<S> getFlowCurrentState() {
            return flowCurrentState;
        }

        Optional<Command> getNextCommand() {
            return isFlowComplete() ?
                    getCompensationCommandWhenCompletedByCompensationOrEmpty(this::currentStepDefinition, flowCurrentState) :
                    Optional.of(currentStepDefinition().getStepCommandProvider().apply(flowCurrentState.getState()));
        }

        private Optional<Command> getCompensationCommandWhenCompletedByCompensationOrEmpty(Supplier<BusinessFlowStep<S>> businessFlowStep, StateEnvelope<S> flowCurrentState) {
            return flowInterruptedByCompensation ?
                    Optional.ofNullable(businessFlowStep.get().getCompensatingCommandFnProvider().apply(flowCurrentState.getState())) :
                    Optional.empty();
        }


        boolean isInitialized() {
            return flowCurrentState != null;
        }

        boolean isFlowComplete() {
            return businessFlowDefinition.getBusinessFlowSteps().size() == flowCurrentState.getStateIndex();
        }

        void initWith(StateEnvelope<S> flowState) {
            requireFlowNotYetInitialized();

            flowCurrentState = flowState;
        }

        StateEnvelope<S> initWith(E event) {
            requireFlowNotYetInitialized();

            flowCurrentState = new StateEnvelope<>(
                    businessFlowDefinition.getFlowInitStateProvider().apply(event)
            );

            return flowCurrentState;
        }

        private void requireFlowNotYetInitialized() {
            requireStateThat(!isInitialized(), "Flow runner got already initialized with current state. The current state cannot be overriden.");
        }

        private void requireFlowNotYetComplete() {
            requireStateThat(
                    !(flowInterruptedByCompensation || isFlowComplete()),
                    "Flow is already complete and can do nothing more");
        }
    }


    @Getter
    public static class StateEnvelope<S> {
        private final int stateIndex;
        private final S state;


        public StateEnvelope(S state) {
            this(0, state);
        }

        StateEnvelope(int stateIndex, S state) {
            this.stateIndex = stateIndex;
            this.state = state;
        }

        public StateEnvelope<S> advanceState() {
            return new StateEnvelope<>(stateIndex + 1, state);
        }
    }
}
