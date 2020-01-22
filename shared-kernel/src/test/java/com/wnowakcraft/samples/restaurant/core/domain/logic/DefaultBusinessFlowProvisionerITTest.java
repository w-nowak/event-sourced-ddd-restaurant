package com.wnowakcraft.samples.restaurant.core.domain.logic;

import com.wnowakcraft.samples.restaurant.core.domain.logic.DefaultBusinessFlowProvisioner.BusinessFlowProvisionerConfig;
import com.wnowakcraft.samples.restaurant.core.domain.model.Command;
import com.wnowakcraft.samples.restaurant.core.domain.model.CompensationSucceededResponse;
import com.wnowakcraft.samples.restaurant.core.domain.model.Response;
import com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.*;
import com.wnowakcraft.samples.restaurant.core.infrastructure.saga.BusinessFlowRunner.StateEnvelope;
import com.wnowakcraft.samples.restaurant.core.infrastructure.saga.BusinessFlowStateHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;
import java.util.function.Consumer;

import static com.wnowakcraft.samples.restaurant.core.domain.logic.BusinessFlowDefinition.OnResponse.*;
import static com.wnowakcraft.samples.restaurant.core.domain.logic.TestData.*;
import static com.wnowakcraft.samples.restaurant.core.domain.logic.TestData.StateIndexAndCompensation.normalFlowAt;
import static com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.CommandResponseChannelMock.ThenRespondWith.thenRespondWith;
import static com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.CommandResponseChannelMock.allowedFlowFinishedResponses;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
class DefaultBusinessFlowProvisionerITTest {
    private static final int FLOW_FULLY_COMPENSATED_STATE_INDEX =  -2;
    private static final int INIT_EVENT_STATE_INDEX =  -1;
    private static final int INIT_EVENT_HANDLED_STATE_INDEX = handled(INIT_EVENT_STATE_INDEX);
    private static final int FIRST_COMMAND_STATE_INDEX = 0;
    private static final int FIRST_COMMAND_HANDLED_STATE_INDEX = handled(FIRST_COMMAND_STATE_INDEX);
    private static final int QUERY_FOR_DATA_STATE_INDEX = 1;
    private static final int QUERY_FOR_DATA_HANDLED_STATE_INDEX = handled(QUERY_FOR_DATA_STATE_INDEX);
    private static final int SECOND_COMMAND_STATE_INDEX = 2;
    private static final int FINISHING_COMMAND_STATE_INDEX = 3;
    private static final int FINISHING_COMMAND_HANDLED_STATE_INDEX = handled(FINISHING_COMMAND_STATE_INDEX);

    private Fixture fixture;

    private static int handled(int stateIndex) {
        return stateIndex + 1;
    }

    @BeforeEach
    void setUp() {
        var initFlowState = TestState.noCommandHandled();

        var businessFlowDefinition =
                BusinessFlowDefinition
                        .startWith(TestInitEvent.class, e -> initFlowState)
                            .compensateBy(s -> INIT_EVENT.COMPENSATION_COMMAND)
                        .thenSend(s -> FIRST_COMMAND.COMMAND)
                            .on(FirstCommandSuccessfulResponse.class, (resp, state) -> state.firstCommandHandled())
                            .on(FirstCommandErrorResponse.class, failureWithCompensation((resp, state) -> state.compensationInitiatedOnFirstCommand()))
                            .on(CompensationSucceededResponse.class, (resp, state) -> state.firstCommandCompensated())
                            .compensateBy(s -> FIRST_COMMAND.COMPENSATION)
                        .thenSend(s -> QUERY_FOR_DATA.QUERY)
                            .on(QueriedDataReturnedResponse.class, (resp, state) -> state.requestedDataIs(resp.getQueriedData()))
                            .on(QueriedDataErrorResponse.class, failureWithCompensation())
                        .thenSend(s -> SECOND_COMMAND.COMMAND)
                            .on(SecondCommandSuccessfulResponse.class, (resp, state) -> state.secondCommandHandled())
                            .on(SecondCommandErrorResponse.class, failureWithCompensation((resp, state) -> state.compensationInitiatedOnSecondCommand()))
                            .on(CompensationSucceededResponse.class, (resp, state) -> state.secondCommandCompensated())
                            .compensateBy(s -> SECOND_COMMAND.COMPENSATION)
                        .thenSend(s -> FINISHING_COMMAND.COMMAND)
                            .on(FinishingCommandSuccessfulResponse.class, success())
                            .on(FinishingCommandErrorResponse.class, failureWithRetry())
                        .done();

        fixture = new Fixture(businessFlowDefinition, initFlowState);
    }

