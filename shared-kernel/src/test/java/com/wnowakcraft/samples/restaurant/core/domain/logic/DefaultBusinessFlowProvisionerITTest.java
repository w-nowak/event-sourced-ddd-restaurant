package com.wnowakcraft.samples.restaurant.core.domain.logic;

import com.wnowakcraft.samples.restaurant.core.domain.logic.DefaultBusinessFlowProvisioner.BusinessFlowProvisionerConfig;
import com.wnowakcraft.samples.restaurant.core.domain.model.*;
import com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.*;
import com.wnowakcraft.samples.restaurant.core.infrastructure.saga.BusinessFlowRunner.StateEnvelope;
import com.wnowakcraft.samples.restaurant.core.infrastructure.saga.BusinessFlowStateHandler;
import lombok.*;
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
import static com.wnowakcraft.samples.restaurant.core.domain.logic.DefaultBusinessFlowProvisionerITTest.StateIndexAndCompensation.normalFlowAt;
import static com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.CommandResponseChannelMock.ThenRespondWith.thenRespondWith;
import static com.wnowakcraft.samples.restaurant.core.infrastructure.messaging.CommandResponseChannelMock.allowedFlowFinishedResponses;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
class DefaultBusinessFlowProvisionerITTest {
    private static final FirstCommand FIRST_COMMAND = new FirstCommand();
    private static final SecondCommand SECOND_COMMAND = new SecondCommand();
    private static final FinishingCommand FINISHING_COMMAND = new FinishingCommand();
    private static final QueryForData QUERY_FOR_DATA = new QueryForData();
    private static final FirstCommandSuccessfulResponse FIRST_COMMAND_SUCCESSFUL_RESPONSE =
            new FirstCommandSuccessfulResponse(UUID.randomUUID());
    private static final SecondCommandSuccessfulResponse SECOND_COMMAND_SUCCESSFUL_RESPONSE =
            new SecondCommandSuccessfulResponse(UUID.randomUUID());
    private static final FinishingCommandSuccessfulResponse FINISHING_COMMAND_SUCCESSFUL_RESPONSE =
            new FinishingCommandSuccessfulResponse(UUID.randomUUID());
    private static final String REQUESTED_DATA = "requested string data";
    private static final QueriedDataReturnedResponse REQUESTED_DATA_RETURNED_RESPONSE =
            new QueriedDataReturnedResponse(UUID.randomUUID(), REQUESTED_DATA);
    private static final InitEventCompensationCommand INIT_EVENT_COMPENSATION =
            new InitEventCompensationCommand();
    private static final FirstCommandCompensation FIRST_COMMAND_COMPENSATION =
            new FirstCommandCompensation();
    private static final SecondCommandCompensation SECOND_COMMAND_COMPENSATION =
            new SecondCommandCompensation();
    private static final int STATE_INDEX_AFTER_INIT_EVENT_HANDLED = 0;
    private static final int STATE_INDEX_AFTER_FIRST_COMMAND_HANDLED = 1;
    private static final int STATE_INDEX_AFTER_REQUESTED_DATA_HANDLED = 2;

    private Fixture fixture;

    @BeforeEach
    void setUp() {
        var initFlowState = new TestState();

        var businessFlowDefinition =
                BusinessFlowDefinition
                        .startWith(TestInitEvent.class, e -> initFlowState)
                            .compensateBy(s -> INIT_EVENT_COMPENSATION)
                        .thenSend(s -> FIRST_COMMAND)
                            .on(FirstCommandSuccessfulResponse.class, (resp, state) -> state.firstCommandHandled())
                            .on(FirstCommandErrorResponse.class, failureWithCompensation((resp, state) -> state.compensationInitiatedOnFirstCommand()))
                            .on(CompensationSucceededResponse.class, (resp, state) -> state.firstCommandCompensated())
                            .compensateBy(s -> FIRST_COMMAND_COMPENSATION)
                        .thenSend(s -> QUERY_FOR_DATA)
                            .on(QueriedDataReturnedResponse.class, (resp, state) -> state.requestedDataIs(resp.getQueriedData()))
                        .thenSend(s -> SECOND_COMMAND)
                            .on(SecondCommandSuccessfulResponse.class, (resp, state) -> state.secondCommandHandled())
                            .on(SecondCommandErrorResponse.class, failureWithCompensation((resp, state) -> state.compensationInitiatedOnSecondCommand()))
                            .on(CompensationSucceededResponse.class, (resp, state) -> state.secondCommandCompensated())
                            .compensateBy(s -> SECOND_COMMAND_COMPENSATION )
                        .thenSend(s -> FINISHING_COMMAND)
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
        fixture.thenFinalStateIs(TestState.bothCommandsHandledAndRequestedDataSetWith(REQUESTED_DATA));
        fixture.thenFlowStateHandlerWasCalledAsForSuccessfulFlow();
    }

