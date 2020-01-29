package com.wnowakcraft.samples.restaurant.core.infrastructure.saga;

import com.google.common.util.concurrent.Runnables;
import com.wnowakcraft.samples.restaurant.core.domain.logic.BusinessFlowDefinition;
import com.wnowakcraft.samples.restaurant.core.domain.logic.BusinessFlowDefinition.BusinessFlowStep;
import com.wnowakcraft.samples.restaurant.core.domain.model.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.wnowakcraft.preconditions.Preconditions.requireStateThat;
import static java.util.Optional.empty;
import static java.util.Optional.of;
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
        if(businessFlowHandler.isNotYetInitialized()){
            businessFlowHandler.initWith(flowStateProvider.apply(commandResponse.getCorrelationId()));
        }

        if(!businessFlowHandler.accepts(commandResponse)) {
           return;
        }

        businessFlowHandler.consume(commandResponse);
        var nextCommand = businessFlowHandler.moveOnToNextCommand();
        var flowCurrentState = businessFlowHandler.getFlowCurrentState();

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
        var firstCommand = businessFlowHandler.moveOnToNextCommand().get();

        businessFlowInitHandler.accept(initFlowState, firstCommand);
    }


    @RequiredArgsConstructor
    public static class BusinessFlowRunnerBuilder<E extends Event, S> {
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
        private static final short INDEX_OF_FULLY_COMPENSATED_STATE = -2;
        private static final short INIT_EVENT_COMPENSATION_STEP_INDEX = -1;
        private final BusinessFlowDefinition<E, S> businessFlowDefinition;
        private StateEnvelope<S> flowCurrentState;

        static <S, E extends Event> BusinessFlowHandler<E, S> createFor(BusinessFlowDefinition<E, S> businessFlowDefinition) {
            return new BusinessFlowHandler<>(businessFlowDefinition);
        }

        private boolean accepts(Response commandResponse) {
            requireFlowNotYetComplete();

            return isCompensationSucceededResponseWhenCompensating(commandResponse) ||
                    isAnyResponseMappingFor(commandResponse.getClass());
        }

        private boolean isCompensationSucceededResponseWhenCompensating(Response commandResponse) {
            return commandResponse instanceof CompensationSucceededResponse && flowCurrentState.isCompensation();
        }

        private boolean isAnyResponseMappingFor(Class<? extends Response> commandResponseClass) {
            return currentStepDefinition().getResponseMapping().keySet().stream()
                    .anyMatch(mappedClass -> mappedClass.isAssignableFrom(commandResponseClass));
        }

        private BusinessFlowStep<S> currentStepDefinition() {
            requireFlowNotYetComplete();

            var flowCurrentStateIndex = flowCurrentState.getStateIndex();

            return flowCurrentStateIndex == INIT_EVENT_COMPENSATION_STEP_INDEX ?
                    BusinessFlowStep.emptyBusinessFlowStep() :
                    businessFlowDefinition.getBusinessFlowSteps().get(flowCurrentStateIndex);
        }

        void consume(Response commandResponse) {
            requireFlowNotYetComplete();

            currentStepDefinition().getResponseMapping().entrySet().stream()
                    .filter(mapping -> mapping.getKey().isAssignableFrom(commandResponse.getClass()))
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .ifPresentOrElse(handleResponse(commandResponse), onlyAdvanceFlowStateWhenCompensationSucceeded(commandResponse));
        }

        private Consumer<BiConsumer<Message, S>> handleResponse(Response commandResponse) {
            return responseConsumer -> {

                if(BusinessFlowDefinition.isNotMarkerConsumer(responseConsumer) ||
                        BusinessFlowDefinition.isCompensateMarkerConsumer(responseConsumer)) {
                    responseConsumer.accept(commandResponse, flowCurrentState.getState());
                }

                if(BusinessFlowDefinition.isCompensateMarkerConsumer(responseConsumer)) {
                    flowCurrentState = flowCurrentState.asCompensation();
                }

                if(!BusinessFlowDefinition.isRetryMarkerConsumer(responseConsumer)) {
                    advanceFlowCurrentState();
                }

            };
        }

        private Runnable onlyAdvanceFlowStateWhenCompensationSucceeded(Response response) {
            return isCompensationSucceededResponseWhenCompensating(response) ?
                    this::advanceFlowCurrentState : Runnables.doNothing();
        }

        private void advanceFlowCurrentState() {
            flowCurrentState = flowCurrentState.advanceState();
        }

        StateEnvelope<S> getFlowCurrentState() {
            return flowCurrentState;
        }

        Optional<Command> moveOnToNextCommand() {
            var possibleNextCommand = getFollowingCommand();

            while (isNoNextCommandDefinedAtCurrentStepWhenCompensating(possibleNextCommand)) {
                advanceFlowCurrentState();
                possibleNextCommand = getFollowingCommand();
            }

            return possibleNextCommand;
        }

        private boolean isNoNextCommandDefinedAtCurrentStepWhenCompensating(Optional<Command> nextCommand) {
            return !isFlowComplete() && flowCurrentState.isCompensation() && nextCommand.isEmpty();
        }

        private Optional<Command> getFollowingCommand() {
            if (isFlowComplete()) {
                return empty();
            }

            var followingCommandProvider =
                    flowCurrentState.isCompensation() ?
                            getNextCompensationCommandProvider() :
                            of(currentStepDefinition().getStepCommandProvider());

            return followingCommandProvider.map(provider -> provider.apply(flowCurrentState.getState()));
        }

        private Optional<Function<S, ? extends Command>> getNextCompensationCommandProvider() {
            return flowCurrentState.getStateIndex() == INIT_EVENT_COMPENSATION_STEP_INDEX ?
                    businessFlowDefinition.getFlowTriggerCompensationCommandProvider() :
                    currentStepDefinition().getCompensatingCommandFnProvider();
        }

        boolean isNotYetInitialized() {
            return flowCurrentState == null;
        }

        boolean isFlowComplete() {
            return
                    flowCurrentState.getStateIndex() == businessFlowDefinition.getBusinessFlowSteps().size() ||
                    flowCurrentState.getStateIndex() == INDEX_OF_FULLY_COMPENSATED_STATE;
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
            requireStateThat(isNotYetInitialized(), "Flow runner got already initialized with current state. The current state cannot be overriden.");
        }

        private void requireFlowNotYetComplete() {
            requireStateThat(!isFlowComplete(), "Flow is already complete and can do nothing more");
        }
    }


    @Getter
    public static class StateEnvelope<S> {
        private final int stateIndex;
        private final boolean compensation;
        private final S state;

        public static <S> StateEnvelope<S> recreateExistingState(int stateIndex, S state, boolean compensation) {
            return new StateEnvelope<>(stateIndex, state, compensation);
        }

        public StateEnvelope(S state) {
            this(0, state, false);
        }

        StateEnvelope(int stateIndex, S state, boolean compensation) {
            this.stateIndex = stateIndex;
            this.state = state;
            this.compensation = compensation;
        }

        public StateEnvelope<S> advanceState() {
            int nextStateIndex = compensation ? stateIndex - 1 : stateIndex + 1;
            return new StateEnvelope<>(nextStateIndex, state, compensation);
        }

        public StateEnvelope<S> asCompensation() {
            return new StateEnvelope<>(stateIndex, state, true);
        }
    }
}