    @Test
    @Timeout(3)
    void successfulFlowPass_forNewBusinessFlow_whenAllSuccessfulResponsesReturned() {
        fixture.givenFlowIsInitialized();
        fixture.givenFlowIsProvisioned();
        fixture.givenFirstCommandReturnsSuccessfulResponse();
        fixture.givenQueryForDataReturnsResponseWithRequestedData();
        fixture.givenSecondCommandReturnsSuccessfulResponse();
        fixture.givenFinalizingCommandReturnsSuccessfulResponse();
        fixture.whenFlowIsInitiatedByInitEvent();
        fixture.thenWaitUntilFlowIsFinished();
        fixture.thenFinalStateIs(
                TestState.noCommandHandled()
                        .firstCommandHandled()
                        .secondCommandHandled()
                        .requestedDataIs(QUERY_FOR_DATA.REQUESTED_DATA)
        );
        fixture.thenFlowStateHandlerWasCalledAsForSuccessfulFlow();
    }

    @Test
    @Timeout(3)
    void successfulFlowPass_forLoadedInProgressBusinessFlowResumedAfterInitEventHandled_whenAllSuccessfulResponsesReturnedForRemainingSteps() {
        fixture.givenFlowIsInitialized();
        fixture.givenFlowIsProvisioned();
        fixture.givenCurrentFlowStateIsReadFromFlowStateHandler(FIRST_COMMAND.SUCCESSFUL_RESPONSE, normalFlowAt(INIT_EVENT_HANDLED_STATE_INDEX), TestState.noCommandHandled());
        fixture.givenQueryForDataReturnsResponseWithRequestedData();
        fixture.givenSecondCommandReturnsSuccessfulResponse();
        fixture.givenFinalizingCommandReturnsSuccessfulResponse();
        fixture.whenFirstCommandSuccessfulResponseIsReceived();
        fixture.thenWaitUntilFlowIsFinished();
        fixture.thenFinalStateIs(
                TestState.noCommandHandled()
                        .firstCommandHandled()
                        .secondCommandHandled()
                        .requestedDataIs(QUERY_FOR_DATA.REQUESTED_DATA)
        );
        fixture.thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithFistCommandSuccessfulResponse();
    }

    @Test
    @Timeout(3)
    void successfulFlowPass_forLoadedInProgressBusinessFlowAfterFistCommandHandled_whenAllSuccessfulResponsesReturnedForRemainingSteps() {
        fixture.givenFlowIsInitialized();
        fixture.givenFlowIsProvisioned();
        fixture.givenCurrentFlowStateIsReadFromFlowStateHandler(
                QUERY_FOR_DATA.RETURNED_RESPONSE,
                normalFlowAt(FIRST_COMMAND_HANDLED_STATE_INDEX),
                TestState.noCommandHandled().firstCommandHandled()
        );
        fixture.givenSecondCommandReturnsSuccessfulResponse();
        fixture.givenFinalizingCommandReturnsSuccessfulResponse();
        fixture.whenRequestedDataIsReceived();
        fixture.thenWaitUntilFlowIsFinished();
        fixture.thenFinalStateIs(
                TestState.noCommandHandled()
                        .firstCommandHandled()
                        .secondCommandHandled()
                        .requestedDataIs(QUERY_FOR_DATA.REQUESTED_DATA)
        );
        fixture.thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithRequestedDataResponse();
    }