    @Test
    @Timeout(3)
    void successfulFlowPass_forLoadedInProgressBusinessFlowResumedAfterInitEventHandled_whenAllSuccessfulResponsesReturnedForRemainingSteps() {
        fixture.givenFlowIsInitialized();
        fixture.givenFlowIsProvisioned();
        fixture.givenCurrentFlowStateIsReadFromFlowStateHandler(FIRST_COMMAND_SUCCESSFUL_RESPONSE, normalFlowAt(STATE_INDEX_AFTER_INIT_EVENT_HANDLED), TestState.noCommandHandled());
        fixture.givenQueryForDataReturnsResponseWithRequestedData();
        fixture.givenSecondCommandReturnsSuccessfulResponse();
        fixture.givenFinalizingCommandReturnsSuccessfulResponse();
        fixture.whenFirstCommandSuccessfulResponseIsReceived();
        fixture.thenWaitUntilFlowIsFinished();
        fixture.thenFinalStateIs(TestState.bothCommandsHandledAndRequestedDataSetWith(REQUESTED_DATA));
        fixture.thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithFistCommandSuccessfulResponse();
    }

    @Test
    @Timeout(3)
    void successfulFlowPass_forLoadedInProgressBusinessFlowAfterFistCommandHandled_whenAllSuccessfulResponsesReturnedForRemainingSteps() {
        fixture.givenFlowIsInitialized();
        fixture.givenFlowIsProvisioned();
        fixture.givenCurrentFlowStateIsReadFromFlowStateHandler(REQUESTED_DATA_RETURNED_RESPONSE, normalFlowAt(STATE_INDEX_AFTER_FIRST_COMMAND_HANDLED),
                TestState.onlyFistCommandHandled());
        fixture.givenSecondCommandReturnsSuccessfulResponse();
        fixture.givenFinalizingCommandReturnsSuccessfulResponse();
        fixture.whenRequestedDataIsReceived();
        fixture.thenWaitUntilFlowIsFinished();
        fixture.thenFinalStateIs(TestState.bothCommandsHandledAndRequestedDataSetWith(REQUESTED_DATA));
        fixture.thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithRequestedDataResponse();
    }

    @Test
    @Timeout(3)
    void successfulFlowPass_forLoadedInProgressBusinessFlowResumedAfterRequestedDataIsHandled_whenAllSuccessfulResponsesReturnedForRemainingSteps() {
        fixture.givenFlowIsInitialized();
        fixture.givenFlowIsProvisioned();
        fixture.givenCurrentFlowStateIsReadFromFlowStateHandler(SECOND_COMMAND_SUCCESSFUL_RESPONSE, normalFlowAt(STATE_INDEX_AFTER_REQUESTED_DATA_HANDLED),
                TestState.fistCommandHandledAndRequestedDataIs(REQUESTED_DATA));
        fixture.givenFinalizingCommandReturnsSuccessfulResponse();
        fixture.whenSecondCommandSuccessfulResponseIsReceived();
        fixture.thenWaitUntilFlowIsFinished();
        fixture.thenFinalStateIs(TestState.bothCommandsHandledAndRequestedDataSetWith(REQUESTED_DATA));
        fixture.thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithSecondCommandSuccessfulResponse(); ;
    }