    @Test
    @Timeout(3)
    void successfulFlowPass_forLoadedInProgressBusinessFlowResumedAfterRequestedDataIsHandled_whenAllSuccessfulResponsesReturnedForRemainingSteps() {
        fixture.givenFlowIsInitialized();
        fixture.givenFlowIsProvisioned();
        fixture.givenCurrentFlowStateIsReadFromFlowStateHandler(
                SECOND_COMMAND.SUCCESSFUL_RESPONSE,
                normalFlowAt(QUERY_FOR_DATA_HANDLED_STATE_INDEX),
                TestState.noCommandHandled().firstCommandHandled().requestedDataIs(QUERY_FOR_DATA.REQUESTED_DATA)
        );
        fixture.givenFinalizingCommandReturnsSuccessfulResponse();
        fixture.whenSecondCommandSuccessfulResponseIsReceived();
        fixture.thenWaitUntilFlowIsFinished();
        fixture.thenFinalStateIs(
                TestState.noCommandHandled()
                        .firstCommandHandled()
                        .secondCommandHandled()
                        .requestedDataIs(QUERY_FOR_DATA.REQUESTED_DATA)
        );
        fixture.thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithSecondCommandSuccessfulResponse(); ;
    }

    @Test
    @Timeout(3)
    void compensatedFlow_whenErrorOccursAtSomePointOfBusinessFlow_onStepWithCompensation_allPreviousCompensableStatesAreCompensated() {
        fixture.givenFlowIsInitialized();
        fixture.givenFlowIsProvisioned();
        fixture.givenCurrentFlowStateIsReadFromFlowStateHandler(FIRST_COMMAND.SUCCESSFUL_RESPONSE, normalFlowAt(INIT_EVENT_HANDLED_STATE_INDEX), TestState.noCommandHandled());
        fixture.givenQueryForDataReturnsResponseWithRequestedData();
        fixture.givenSecondCommandReturnsErrorResponse();
        fixture.givenSecondCommandCompensationSucceeds();
        fixture.givenFirstCommandCompensationSucceeds();
        fixture.givenInitEventCompensationSucceeds();
        fixture.whenFirstCommandSuccessfulResponseIsReceived();
        fixture.thenWaitUntilFlowIsFinished();
        fixture.thenFinalStateIs(
                TestState.noCommandHandled()
                        .firstCommandHandled()
                        .requestedDataIs(QUERY_FOR_DATA.REQUESTED_DATA)
                        .compensationInitiatedOnSecondCommand()
                        .firstCommandCompensated()
        );
        fixture.thenFlowStateHandlerWasCalledAsForCompensatedFlow_whereCompensationStartedOnSecondCommand();
    }

    @Test
    @Timeout(3)
    void compensatedFlow_whenErrorOccursAtSomePointOfBusinessFlow_onStepWithNoCompensation_allPreviousCompensableStatesAreCompensated() {
        fixture.givenFlowIsInitialized();
        fixture.givenFlowIsProvisioned();
        fixture.givenCurrentFlowStateIsReadFromFlowStateHandler(FIRST_COMMAND.SUCCESSFUL_RESPONSE, normalFlowAt(INIT_EVENT_HANDLED_STATE_INDEX), TestState.noCommandHandled());
        fixture.givenQueryForDataReturnsErrorResponse();
        fixture.givenFirstCommandCompensationSucceeds();
        fixture.givenInitEventCompensationSucceeds();
        fixture.whenFirstCommandSuccessfulResponseIsReceived();
        fixture.thenWaitUntilFlowIsFinished();
        fixture.thenFinalStateIs(
                TestState.noCommandHandled()
                        .firstCommandHandled()
                        .firstCommandCompensated()
        );
        fixture.thenFlowStateHandlerWasCalledAsForCompensatedFlow_whereCompensationStartedOnQueryForData();
    }

    private static class Fixture {
        private static final String COMMAND_RESPONSE_CHANNEL_NAME = "response_channel_name";

        private final BusinessFlowDefinition<TestInitEvent, TestState> businessFlowDefinition;

        @Mock private BusinessFlowStateHandler<TestState> flowStateHandler;
        @Mock private CommandChannelFactory commandChannelFactory;
        @Mock private EventListenerFactory eventListenerFactory;
        private EventListenerBuilder eventListenerBuilder;
        private BusinessFlowProvisionerConfig<TestInitEvent> flowProvisionerConfig;
        private TestState actualFlowState;
        private CommandResponseChannelMock commandResponseChannelMock;
        private ArgumentCaptor<Consumer<TestInitEvent>> initEventCaptor = ArgumentCaptor.forClass(Consumer.class);
        private BusinessFlowProvisioner<TestInitEvent, TestState> businessFlowProvisioner;

        Fixture(BusinessFlowDefinition<TestInitEvent, TestState> businessFlowDefinition, TestState flowInitialState) {
            this.businessFlowDefinition = businessFlowDefinition;
            this.actualFlowState = flowInitialState;
            MockitoAnnotations.initMocks(this);

            eventListenerBuilder = new EventListenerBuilder(eventListenerFactory);

            flowProvisionerConfig = new BusinessFlowProvisionerConfig<>(
                    COMMAND_RESPONSE_CHANNEL_NAME, EVENT_FAMILY, INIT_EVENT.EVENT.getClass());

            businessFlowProvisioner = new DefaultBusinessFlowProvisioner<>(
                    eventListenerBuilder,
                    commandChannelFactory,
                    flowStateHandler,
                    flowProvisionerConfig
            );
        }

        void givenFlowIsInitialized() {
            setUpCommandResponseChannelMock();
            mockFlowStateHandlerToNotifyAboutPublishedCommands();
            mockEventListenerFactoryToCaptureEventListener();
        }

        private void setUpCommandResponseChannelMock() {
            commandResponseChannelMock =
                    CommandResponseChannelMock.mockCommandResponseChannel(
                            COMMAND_RESPONSE_CHANNEL_NAME, commandChannelFactory,
                            allowedFlowFinishedResponses(FINISHING_COMMAND.SUCCESSFUL_RESPONSE, INIT_EVENT.COMPENSATION_SUCCEEDED_RESPONSE)
                    );
        }

        private void mockFlowStateHandlerToNotifyAboutPublishedCommands() {
            willAnswer(this::notifyCommandSent)
                    .given(flowStateHandler)
                    .createNewState(anyStateEnvelope(), any(Command.class));
            willAnswer(this::notifyCommandSent)
                    .given(flowStateHandler)
                    .updateState(anyStateEnvelope(), any(Command.class));
        }

        private Void notifyCommandSent(InvocationOnMock invocationOnMock) {
            commandResponseChannelMock
                    .getSentCommandNotifier()
                    .notifyCommandSent(invocationOnMock.getArgument(1, Command.class));

            return null;
        }

        private void mockEventListenerFactoryToCaptureEventListener() {
            EventListener<TestInitEvent> eventListener = mock(EventListener.class);
            given(eventListenerFactory.<TestInitEvent>listenToEventsOfKind(EVENT_FAMILY)).willReturn(completedFuture(eventListener));
            willDoNothing().given(eventListener).onEvent(initEventCaptor.capture());
        }


        void givenFlowIsProvisioned() {
            businessFlowProvisioner.provision(businessFlowDefinition);
        }

        void givenFirstCommandReturnsSuccessfulResponse() {
            commandResponseChannelMock.when(FIRST_COMMAND.COMMAND, thenRespondWith(FIRST_COMMAND.SUCCESSFUL_RESPONSE));
        }

        void givenQueryForDataReturnsResponseWithRequestedData() {
            commandResponseChannelMock.when(QUERY_FOR_DATA.QUERY, thenRespondWith(QUERY_FOR_DATA.RETURNED_RESPONSE));
        }

        void givenQueryForDataReturnsErrorResponse() {
            commandResponseChannelMock.when(QUERY_FOR_DATA.QUERY, thenRespondWith(QUERY_FOR_DATA.ERROR_RESPONSE));
        }

        void givenSecondCommandReturnsSuccessfulResponse() {
            commandResponseChannelMock.when(SECOND_COMMAND.COMMAND, thenRespondWith(SECOND_COMMAND.SUCCESSFUL_RESPONSE));
        }

        void givenSecondCommandReturnsErrorResponse() {
            commandResponseChannelMock.when(SECOND_COMMAND.COMMAND, thenRespondWith(SECOND_COMMAND.ERROR_RESPONSE));
        }