    @Test
    @Timeout(3)
    void compensatedFlow_whenErrorOccursAtSomePointOfBuisnessFlow_allPreviousCompensableStatesAreCompensated() {
        fixture.givenFlowIsInitialized();
        fixture.givenFlowIsProvisioned();
        fixture.givenCurrentFlowStateIsReadFromFlowStateHandler(FIRST_COMMAND_SUCCESSFUL_RESPONSE, normalFlowAt(STATE_INDEX_AFTER_INIT_EVENT_HANDLED), TestState.noCommandHandled());
        fixture.givenQueryForDataReturnsResponseWithRequestedData();
        fixture.givenSecondCommandReturnsErrorResponse();
        fixture.givenSecondCommandCompensationSucceeds();
        fixture.givenFirstCommandCompensationSucceeds();
        fixture.givenInitEventCompensationSucceeds();
        fixture.whenFirstCommandSuccessfulResponseIsReceived();
        fixture.thenWaitUntilFlowIsFinished();
        fixture.thenFinalStateIs(TestState.stateStartedOnFirstCommandCompensatedOnSecondWithRequestedDataSetWith(REQUESTED_DATA));
        fixture.thenFlowStateHandlerWasCalledAsForCompensatedFlow_whereCompensationStartedOnSecondCommand();
    }