        void givenFinalizingCommandReturnsSuccessfulResponse() {
            commandResponseChannelMock.when(FINISHING_COMMAND.COMMAND, thenRespondWith(FINISHING_COMMAND.SUCCESSFUL_RESPONSE));
        }

        void givenSecondCommandCompensationSucceeds() {
            commandResponseChannelMock.when(SECOND_COMMAND.COMPENSATION, thenRespondWith(SECOND_COMMAND.COMPENSATION_SUCCEEDED_RESPONSE));
        }

        void givenFirstCommandCompensationSucceeds() {
            commandResponseChannelMock.when(FIRST_COMMAND.COMPENSATION, thenRespondWith(FIRST_COMMAND.COMPENSATION_SUCCEEDED_RESPONSE));
        }

        void givenInitEventCompensationSucceeds() {
            commandResponseChannelMock.when(INIT_EVENT.COMPENSATION_COMMAND, thenRespondWith(INIT_EVENT.COMPENSATION_SUCCEEDED_RESPONSE));
        }

        void whenFlowIsInitiatedByInitEvent() {
            initEventCaptor.getValue().accept(INIT_EVENT.EVENT);
        }

        void thenWaitUntilFlowIsFinished() {
            commandResponseChannelMock
                    .getAsyncTestWaitSupport()
                    .waitUntilAsyncFlowFinished();
        }

        void thenFinalStateIs(TestState expectedTestState) {
            assertThat(actualFlowState).isEqualTo(expectedTestState);
        }

        void thenFlowStateHandlerWasCalledAsForSuccessfulFlow() {
            then(flowStateHandler).should(never()).readFlowState(any(UUID.class));
            then(flowStateHandler).should()
                    .createNewState(argThat(matchesStateWithIndexOf(FIRST_COMMAND_STATE_INDEX)), eq(FIRST_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(QUERY_FOR_DATA_STATE_INDEX)), eq(QUERY_FOR_DATA.QUERY));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(SECOND_COMMAND_STATE_INDEX)), eq(SECOND_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(FINISHING_COMMAND_STATE_INDEX)), eq(FINISHING_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .finalizeState(argThat(matchesStateWithIndexOf(FINISHING_COMMAND_HANDLED_STATE_INDEX)));
        }

        void thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithFistCommandSuccessfulResponse() {
            then(flowStateHandler).should().readFlowState(FIRST_COMMAND.SUCCESSFUL_RESPONSE.getCorrelationId());
            then(flowStateHandler).should(never())
                    .createNewState(anyStateEnvelope(), any(Command.class));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(FIRST_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(QUERY_FOR_DATA_STATE_INDEX)), eq(QUERY_FOR_DATA.QUERY));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(SECOND_COMMAND_STATE_INDEX)), eq(SECOND_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(FINISHING_COMMAND_STATE_INDEX)), eq(FINISHING_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .finalizeState(argThat(matchesStateWithIndexOf(FINISHING_COMMAND_HANDLED_STATE_INDEX)));
        }

        @SuppressWarnings("unchecked")
        private static StateEnvelope<TestState> anyStateEnvelope() {
            return any(StateEnvelope.class);
        }