    private static class Fixture {
        private static final String COMMAND_RESPONSE_CHANNEL_NAME = "response_channel_name";
        private static final Class<BaseTestEvent> EVENT_KIND = BaseTestEvent.class;
        private static final TestInitEvent INIT_EVENT = new TestInitEvent();
        private static final FinishingCommandErrorResponse FINISHING_COMMAND_ERROR_RESPONSE =
                new FinishingCommandErrorResponse(UUID.randomUUID());
        private static final SecondCommandErrorResponse SECOND_COMMAND_ERROR_RESPONSE =
                new SecondCommandErrorResponse(UUID.randomUUID());
        private static final CompensationCommandSucceededResponse SECOND_COMMAND_COMPENSATION_SUCCEEDED_RESPONSE =
                new CompensationCommandSucceededResponse(UUID.randomUUID());
        private static final CompensationCommandSucceededResponse FIRST_COMMAND_COMPENSATION_SUCCEEDED_RESPONSE =
                new CompensationCommandSucceededResponse(UUID.randomUUID());
        private static final CompensationCommandSucceededResponse INIT_EVENT_COMPENSATION_SUCCEEDED_RESPONSE =
                new CompensationCommandSucceededResponse(UUID.randomUUID());

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
                    COMMAND_RESPONSE_CHANNEL_NAME, EVENT_KIND, INIT_EVENT.getClass());

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
                            allowedFlowFinishedResponses(FINISHING_COMMAND_SUCCESSFUL_RESPONSE, INIT_EVENT_COMPENSATION_SUCCEEDED_RESPONSE)
                    );
        }

        private void mockFlowStateHandlerToNotifyAboutPublishedCommands() {
            willAnswer(this::notifyCommandSent)
                    .given(flowStateHandler)
                    .createNewState(any(StateEnvelope.class), any(Command.class));
            willAnswer(this::notifyCommandSent)
                    .given(flowStateHandler)
                    .updateState(any(StateEnvelope.class), any(Command.class));
        }

        private Void notifyCommandSent(InvocationOnMock invocationOnMock) {
            commandResponseChannelMock
                    .getSentCommandNotifier()
                    .notifyCommandSent(invocationOnMock.getArgument(1, Command.class));

            return null;
        }

        private void mockEventListenerFactoryToCaptureEventListener() {
            EventListener<TestInitEvent> eventListener = mock(EventListener.class);
            given(eventListenerFactory.<TestInitEvent>listenToEventsOfKind(EVENT_KIND)).willReturn(completedFuture(eventListener));
            willDoNothing().given(eventListener).onEvent(initEventCaptor.capture());
        }


        void givenFlowIsProvisioned() {
            businessFlowProvisioner.provision(businessFlowDefinition);
        }

        void givenFirstCommandReturnsSuccessfulResponse() {
            commandResponseChannelMock.when(FIRST_COMMAND, thenRespondWith(FIRST_COMMAND_SUCCESSFUL_RESPONSE));
        }

        void givenQueryForDataReturnsResponseWithRequestedData() {
            commandResponseChannelMock.when(QUERY_FOR_DATA, thenRespondWith(REQUESTED_DATA_RETURNED_RESPONSE));
        }

        void givenSecondCommandReturnsSuccessfulResponse() {
            commandResponseChannelMock.when(SECOND_COMMAND, thenRespondWith(SECOND_COMMAND_SUCCESSFUL_RESPONSE));
        }

        void givenSecondCommandReturnsErrorResponse() {
            commandResponseChannelMock.when(SECOND_COMMAND, thenRespondWith(SECOND_COMMAND_ERROR_RESPONSE));
        }

        void givenFinalizingCommandReturnsSuccessfulResponse() {
            commandResponseChannelMock.when(FINISHING_COMMAND, thenRespondWith(FINISHING_COMMAND_SUCCESSFUL_RESPONSE));
        }

        void givenSecondCommandCompensationSucceeds() {
            commandResponseChannelMock.when(SECOND_COMMAND_COMPENSATION, thenRespondWith(SECOND_COMMAND_COMPENSATION_SUCCEEDED_RESPONSE));
        }

        void givenFirstCommandCompensationSucceeds() {
            commandResponseChannelMock.when(FIRST_COMMAND_COMPENSATION, thenRespondWith(FIRST_COMMAND_COMPENSATION_SUCCEEDED_RESPONSE));
        }

        void givenInitEventCompensationSucceeds() {
            commandResponseChannelMock.when(INIT_EVENT_COMPENSATION, thenRespondWith(INIT_EVENT_COMPENSATION_SUCCEEDED_RESPONSE));
        }

        void whenFlowIsInitiatedByInitEvent() {
            initEventCaptor.getValue().accept(INIT_EVENT);
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
                    .createNewState(argThat(matchesStateWithIndexOf(0)), eq(FIRST_COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(1)), eq(QUERY_FOR_DATA));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(2)), eq(SECOND_COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(3)), eq(FINISHING_COMMAND));
            then(flowStateHandler).should()
                    .finalizeState(argThat(matchesStateWithIndexOf(4)));
        }

        void thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithFistCommandSuccessfulResponse() {
            then(flowStateHandler).should().readFlowState(FIRST_COMMAND_SUCCESSFUL_RESPONSE.getCorrelationId());
            then(flowStateHandler).should(never())
                    .createNewState(any(StateEnvelope.class), any(Command.class));
            then(flowStateHandler).should(never())
                    .updateState(argThat(matchesStateWithIndexOf(0)), eq(FIRST_COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(1)), eq(QUERY_FOR_DATA));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(2)), eq(SECOND_COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(3)), eq(FINISHING_COMMAND));
            then(flowStateHandler).should()
                    .finalizeState(argThat(matchesStateWithIndexOf(4)));
        }

        void thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithRequestedDataResponse() {
            then(flowStateHandler).should().readFlowState(REQUESTED_DATA_RETURNED_RESPONSE.getCorrelationId());
            then(flowStateHandler).should(never())
                    .createNewState(any(StateEnvelope.class), any(Command.class));
            then(flowStateHandler).should(never())
                    .updateState(argThat(matchesStateWithIndexOf(0)), eq(FIRST_COMMAND));
            then(flowStateHandler).should(never())
                    .updateState(argThat(matchesStateWithIndexOf(1)), eq(QUERY_FOR_DATA));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(2)), eq(SECOND_COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(3)), eq(FINISHING_COMMAND));
            then(flowStateHandler).should()
                    .finalizeState(argThat(matchesStateWithIndexOf(4)));
        }

        void thenFlowStateHandlerWasCalledAsForSuccessfulFlow_startingWithSecondCommandSuccessfulResponse() {
            then(flowStateHandler).should().readFlowState(SECOND_COMMAND_SUCCESSFUL_RESPONSE.getCorrelationId());
            then(flowStateHandler).should(never())
                    .createNewState(any(StateEnvelope.class), any(Command.class));
            then(flowStateHandler).should(never())
                    .updateState(argThat(matchesStateWithIndexOf(0)), eq(FIRST_COMMAND));
            then(flowStateHandler).should(never())
                    .updateState(argThat(matchesStateWithIndexOf(1)), eq(QUERY_FOR_DATA));
            then(flowStateHandler).should(never())
                    .updateState(argThat(matchesStateWithIndexOf(2)), eq(SECOND_COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(3)), eq(FINISHING_COMMAND));
            then(flowStateHandler).should()
                    .finalizeState(argThat(matchesStateWithIndexOf(4)));
        }

        void thenFlowStateHandlerWasCalledAsForCompensatedFlow_whereCompensationStartedOnSecondCommand() {
            then(flowStateHandler).should().readFlowState(FIRST_COMMAND_SUCCESSFUL_RESPONSE.getCorrelationId());
            then(flowStateHandler).should(never())
                    .createNewState(any(StateEnvelope.class), any(Command.class));
            then(flowStateHandler).should(never())
                    .updateState(any(StateEnvelope.class), eq(FIRST_COMMAND));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(1)), eq(QUERY_FOR_DATA));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(2)), eq(SECOND_COMMAND));
            then(flowStateHandler).should(never())
                    .updateState(any(StateEnvelope.class), eq(FINISHING_COMMAND));
            then(flowStateHandler).should(never())
                    .updateState(any(StateEnvelope.class), eq(SECOND_COMMAND_COMPENSATION));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(0)), eq(FIRST_COMMAND_COMPENSATION));
            then(flowStateHandler).should()
                    .updateState(argThat(matchesStateWithIndexOf(-1)), eq(INIT_EVENT_COMPENSATION));
            then(flowStateHandler).should()
                    .finalizeState(argThat(matchesStateWithIndexOf(-2)));
        }

        private ArgumentMatcher<StateEnvelope<TestState>> matchesStateWithIndexOf(int stateIndex) {
            return actualEnvelope -> actualEnvelope.getStateIndex() == stateIndex &&
                                        actualEnvelope.getState() == actualFlowState;
        }

        void givenCurrentFlowStateIsReadFromFlowStateHandler(Response onGivenCommandResponse,
                                                             StateIndexAndCompensation stateIndexAndCompensation,
                                                             TestState testState) {
            actualFlowState = testState;
            given(flowStateHandler.readFlowState(onGivenCommandResponse.getCorrelationId()))
                    .willReturn(StateEnvelope.recreateExistingState(stateIndexAndCompensation.getIndex(), testState, stateIndexAndCompensation.isCompensation()));
        }

        void whenFirstCommandSuccessfulResponseIsReceived() {
            commandResponseChannelMock.acceptNewCommandResponseJustReceived(FIRST_COMMAND_SUCCESSFUL_RESPONSE);
        }

        void whenSecondCommandSuccessfulResponseIsReceived() {
            commandResponseChannelMock.acceptNewCommandResponseJustReceived(SECOND_COMMAND_SUCCESSFUL_RESPONSE);
        }

        void whenRequestedDataIsReceived() {
            commandResponseChannelMock.acceptNewCommandResponseJustReceived(REQUESTED_DATA_RETURNED_RESPONSE);
        }
    }

    @Value
    static class StateIndexAndCompensation {
        private static final boolean IS_COMPENSATION_FLOW = true;
        private static final boolean IS_NORMAL_FLOW = false;
        private int index;
        private boolean compensation;

        static StateIndexAndCompensation normalFlowAt(int index) {
            return new StateIndexAndCompensation(index, IS_NORMAL_FLOW);
        }

        static StateIndexAndCompensation compensationFlowAt(int index) {
            return new StateIndexAndCompensation(index, IS_COMPENSATION_FLOW);
        }
    }

    @ToString
    @AllArgsConstructor
    @EqualsAndHashCode
    @NoArgsConstructor(access = PRIVATE)
    private static class TestState {
        private final static String NO_QUERIED_DATA = null;

        private boolean firstCommandHandled;
        private boolean secondCommandHandled;
        private boolean firstCommandCompensated;
        private boolean secondCommandCompensated;
        private boolean compensationInitiatedOnFistCommand;
        private boolean compensationInitiatedOnSecondCommand;
        private String requestedData;

        static TestState bothCommandsHandledAndRequestedDataSetWith(String requestedData) {
            return new TestState(true, true, false, false, false, false, requestedData);
        }

        static TestState onlyFistCommandHandled () {
            return new TestState(true, false, false, false, false, false, NO_QUERIED_DATA);
        }

        static TestState fistCommandHandledAndRequestedDataIs(String requestedData) {
            return new TestState(true, false, false, false, false, false, requestedData);
        }

        static TestState noCommandHandled() {
            return new TestState(false, false, false, false, false, false, NO_QUERIED_DATA);
        }

        static TestState stateStartedOnFirstCommandCompensatedOnSecondWithRequestedDataSetWith(String requestedData) {
            return new TestState(true, false, true, false, false, true, requestedData);
        }

        void firstCommandHandled() {
            firstCommandHandled = true;
        }

        void firstCommandCompensated() {
            firstCommandCompensated = true;
        }

        void secondCommandHandled() {
            secondCommandHandled = true;
        }

        void secondCommandCompensated() {
            secondCommandCompensated = true;
        }

        void requestedDataIs(String requestedData) {
            this.requestedData = requestedData;
        }

        void compensationInitiatedOnFirstCommand() {
            this.compensationInitiatedOnFistCommand = true;
        }

        void compensationInitiatedOnSecondCommand() {
            this.compensationInitiatedOnSecondCommand = true;
        }
    }

    private static class TestInitEvent extends BaseTestEvent {
        TestInitEvent() {
            super(BaseTestEvent.AGGREGATE_ID, BaseTestEvent.SEQUENCE_NUMBER, BaseTestEvent.GENERATED_ON);
        }
    }

    private static class InitEventCompensationCommand extends AbstractCommand {}

    @RequiredArgsConstructor(access = PROTECTED)
    private static class TestAbstractCommandResponse implements Response {
        private final UUID responseUuid;

        @Override
        public UUID getCorrelationId() {
            return responseUuid;
        }
    }


    private static class FirstCommand extends AbstractCommand {}
    private static class FirstCommandCompensation extends AbstractCommand {}
    private static class FirstCommandSuccessfulResponse extends TestAbstractCommandResponse {
        FirstCommandSuccessfulResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }
    private static class FirstCommandErrorResponse extends TestAbstractCommandResponse {
        protected FirstCommandErrorResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }

    private static class SecondCommand extends AbstractCommand {}
    private static class SecondCommandCompensation extends AbstractCommand {}
    private static class SecondCommandSuccessfulResponse extends TestAbstractCommandResponse {
        protected SecondCommandSuccessfulResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }
    private static class SecondCommandErrorResponse extends TestAbstractCommandResponse {
        protected SecondCommandErrorResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }

    private static class FinishingCommand extends AbstractCommand {}
    private static class FinishingCommandSuccessfulResponse extends TestAbstractCommandResponse {
        protected FinishingCommandSuccessfulResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }
    private static class FinishingCommandErrorResponse extends TestAbstractCommandResponse {
        protected FinishingCommandErrorResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }

    private static class CompensationCommandSucceededResponse extends TestAbstractCommandResponse implements CompensationSucceededResponse{
        protected CompensationCommandSucceededResponse(UUID responseUuid) {
            super(responseUuid);
        }
    }

    private static class QueryForData extends AbstractQuery {}
    @Getter
    private static class QueriedDataReturnedResponse extends AbstractResponse {
        private final String queriedData;
        protected QueriedDataReturnedResponse(UUID responseUuid, String queriedData) {
            super(responseUuid);
            this.queriedData = queriedData;
        }
    }
}