        void thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithRequestedDataResponse() {
            then(flowStateHandler).should().readFlowState(QUERY_FOR_DATA.RETURNED_RESPONSE.getCorrelationId());
            then(flowStateHandler).should(never())
                    .createNewState(anyStateEnvelope(), any(Command.class));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(FIRST_COMMAND.COMMAND));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(QUERY_FOR_DATA.QUERY));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(SECOND_COMMAND_STATE_INDEX)), eq(SECOND_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(FINISHING_COMMAND_STATE_INDEX)), eq(FINISHING_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .finalizeState(argThat(matchesStateWithIndexOf(FINISHING_COMMAND_HANDLED_STATE_INDEX)));
        }

        void thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithSecondCommandSuccessfulResponse() {
            then(flowStateHandler).should().readFlowState(SECOND_COMMAND.SUCCESSFUL_RESPONSE.getCorrelationId());
            then(flowStateHandler).should(never())
                    .createNewState(anyStateEnvelope(), any(Command.class));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(FIRST_COMMAND.COMMAND));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(QUERY_FOR_DATA.QUERY));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(SECOND_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(FINISHING_COMMAND_STATE_INDEX)), eq(FINISHING_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .finalizeState(argThat(matchesStateWithIndexOf(FINISHING_COMMAND_HANDLED_STATE_INDEX)));
        }

        void thenFlowStateHandlerWasCalledAsForCompensatedFlow_whereCompensationStartedOnSecondCommand() {
            then(flowStateHandler).should().readFlowState(FIRST_COMMAND.SUCCESSFUL_RESPONSE.getCorrelationId());
            then(flowStateHandler).should(never())
                    .createNewState(anyStateEnvelope(), any(Command.class));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(FIRST_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(QUERY_FOR_DATA_STATE_INDEX)), eq(QUERY_FOR_DATA.QUERY));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(SECOND_COMMAND_STATE_INDEX)), eq(SECOND_COMMAND.COMMAND));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(FINISHING_COMMAND.COMMAND));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(SECOND_COMMAND.COMPENSATION));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(FIRST_COMMAND_STATE_INDEX)), eq(FIRST_COMMAND.COMPENSATION));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(INIT_EVENT_STATE_INDEX)), eq(INIT_EVENT.COMPENSATION_COMMAND));
            then(flowStateHandler).should()
                    .finalizeState(argThat(matchesStateWithIndexOf(FLOW_FULLY_COMPENSATED_STATE_INDEX)));
        }

        void thenFlowStateHandlerWasCalledAsForCompensatedFlow_whereCompensationStartedOnQueryForData() {
            then(flowStateHandler).should().readFlowState(FIRST_COMMAND.SUCCESSFUL_RESPONSE.getCorrelationId());
            then(flowStateHandler).should(never())
                    .createNewState(anyStateEnvelope(), any(Command.class));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(FIRST_COMMAND.COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(QUERY_FOR_DATA_STATE_INDEX)), eq(QUERY_FOR_DATA.QUERY));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(SECOND_COMMAND.COMMAND));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(FINISHING_COMMAND.COMMAND));
            then(flowStateHandler).should(never())
                    .updateState(anyStateEnvelope(), eq(SECOND_COMMAND.COMPENSATION));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(FIRST_COMMAND_STATE_INDEX)), eq(FIRST_COMMAND.COMPENSATION));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(INIT_EVENT_STATE_INDEX)), eq(INIT_EVENT.COMPENSATION_COMMAND));
            then(flowStateHandler).should()
                    .finalizeState(argThat(matchesStateWithIndexOf(FLOW_FULLY_COMPENSATED_STATE_INDEX)));
        }

        private ArgumentMatcher<StateEnvelope<TestState>> matchesStateWithIndexOf(int stateIndex) {
            return actualEnvelope -> actualEnvelope.getStateIndex() == stateIndex &&
                                        actualEnvelope.getState().equals(actualFlowState);
        }

        void givenCurrentFlowStateIsReadFromFlowStateHandler(Response onGivenCommandResponse,
                                                             StateIndexAndCompensation stateIndexAndCompensation,
                                                             TestState testState) {
            actualFlowState = testState;
            given(flowStateHandler.readFlowState(onGivenCommandResponse.getCorrelationId()))
                    .willReturn(StateEnvelope.recreateExistingState(stateIndexAndCompensation.getIndex(), testState, stateIndexAndCompensation.isCompensation()));
        }

        void whenFirstCommandSuccessfulResponseIsReceived() {
            commandResponseChannelMock.acceptNewCommandResponseJustReceived(FIRST_COMMAND.SUCCESSFUL_RESPONSE);
        }

        void whenSecondCommandSuccessfulResponseIsReceived() {
            commandResponseChannelMock.acceptNewCommandResponseJustReceived(SECOND_COMMAND.SUCCESSFUL_RESPONSE);
        }

        void whenRequestedDataIsReceived() {
            commandResponseChannelMock.acceptNewCommandResponseJustReceived(QUERY_FOR_DATA.RETURNED_RESPONSE);
        }
    }
}